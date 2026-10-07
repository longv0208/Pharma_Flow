package dao;

import db.DBContext;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Read-only reporting DAO — CHU_QUAN_QUAN_TRI operational reports and the
 * admin dashboard. Every query is a SELECT with aggregate functions; nothing
 * here ever INSERTs, UPDATEs or DELETEs. Report rows are small static
 * projection classes inside this DAO — they are query results, not DB
 * entities, so they deliberately do not live in model/ (rule.md §17).
 *
 * Revenue definitions (spec §7-9):
 *   POS revenue    = SUM(sale_transactions.total_amount) WHERE status='HOAN_TAT',
 *                    completion date = sale_datetime.
 *   Online revenue = SUM(online_orders.total_amount) WHERE order_status='HOAN_TAT',
 *                    completion date = updated_at — the schema has no
 *                    completed_at and HOAN_TAT is terminal, so updated_at is
 *                    the DANG_GIAO→HOAN_TAT write timestamp.
 *   Pending / failed / cancelled / rejected rows are never counted, and
 *   inventory_movements are audit records — never summed as revenue.
 *
 * Date filtering uses half-open intervals (>= from 00:00:00 AND < to+1day)
 * so a sale late on the `to` day is included and indexes stay usable.
 * Saleable stock follows InventoryDAO.ALLOCATABLE exactly — never a blanket
 * SUM(on_hand - reserved) over blocked/expired/empty batches.
 */
public class ReportDAO extends DBContext {

    private static final Logger LOG = Logger.getLogger(ReportDAO.class.getName());

    /* ==================== projections ==================== */
    /** Combined POS + online totals for one period. */
    public static class SalesSummary {
        public BigDecimal posRevenue = BigDecimal.ZERO;
        public BigDecimal onlineRevenue = BigDecimal.ZERO;
        public long posCount;
        public long onlineCount;
        public long posUnits;
        public long onlineUnits;

        public BigDecimal getPosRevenue() {
            return posRevenue;
        }

        public BigDecimal getOnlineRevenue() {
            return onlineRevenue;
        }

        public long getPosCount() {
            return posCount;
        }

        public long getOnlineCount() {
            return onlineCount;
        }

        public long getPosUnits() {
            return posUnits;
        }

        public long getOnlineUnits() {
            return onlineUnits;
        }

        /** Combined revenue — read as `${sales.totalRevenue}` in JSP. */
        public BigDecimal getTotalRevenue() {
            return posRevenue.add(onlineRevenue);
        }

        /** Combined completed transaction count — `${sales.totalCount}`. */
        public long getTotalCount() {
            return posCount + onlineCount;
        }

        /** Combined units sold — `${sales.totalUnits}`. */
        public long getTotalUnits() {
            return posUnits + onlineUnits;
        }

        /** Percentage (0-100) of total revenue from POS — `${sales.posShare}`, 0 when total is 0. */
        public double getPosShare() {
            if (getTotalRevenue().signum() <= 0) {
                return 0;
            }
            return posRevenue.multiply(BigDecimal.valueOf(100))
                    .divide(getTotalRevenue(), 2, java.math.RoundingMode.HALF_UP)
                    .doubleValue();
        }

        /** Percentage (0-100) of total revenue from online — `${sales.onlineShare}`, 0 when total is 0. */
        public double getOnlineShare() {
            if (getTotalRevenue().signum() <= 0) {
                return 0;
            }
            return onlineRevenue.multiply(BigDecimal.valueOf(100))
                    .divide(getTotalRevenue(), 2, java.math.RoundingMode.HALF_UP)
                    .doubleValue();
        }
    }

    /** One calendar day of combined revenue. */
    public static class DailySalesRow {
        public Date day;
        public BigDecimal posRevenue = BigDecimal.ZERO;
        public BigDecimal onlineRevenue = BigDecimal.ZERO;
        public long posCount;
        public long onlineCount;

        public Date getDay() {
            return day;
        }

        public BigDecimal getPosRevenue() {
            return posRevenue;
        }

        public BigDecimal getOnlineRevenue() {
            return onlineRevenue;
        }

        public long getPosCount() {
            return posCount;
        }

        public long getOnlineCount() {
            return onlineCount;
        }

