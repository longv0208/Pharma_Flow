package dao;

import db.DBContext;
import model.InventoryAlert;
import model.InventoryAlertSetting;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dynamic inventory alerts + the one global `inventory_alert_settings` row.
 * Extends DBContext per rule.md §20.
 *
 * Alerts are NEVER persisted — this DAO only SELECTs batches/products and
 * calculates the alert list on the fly. GET /inventory/alerts is read-only:
 * no batch status updates, no movements, no alert rows.
 *
 * Saleable stock = SUM(on_hand - reserved) over batches matching
 * {@link InventoryDAO#ALLOCATABLE} (CO_SAN/SAP_HET_HAN, not expired by
 * date). BI_KHOA/HET_HAN/HET_HANG batches never count.
 *
 * Expiry alerts read expiry_date directly (authoritative), never the cached
 * batch.status — a batch can expire while still flagged CO_SAN.
 */
public class InventoryAlertDAO extends DBContext {

    private static final Logger LOG = Logger.getLogger(InventoryAlertDAO.class.getName());

    /** Safe fallbacks when the global settings row is absent. */
    public static final int DEFAULT_MINIMUM_STOCK_LEVEL = 10;
    public static final int DEFAULT_NEAR_EXPIRY_WARNING_DAYS = 90;

    /** The only alert types the list filter accepts. */
    private static final Set<String> ALERT_TYPES = new HashSet<>(Arrays.asList(
            "OUT_OF_STOCK", "LOW_STOCK", "NEAR_EXPIRY", "EXPIRED"));