        /** Combined day revenue — `${d.totalRevenue}` in JSP. */
        public BigDecimal getTotalRevenue() {
            return posRevenue.add(onlineRevenue);
        }
    }

    /** One product line of the top-sellers report. */
    public static class TopProductRow {
        public long productId;
        public String productName;
        public String sku;
        public long posQty;
        public long onlineQty;
        public long totalQty;
        public BigDecimal revenue = BigDecimal.ZERO;

        public long getProductId() {
            return productId;
        }

        public String getProductName() {
            return productName;
        }

        public String getSku() {
            return sku;
        }

        public long getPosQty() {
            return posQty;
        }

        public long getOnlineQty() {
            return onlineQty;
        }

        public long getTotalQty() {
            return totalQty;
        }

        public BigDecimal getRevenue() {
            return revenue;
        }
    }

    /** Current online-order workflow counters keyed by status code. */
    public static class OrderStatusSummary {
        public long choXuLy;
        public long daXacNhan;
        public long dangChuanBi;
        public long sanSang;
        public long dangGiao;
        public long hoanTat;
        public long daHuy;
        public long tuChoi;

        public long getChoXuLy() {
            return choXuLy;
        }

        public long getDaXacNhan() {
            return daXacNhan;
        }

        public long getDangChuanBi() {
            return dangChuanBi;
        }

        public long getSanSang() {
            return sanSang;
        }

        public long getDangGiao() {
            return dangGiao;
        }

        public long getHoanTat() {
            return hoanTat;
        }

        public long getDaHuy() {
            return daHuy;
        }

        public long getTuChoi() {
            return tuChoi;
        }

        /** Orders still moving through staff + shipper — `${orderStatus.inProgress}` card. */
        public long getInProgress() {
            return choXuLy + daXacNhan + dangChuanBi + sanSang + dangGiao;
        }
    }

    /** Current physical inventory snapshot — all-time, no date filter. */
    public static class InventorySummary {
        public long physicalOnHand;
        public long reserved;
        public long saleable;
        public long batchCount;
        public long blockedBatchCount;
        public long expiredBatchCount;
        public long nearExpiryBatchCount;

        public long getPhysicalOnHand() {
            return physicalOnHand;
        }

        public long getReserved() {
            return reserved;
        }

        public long getSaleable() {
            return saleable;
        }

        public long getBatchCount() {
            return batchCount;
        }

        public long getBlockedBatchCount() {
            return blockedBatchCount;
        }

        public long getExpiredBatchCount() {
            return expiredBatchCount;
        }

        public long getNearExpiryBatchCount() {
            return nearExpiryBatchCount;
        }
    }

    /* ==================== sales summary ==================== */
    /**
     * POS + online totals for [from, to]. Two aggregate SELECTs (revenue +
     * units each side) — no N+1, no double counting.
     */
    public SalesSummary findSalesSummary(Date from, Date to) {
        SalesSummary s = new SalesSummary();
        s.posRevenue = queryPosRevenue(from, to);
        s.posCount = queryPosCount(from, to);
        s.onlineRevenue = queryOnlineRevenue(from, to);
        s.onlineCount = queryOnlineCount(from, to);
        s.posUnits = queryPosUnits(from, to);
        s.onlineUnits = queryOnlineUnits(from, to);
        return s;
    }

    private BigDecimal queryPosRevenue(Date from, Date to) {
        String sql = "SELECT COALESCE(SUM(total_amount),0) FROM sale_transactions "
                + "WHERE status='HOAN_TAT' "
                + "AND sale_datetime >= ? AND sale_datetime < DATE_ADD(?, INTERVAL 1 DAY)";
        return queryBigDecimal(sql, from, to);
    }

    private long queryPosCount(Date from, Date to) {
        String sql = "SELECT COUNT(*) FROM sale_transactions "
                + "WHERE status='HOAN_TAT' "
                + "AND sale_datetime >= ? AND sale_datetime < DATE_ADD(?, INTERVAL 1 DAY)";
        return queryLong(sql, from, to);
    }

    private BigDecimal queryOnlineRevenue(Date from, Date to) {
        String sql = "SELECT COALESCE(SUM(total_amount),0) FROM online_orders "
                + "WHERE order_status='HOAN_TAT' "
                + "AND updated_at >= ? AND updated_at < DATE_ADD(?, INTERVAL 1 DAY)";
        return queryBigDecimal(sql, from, to);
    }

    private long queryOnlineCount(Date from, Date to) {
        String sql = "SELECT COUNT(*) FROM online_orders "
                + "WHERE order_status='HOAN_TAT' "
                + "AND updated_at >= ? AND updated_at < DATE_ADD(?, INTERVAL 1 DAY)";
        return queryLong(sql, from, to);
    }

    private long queryPosUnits(Date from, Date to) {
        String sql = "SELECT COALESCE(SUM(i.quantity),0) FROM sale_items i "
                + "JOIN sale_transactions t ON t.sale_transaction_id = i.sale_transaction_id "
                + "WHERE t.status='HOAN_TAT' "
                + "AND t.sale_datetime >= ? AND t.sale_datetime < DATE_ADD(?, INTERVAL 1 DAY)";
        return queryLong(sql, from, to);
    }

    private long queryOnlineUnits(Date from, Date to) {
        String sql = "SELECT COALESCE(SUM(i.quantity),0) FROM online_order_items i "
                + "JOIN online_orders o ON o.online_order_id = i.online_order_id "
                + "WHERE o.order_status='HOAN_TAT' "
                + "AND o.updated_at >= ? AND o.updated_at < DATE_ADD(?, INTERVAL 1 DAY)";
        return queryLong(sql, from, to);
    }

    /* ==================== daily revenue ==================== */
    /**
     * One row per calendar day inside [from, to] — POS by
     * DATE(sale_datetime), online by DATE(updated_at), merged client-side by
     * day so each list stays one aggregate query (no N+1 per day). Sorted
     * ascending for the report table.
     */
    public List<DailySalesRow> findDailySales(Date from, Date to) {
        java.util.Map<String, DailySalesRow> byDay = new java.util.TreeMap<>();
        readDailyPos(from, to, byDay);
        readDailyOnline(from, to, byDay);
        return new ArrayList<>(byDay.values());
    }