    /* ==================== settings ==================== */
    /**
     * The single global settings row (product_id IS NULL). Returns the row or
     * null when it does not exist yet — callers fall back to defaults.
     */
    public InventoryAlertSetting findGlobalSettings() {
        String sql = "SELECT setting_id, product_id, minimum_stock_level, "
                + "near_expiry_warning_days, updated_by, updated_at "
                + "FROM inventory_alert_settings "
                + "WHERE product_id IS NULL LIMIT 1";
        try {
            connection = getConnection();
            if (connection == null) {
                return null;
            }
            statement = connection.prepareStatement(sql);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                InventoryAlertSetting s = new InventoryAlertSetting();
                s.setSettingId(resultSet.getLong("setting_id"));
                s.setProductId(null);
                s.setMinimumStockLevel(resultSet.getInt("minimum_stock_level"));
                s.setNearExpiryWarningDays(resultSet.getInt("near_expiry_warning_days"));
                Long updatedBy = resultSet.getLong("updated_by");
                if (resultSet.wasNull()) {
                    updatedBy = null;
                }
                s.setUpdatedBy(updatedBy);
                s.setUpdatedAt(resultSet.getTimestamp("updated_at"));
                return s;
            }
            return null;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findGlobalSettings failed", ex);
            return null;
        } finally {
            closeResources();
        }
    }

    /**
     * Effective thresholds — DB row when present, safe defaults otherwise.
     * Never null, so the alerts page works even before anyone saves settings.
     */
    public InventoryAlertSetting getEffectiveSettings() {
        InventoryAlertSetting s = findGlobalSettings();
        if (s == null) {
            s = new InventoryAlertSetting();
            s.setProductId(null);
            s.setMinimumStockLevel(DEFAULT_MINIMUM_STOCK_LEVEL);
            s.setNearExpiryWarningDays(DEFAULT_NEAR_EXPIRY_WARNING_DAYS);
        }
        return s;
    }

    /**
     * Insert-or-update the global row (product_id IS NULL). updated_by comes
     * from the session user — never trusted from the request. Returns the
     * saved row, or null on DB error.
     */
    public InventoryAlertSetting saveGlobalSettings(int minimumStockLevel,
            int nearExpiryWarningDays, long updatedBy) {
        InventoryAlertSetting existing = findGlobalSettings();
        try {
            connection = getConnection();
            if (connection == null) {
                return null;
            }
            if (existing == null) {
                String sql = "INSERT INTO inventory_alert_settings "
                        + "(product_id, minimum_stock_level, near_expiry_warning_days, updated_by) "
                        + "VALUES (NULL, ?, ?, ?)";
                statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                statement.setInt(1, minimumStockLevel);
                statement.setInt(2, nearExpiryWarningDays);
                statement.setLong(3, updatedBy);
                statement.executeUpdate();
                resultSet = statement.getGeneratedKeys();
                if (resultSet.next()) {
                    InventoryAlertSetting s = new InventoryAlertSetting();
                    s.setSettingId(resultSet.getLong(1));
                    s.setProductId(null);
                    s.setMinimumStockLevel(minimumStockLevel);
                    s.setNearExpiryWarningDays(nearExpiryWarningDays);
                    s.setUpdatedBy(updatedBy);
                    return s;
                }
                return null;
            }
            String sql = "UPDATE inventory_alert_settings "
                    + "SET minimum_stock_level = ?, near_expiry_warning_days = ?, "
                    + "updated_by = ? "
                    + "WHERE setting_id = ?";
            statement = connection.prepareStatement(sql);
            statement.setInt(1, minimumStockLevel);
            statement.setInt(2, nearExpiryWarningDays);
            statement.setLong(3, updatedBy);
            statement.setLong(4, existing.getSettingId());
            statement.executeUpdate();
            existing.setMinimumStockLevel(minimumStockLevel);
            existing.setNearExpiryWarningDays(nearExpiryWarningDays);
            existing.setUpdatedBy(updatedBy);
            return existing;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "saveGlobalSettings failed", ex);
            return null;
        } finally {
            closeResources();
        }
    }

    /* ==================== alerts ==================== */
    /**
     * All current alerts — product-level stock alerts first (EXPIRED before
     * OUT_OF_STOCK/LOW_STOCK is handled in SQL ordering below), then expiry
     * alerts per batch. Filters: type allowlist + keyword on product
     * name/sku/batch number. Read-only; no writes of any kind.
     *
     * Severity order: EXPIRED → OUT_OF_STOCK → LOW_STOCK → NEAR_EXPIRY.
     * Inside expiry alerts the earlier expiry comes first; inside stock
     * alerts the lower available quantity comes first.
     */
    public List<InventoryAlert> findAlerts(String alertType, String keyword,
            int minimumStockLevel, int nearExpiryWarningDays) {
        List<InventoryAlert> out = new ArrayList<>();
        out.addAll(findProductStockAlerts(alertType, keyword, minimumStockLevel));
        out.addAll(findExpiryAlerts(alertType, keyword, nearExpiryWarningDays));
        sortAlerts(out);
        return out;
    }

    /**
     * Summary counters for the four alert cards — same business rules as
     * findAlerts, computed in two aggregate queries (no N+1).
     * Index map: [0]=OUT_OF_STOCK, [1]=LOW_STOCK, [2]=NEAR_EXPIRY, [3]=EXPIRED.
     */
    public int[] getAlertSummary(int minimumStockLevel, int nearExpiryWarningDays) {
        int[] counts = new int[4];
        for (InventoryAlert a : findAlerts(null, null,
                minimumStockLevel, nearExpiryWarningDays)) {
            if ("OUT_OF_STOCK".equals(a.getAlertType())) {
                counts[0]++;
            } else if ("LOW_STOCK".equals(a.getAlertType())) {
                counts[1]++;
            } else if ("NEAR_EXPIRY".equals(a.getAlertType())) {
                counts[2]++;
            } else if ("EXPIRED".equals(a.getAlertType())) {
                counts[3]++;
            }
        }
        return counts;
    }

    /**
     * Product-level stock alerts — one row per HOAT_DONG product whose SALEABLE
     * stock is at/below the threshold. Saleable excludes BI_KHOA / HET_HAN /
     * HET_HANG batches and past-expiry dates (InventoryDAO.ALLOCATABLE).
     * saleable <= 0 → OUT_OF_STOCK; 0 < saleable <= minimum → LOW_STOCK.
     * A product never gets both — the HAVING picks exactly one.
     */
    private List<InventoryAlert> findProductStockAlerts(String alertType,
            String keyword, int minimumStockLevel) {
        List<InventoryAlert> out = new ArrayList<>();
        boolean wantOut = alertType == null || "OUT_OF_STOCK".equals(alertType);
        boolean wantLow = alertType == null || "LOW_STOCK".equals(alertType);
        if (!wantOut && !wantLow) {
            return out;
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT p.product_id, p.product_name, p.sku, ");
        sql.append("COALESCE(SUM(b.on_hand_quantity - b.reserved_quantity),0) AS saleable ");
        sql.append("FROM products p ");
        sql.append("LEFT JOIN inventory_batches b ");
        sql.append("  ON b.product_id = p.product_id ");
        sql.append(" AND ").append(InventoryDAO.ALLOCATABLE).append(" ");
        sql.append("WHERE p.status = 'HOAT_DONG' ");
        if (keyword != null && !keyword.isEmpty()) {
            sql.append("AND (p.product_name LIKE ? OR p.sku LIKE ?) ");
        }
        sql.append("GROUP BY p.product_id, p.product_name, p.sku ");
        if (wantOut && wantLow) {
            sql.append("HAVING saleable <= ? ");
        } else if (wantOut) {
            sql.append("HAVING saleable <= 0 ");
        } else {
            sql.append("HAVING saleable > 0 AND saleable <= ? ");
        }
        sql.append("ORDER BY saleable ASC, p.product_name ASC");

        try {
            connection = getConnection();
            if (connection == null) {
                return out;
            }
            statement = connection.prepareStatement(sql.toString());
            int i = 1;
            if (keyword != null && !keyword.isEmpty()) {
                statement.setString(i++, "%" + keyword + "%");
                statement.setString(i++, "%" + keyword + "%");
            }
            if (wantOut && wantLow) {
                statement.setInt(i++, minimumStockLevel);
            } else if (wantLow) {
                statement.setInt(i++, minimumStockLevel);
            }
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                int saleable = resultSet.getInt("saleable");
                InventoryAlert a = new InventoryAlert();
                if (saleable <= 0) {
                    a.setAlertType("OUT_OF_STOCK");
                    a.setSeverity("High");
                    a.setMessage("Không còn tồn kho có thể bán");
                } else {
                    a.setAlertType("LOW_STOCK");
                    a.setSeverity("Warning");
                    a.setMessage("Tồn kho có thể bán thấp hơn mức tối thiểu");
                }
                a.setProductId(resultSet.getLong("product_id"));
                a.setProductName(resultSet.getString("product_name"));
                a.setSku(resultSet.getString("sku"));
                a.setQuantity(saleable);
                a.setThreshold(minimumStockLevel);
                out.add(a);
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findProductStockAlerts failed", ex);
        } finally {
            closeResources();
        }
        return out;
    }

    /**
     * Batch-level expiry alerts — one row per physical batch with stock left.
     * EXPIRED: expiry_date <= today AND on_hand > 0 (blocked batches included —
     * they still physically exist). NEAR_EXPIRY: 0 < days-to-expiry <= warning
     * window AND on_hand > 0. Expiry date is authoritative; batch.status is
     * not consulted and never written.
     */
    private List<InventoryAlert> findExpiryAlerts(String alertType,
            String keyword, int nearExpiryWarningDays) {
        List<InventoryAlert> out = new ArrayList<>();
        boolean wantExpired = alertType == null || "EXPIRED".equals(alertType);
        boolean wantNear = alertType == null || "NEAR_EXPIRY".equals(alertType);
        if (!wantExpired && !wantNear) {
            return out;
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT b.batch_id, b.batch_number, b.expiry_date, ");
        sql.append("b.on_hand_quantity, b.status, ");
        sql.append("DATEDIFF(b.expiry_date, CURDATE()) AS days_to_expiry, ");
        sql.append("p.product_id, p.product_name, p.sku ");
        sql.append("FROM inventory_batches b ");
        sql.append("JOIN products p ON p.product_id = b.product_id ");
        sql.append("WHERE b.on_hand_quantity > 0 ");
        sql.append("AND b.expiry_date IS NOT NULL ");
        if (wantExpired && wantNear) {
            sql.append("AND b.expiry_date <= DATE_ADD(CURDATE(), INTERVAL ? DAY) ");
        } else if (wantExpired) {
            sql.append("AND b.expiry_date <= CURDATE() ");
        } else {
            sql.append("AND b.expiry_date > CURDATE() ");
            sql.append("AND b.expiry_date <= DATE_ADD(CURDATE(), INTERVAL ? DAY) ");
        }
        if (keyword != null && !keyword.isEmpty()) {
            sql.append("AND (p.product_name LIKE ? OR p.sku LIKE ? ");
            sql.append("OR b.batch_number LIKE ?) ");
        }
        sql.append("ORDER BY b.expiry_date ASC, p.product_name ASC");

        try {
            connection = getConnection();
            if (connection == null) {
                return out;
            }
            statement = connection.prepareStatement(sql.toString());
            int i = 1;
            if (wantNear) {
                statement.setInt(i++, nearExpiryWarningDays);
            }
            if (keyword != null && !keyword.isEmpty()) {
                statement.setString(i++, "%" + keyword + "%");
                statement.setString(i++, "%" + keyword + "%");
                statement.setString(i++, "%" + keyword + "%");
            }
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                int daysToExpiry = resultSet.getInt("days_to_expiry");
                InventoryAlert a = new InventoryAlert();
                if (daysToExpiry <= 0) {
                    a.setAlertType("EXPIRED");
                    a.setSeverity("Critical");
                    a.setMessage("Lô đã quá hạn sử dụng — ngừng bán");
                } else {
                    a.setAlertType("NEAR_EXPIRY");
                    a.setSeverity("Warning");
                    a.setMessage("Lô sắp hết hạn trong khoảng cảnh báo");
                }
                a.setProductId(resultSet.getLong("product_id"));
                a.setProductName(resultSet.getString("product_name"));
                a.setSku(resultSet.getString("sku"));
                a.setBatchId(resultSet.getLong("batch_id"));
                a.setBatchNumber(resultSet.getString("batch_number"));
                a.setQuantity(resultSet.getInt("on_hand_quantity"));
                a.setExpiryDate(resultSet.getDate("expiry_date"));
                a.setDaysToExpiry(daysToExpiry);
                out.add(a);
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findExpiryAlerts failed", ex);
        } finally {
            closeResources();
        }
        return out;
    }

    /**
     * Merge order across the two lists: EXPIRED → OUT_OF_STOCK → LOW_STOCK →
     * NEAR_EXPIRY. Each list is already internally ordered; this only sorts
     * by severity bucket (stable sort keeps the inner order).
     */
    private static void sortAlerts(List<InventoryAlert> alerts) {
        alerts.sort(new Comparator<InventoryAlert>() {
            @Override
            public int compare(InventoryAlert a, InventoryAlert b) {
                return severityRank(a.getAlertType()) - severityRank(b.getAlertType());
            }
        });
    }

    private static int severityRank(String alertType) {
        if ("EXPIRED".equals(alertType)) {
            return 0;
        }
        if ("OUT_OF_STOCK".equals(alertType)) {
            return 1;
        }
        if ("LOW_STOCK".equals(alertType)) {
            return 2;
        }
        return 3; // NEAR_EXPIRY
    }

    /** True when the ?type= filter is one of the four known alert types. */
    public static boolean isKnownAlertType(String alertType) {
        return alertType != null && ALERT_TYPES.contains(alertType);
    }
}