    private void readDailyPos(Date from, Date to,
            java.util.Map<String, DailySalesRow> byDay) {
        String sql = "SELECT DATE(sale_datetime) AS day, "
                + "COALESCE(SUM(total_amount),0) AS revenue, COUNT(*) AS cnt "
                + "FROM sale_transactions "
                + "WHERE status='HOAN_TAT' "
                + "AND sale_datetime >= ? AND sale_datetime < DATE_ADD(?, INTERVAL 1 DAY) "
                + "GROUP BY DATE(sale_datetime) ORDER BY day ASC";
        try {
            connection = getConnection();
            if (connection == null) {
                return;
            }
            statement = connection.prepareStatement(sql);
            statement.setDate(1, from);
            statement.setDate(2, to);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                Date day = resultSet.getDate("day");
                DailySalesRow row = byDay.get(day.toString());
                if (row == null) {
                    row = new DailySalesRow();
                    row.day = day;
                    byDay.put(day.toString(), row);
                }
                row.posRevenue = resultSet.getBigDecimal("revenue");
                row.posCount = resultSet.getLong("cnt");
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "readDailyPos failed", ex);
        } finally {
            closeResources();
        }
    }

    private void readDailyOnline(Date from, Date to,
            java.util.Map<String, DailySalesRow> byDay) {
        String sql = "SELECT DATE(updated_at) AS day, "
                + "COALESCE(SUM(total_amount),0) AS revenue, COUNT(*) AS cnt "
                + "FROM online_orders "
                + "WHERE order_status='HOAN_TAT' "
                + "AND updated_at >= ? AND updated_at < DATE_ADD(?, INTERVAL 1 DAY) "
                + "GROUP BY DATE(updated_at) ORDER BY day ASC";
        try {
            connection = getConnection();
            if (connection == null) {
                return;
            }
            statement = connection.prepareStatement(sql);
            statement.setDate(1, from);
            statement.setDate(2, to);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                Date day = resultSet.getDate("day");
                DailySalesRow row = byDay.get(day.toString());
                if (row == null) {
                    row = new DailySalesRow();
                    row.day = day;
                    byDay.put(day.toString(), row);
                }
                row.onlineRevenue = resultSet.getBigDecimal("revenue");
                row.onlineCount = resultSet.getLong("cnt");
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "readDailyOnline failed", ex);
        } finally {
            closeResources();
        }
    }

    /* ==================== top products ==================== */
    /**
     * Top-N products by units sold inside [from, to] — one UNION ALL query
     * combining completed POS and online item subtotals. Historical prices
     * come from the stored item subtotal, never products.selling_price.
     */
    public List<TopProductRow> findTopProducts(Date from, Date to, int limit) {
        String sql = "SELECT x.product_id, x.product_name, x.sku, "
                + "SUM(x.pos_qty) AS pos_qty, SUM(x.online_qty) AS online_qty, "
                + "SUM(x.pos_qty + x.online_qty) AS total_qty, "
                + "SUM(x.revenue) AS revenue "
                + "FROM ("
                + "  SELECT i.product_id, p.product_name, p.sku, "
                + "         i.quantity AS pos_qty, 0 AS online_qty, i.subtotal AS revenue "
                + "  FROM sale_items i "
                + "  JOIN sale_transactions t ON t.sale_transaction_id = i.sale_transaction_id "
                + "  JOIN products p ON p.product_id = i.product_id "
                + "  WHERE t.status='HOAN_TAT' "
                + "  AND t.sale_datetime >= ? AND t.sale_datetime < DATE_ADD(?, INTERVAL 1 DAY) "
                + "  UNION ALL "
                + "  SELECT i.product_id, p.product_name, p.sku, "
                + "         0 AS pos_qty, i.quantity AS online_qty, i.subtotal AS revenue "
                + "  FROM online_order_items i "
                + "  JOIN online_orders o ON o.online_order_id = i.online_order_id "
                + "  JOIN products p ON p.product_id = i.product_id "
                + "  WHERE o.order_status='HOAN_TAT' "
                + "  AND o.updated_at >= ? AND o.updated_at < DATE_ADD(?, INTERVAL 1 DAY) "
                + ") x "
                + "GROUP BY x.product_id, x.product_name, x.sku "
                + "ORDER BY total_qty DESC, revenue DESC LIMIT ?";
        List<TopProductRow> out = new ArrayList<>();
        try {
            connection = getConnection();
            if (connection == null) {
                return out;
            }
            statement = connection.prepareStatement(sql);
            statement.setDate(1, from);
            statement.setDate(2, to);
            statement.setDate(3, from);
            statement.setDate(4, to);
            statement.setInt(5, limit);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                TopProductRow r = new TopProductRow();
                r.productId = resultSet.getLong("product_id");
                r.productName = resultSet.getString("product_name");
                r.sku = resultSet.getString("sku");
                r.posQty = resultSet.getLong("pos_qty");
                r.onlineQty = resultSet.getLong("online_qty");
                r.totalQty = resultSet.getLong("total_qty");
                r.revenue = resultSet.getBigDecimal("revenue");
                out.add(r);
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findTopProducts failed", ex);
        } finally {
            closeResources();
        }
        return out;
    }

    /* ==================== order status summary ==================== */
    /**
     * Current all-time online order counts by workflow status — one GROUP BY
     * query, missing statuses stay 0 in Java.
     */
    public OrderStatusSummary findOrderStatusSummary() {
        OrderStatusSummary s = new OrderStatusSummary();
        String sql = "SELECT order_status, COUNT(*) AS cnt "
                + "FROM online_orders GROUP BY order_status";
        try {
            connection = getConnection();
            if (connection == null) {
                return s;
            }
            statement = connection.prepareStatement(sql);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String status = resultSet.getString("order_status");
                long cnt = resultSet.getLong("cnt");
                if ("CHO_XU_LY".equals(status)) {
                    s.choXuLy = cnt;
                } else if ("DA_XAC_NHAN".equals(status)) {
                    s.daXacNhan = cnt;
                } else if ("DANG_CHUAN_BI".equals(status)) {
                    s.dangChuanBi = cnt;
                } else if ("SAN_SANG".equals(status)) {
                    s.sanSang = cnt;
                } else if ("DANG_GIAO".equals(status)) {
                    s.dangGiao = cnt;
                } else if ("HOAN_TAT".equals(status)) {
                    s.hoanTat = cnt;
                } else if ("DA_HUY".equals(status)) {
                    s.daHuy = cnt;
                } else if ("TU_CHOI".equals(status)) {
                    s.tuChoi = cnt;
                }
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findOrderStatusSummary failed", ex);
        } finally {
            closeResources();
        }
        return s;
    }

    /* ==================== inventory summary ==================== */
    /**
     * Current physical inventory snapshot — aggregate SQL, never a
     * batch-by-batch Java loop.
     *
     *   physicalOnHand      = SUM(on_hand_quantity) — every batch row.
     *   reserved            = SUM(reserved_quantity).
     *   saleable            = SUM(on_hand - reserved) restricted to
     *                         InventoryDAO.ALLOCATABLE (CO_SAN/SAP_HET_HAN and
     *                         expiry_date > CURDATE()) — blocked, expired and
     *                         empty batches contribute 0.
     *   batchCount          = COUNT(*) rows in inventory_batches.
     *   blockedBatchCount   = COUNT(status='BI_KHOA').
     *   expiredBatchCount   = COUNT(expiry_date <= CURDATE() AND on_hand > 0)
     *                         — expiry date is authoritative, not status.
     *   nearExpiryBatchCount = COUNT(CURDATE() < expiry <= CURDATE()+days
     *                         AND on_hand > 0) — same rule InventoryAlertDAO
     *                         uses for NEAR_EXPIRY.
     */
    public InventorySummary findInventorySummary(int nearExpiryWarningDays) {
        InventorySummary s = new InventorySummary();
        String sql = "SELECT "
                + "COALESCE(SUM(on_hand_quantity),0) AS physical_on_hand, "
                + "COALESCE(SUM(reserved_quantity),0) AS reserved, "
                + "COALESCE(SUM(CASE WHEN " + InventoryDAO.ALLOCATABLE
                + "            THEN on_hand_quantity - reserved_quantity ELSE 0 END),0) AS saleable, "
                + "COUNT(*) AS batch_count, "
                + "COALESCE(SUM(CASE WHEN status='BI_KHOA' THEN 1 ELSE 0 END),0) AS blocked_count, "
                + "COALESCE(SUM(CASE WHEN on_hand_quantity > 0 AND expiry_date <= CURDATE() "
                + "            THEN 1 ELSE 0 END),0) AS expired_count, "
                + "COALESCE(SUM(CASE WHEN on_hand_quantity > 0 AND expiry_date > CURDATE() "
                + "            AND expiry_date <= DATE_ADD(CURDATE(), INTERVAL ? DAY) "
                + "            THEN 1 ELSE 0 END),0) AS near_expiry_count "
                + "FROM inventory_batches b";
        try {
            connection = getConnection();
            if (connection == null) {
                return s;
            }
            statement = connection.prepareStatement(sql);
            statement.setInt(1, nearExpiryWarningDays);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                s.physicalOnHand = resultSet.getLong("physical_on_hand");
                s.reserved = resultSet.getLong("reserved");
                s.saleable = resultSet.getLong("saleable");
                s.batchCount = resultSet.getLong("batch_count");
                s.blockedBatchCount = resultSet.getLong("blocked_count");
                s.expiredBatchCount = resultSet.getLong("expired_count");
                s.nearExpiryBatchCount = resultSet.getLong("near_expiry_count");
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findInventorySummary failed", ex);
        } finally {
            closeResources();
        }
        return s;
    }

    /* ==================== small aggregate helpers ==================== */
    private BigDecimal queryBigDecimal(String sql, Date from, Date to) {
        try {
            connection = getConnection();
            if (connection == null) {
                return BigDecimal.ZERO;
            }
            statement = connection.prepareStatement(sql);
            statement.setDate(1, from);
            statement.setDate(2, to);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                BigDecimal v = resultSet.getBigDecimal(1);
                return v == null ? BigDecimal.ZERO : v;
            }
            return BigDecimal.ZERO;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "queryBigDecimal failed", ex);
            return BigDecimal.ZERO;
        } finally {
            closeResources();
        }
    }

    private long queryLong(String sql, Date from, Date to) {
        try {
            connection = getConnection();
            if (connection == null) {
                return 0;
            }
            statement = connection.prepareStatement(sql);
            statement.setDate(1, from);
            statement.setDate(2, to);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getLong(1);
            }
            return 0;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "queryLong failed", ex);
            return 0;
        } finally {
            closeResources();
        }
    }
}
