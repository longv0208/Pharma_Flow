package dao;

import db.DBContext;
import model.InventoryBatch;
import model.InventoryMovement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Access to `inventory_batches`, `inventory_movements`, and product-level
 * inventory aggregation. Extends DBContext per rule.md §20.
 *
 * Read methods use the inherited connection/statement/resultSet fields and
 * closeResources() in finally. Write methods (block/unblock) open a dedicated
 * local Connection with setAutoCommit(false) — one transaction, commit or
 * rollback, closeQuietly/rollbackQuietly helpers.
 */
public class InventoryDAO extends DBContext {

    private static final Logger LOG = Logger.getLogger(InventoryDAO.class.getName());

    /** Fixed low-stock threshold — consistent with the project, not a settings table. */
    public static final int LOW_STOCK_THRESHOLD = 10;

    /**
     * Allocatable predicate — docs/inventory-availability-rules.md §2.
     * A batch may be picked/sold only if it satisfies this fragment. Alias `b`.
     */
    public static final String ALLOCATABLE =
            "b.status IN ('CO_SAN','SAP_HET_HAN') AND b.expiry_date > CURDATE()";

    /* ==================== mapping ==================== */
    /**
     * Maps one ResultSet row to an InventoryBatch. Expects all batch columns
     * plus joined columns product_name, sku, supplier_name, goods_receipt_id.
     */
    public InventoryBatch getBatchFromResultSet(ResultSet rs) throws SQLException {
        InventoryBatch b = new InventoryBatch();
        b.setBatchId(rs.getLong("batch_id"));
        b.setProductId(rs.getLong("product_id"));
        Long supplierId = rs.getLong("supplier_id");
        if (rs.wasNull()) {
            supplierId = null;
        }
        b.setSupplierId(supplierId);
        Long receiptId = rs.getLong("source_goods_receipt_id");
        if (rs.wasNull()) {
            receiptId = null;
        }
        b.setSourceGoodsReceiptId(receiptId);
        b.setBatchNumber(rs.getString("batch_number"));
        b.setExpiryDate(rs.getDate("expiry_date"));
        b.setOnHandQuantity(rs.getInt("on_hand_quantity"));
        b.setReservedQuantity(rs.getInt("reserved_quantity"));
        b.setCostPrice(rs.getBigDecimal("cost_price"));
        b.setStorageLocation(rs.getString("storage_location"));
        b.setStatus(rs.getString("status"));
        b.setCreatedAt(rs.getTimestamp("created_at"));
        b.setUpdatedAt(rs.getTimestamp("updated_at"));
        b.setProductName(rs.getString("product_name"));
        b.setSku(rs.getString("sku"));
        b.setSupplierName(rs.getString("supplier_name"));
        return b;
    }

    /**
     * Maps one ResultSet row to an InventoryMovement. Expects joined columns
     * product_name, sku, batch_number, performed_by_name.
     */
    public InventoryMovement getMovementFromResultSet(ResultSet rs) throws SQLException {
        InventoryMovement m = new InventoryMovement();
        m.setMovementId(rs.getLong("movement_id"));
        m.setBatchId(rs.getLong("batch_id"));
        Long performedBy = rs.getLong("performed_by");
        if (rs.wasNull()) {
            performedBy = null;
        }
        m.setPerformedBy(performedBy);
        m.setMovementType(rs.getString("movement_type"));
        m.setOnHandChange(rs.getInt("on_hand_change"));
        m.setReservedChange(rs.getInt("reserved_change"));
        m.setOnHandBefore(rs.getInt("on_hand_before"));
        m.setOnHandAfter(rs.getInt("on_hand_after"));
        m.setReservedBefore(rs.getInt("reserved_before"));
        m.setReservedAfter(rs.getInt("reserved_after"));
        m.setReferenceType(rs.getString("reference_type"));
        Long refId = rs.getLong("reference_id");
        if (rs.wasNull()) {
            refId = null;
        }
        m.setReferenceId(refId);
        m.setReason(rs.getString("reason"));
        m.setCreatedAt(rs.getTimestamp("created_at"));
        m.setProductName(rs.getString("product_name"));
        m.setSku(rs.getString("sku"));
        m.setBatchNumber(rs.getString("batch_number"));
        m.setPerformedByName(rs.getString("performed_by_name"));
        return m;
    }

    /* ==================== product-level inventory ==================== */
    /**
     * Inventory List — one row per product with aggregated batch totals.
     * LEFT JOIN keeps products with zero batches visible (status OUT_OF_STOCK).
     * Optional filters: keyword (name/sku), categoryId, derived status.
     */
    public List<ProductInventoryRow> findInventoryProducts(
            String keyword, Long categoryId, String inventoryStatus,
            int offset, int limit) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT p.product_id, p.product_name, p.sku, c.category_name, ");
        sql.append("COUNT(DISTINCT b.batch_id) AS batch_count, ");
        sql.append("COALESCE(SUM(b.on_hand_quantity),0) AS on_hand, ");
        sql.append("COALESCE(SUM(b.reserved_quantity),0) AS reserved ");
        sql.append("FROM products p ");
        sql.append("LEFT JOIN categories c ON c.category_id = p.category_id ");
        sql.append("LEFT JOIN inventory_batches b ON b.product_id = p.product_id ");
        sql.append("WHERE p.status = 'HOAT_DONG' ");
        if (keyword != null && !keyword.isEmpty()) {
            sql.append("AND (p.product_name LIKE ? OR p.sku LIKE ?) ");
        }
        if (categoryId != null) {
            sql.append("AND p.category_id = ? ");
        }
        sql.append("GROUP BY p.product_id, p.product_name, p.sku, c.category_name ");

        // Derived status filter applied after aggregation — HAVING not WHERE
        if (inventoryStatus != null && !inventoryStatus.isEmpty()) {
            if ("OUT_OF_STOCK".equals(inventoryStatus)) {
                sql.append("HAVING on_hand - reserved <= 0 ");
            } else if ("LOW_STOCK".equals(inventoryStatus)) {
                sql.append("HAVING on_hand - reserved > 0 AND on_hand - reserved <= ").append(LOW_STOCK_THRESHOLD).append(" ");
            } else if ("NORMAL".equals(inventoryStatus)) {
                sql.append("HAVING on_hand - reserved > ").append(LOW_STOCK_THRESHOLD).append(" ");
            }
        }
        sql.append("ORDER BY p.product_name ASC LIMIT ? OFFSET ?");

        List<ProductInventoryRow> out = new ArrayList<>();
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
            if (categoryId != null) {
                statement.setLong(i++, categoryId);
            }
            statement.setInt(i++, limit);
            statement.setInt(i++, offset);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                out.add(new ProductInventoryRow(
                        resultSet.getLong("product_id"),
                        resultSet.getString("product_name"),
                        resultSet.getString("sku"),
                        resultSet.getString("category_name"),
                        resultSet.getInt("batch_count"),
                        resultSet.getInt("on_hand"),
                        resultSet.getInt("reserved")
                ));
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findInventoryProducts failed", ex);
        } finally {
            closeResources();
        }
        return out;
    }

    /** Total matching rows for pagination. */
    public int countInventoryProducts(String keyword, Long categoryId, String inventoryStatus) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM (");
        sql.append("SELECT p.product_id, ");
        sql.append("COALESCE(SUM(b.on_hand_quantity),0) AS on_hand, ");
        sql.append("COALESCE(SUM(b.reserved_quantity),0) AS reserved ");
        sql.append("FROM products p ");
        sql.append("LEFT JOIN inventory_batches b ON b.product_id = p.product_id ");
        sql.append("WHERE p.status = 'HOAT_DONG' ");
        if (keyword != null && !keyword.isEmpty()) {
            sql.append("AND (p.product_name LIKE ? OR p.sku LIKE ?) ");
        }
        if (categoryId != null) {
            sql.append("AND p.category_id = ? ");
        }
        sql.append("GROUP BY p.product_id ");
        if (inventoryStatus != null && !inventoryStatus.isEmpty()) {
            if ("OUT_OF_STOCK".equals(inventoryStatus)) {
                sql.append("HAVING on_hand - reserved <= 0 ");
            } else if ("LOW_STOCK".equals(inventoryStatus)) {
                sql.append("HAVING on_hand - reserved > 0 AND on_hand - reserved <= ").append(LOW_STOCK_THRESHOLD).append(" ");
            } else if ("NORMAL".equals(inventoryStatus)) {
                sql.append("HAVING on_hand - reserved > ").append(LOW_STOCK_THRESHOLD).append(" ");
            }
        }
        sql.append(") t");

        try {
            connection = getConnection();
            if (connection == null) {
                return 0;
            }
            statement = connection.prepareStatement(sql.toString());
            int i = 1;
            if (keyword != null && !keyword.isEmpty()) {
                statement.setString(i++, "%" + keyword + "%");
                statement.setString(i++, "%" + keyword + "%");
            }
            if (categoryId != null) {
                statement.setLong(i++, categoryId);
            }
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
            return 0;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "countInventoryProducts failed", ex);
            return 0;
        } finally {
            closeResources();
        }
    }

    /**
     * Product summary row for Product Inventory Detail — totals + batch count.
     */
    public ProductInventoryRow findProductInventorySummary(long productId) {
        String sql = "SELECT p.product_id, p.product_name, p.sku, c.category_name, "
                + "p.product_type, p.selling_unit, "
                + "COUNT(DISTINCT b.batch_id) AS batch_count, "
                + "COALESCE(SUM(b.on_hand_quantity),0) AS on_hand, "
                + "COALESCE(SUM(b.reserved_quantity),0) AS reserved "
                + "FROM products p "
                + "LEFT JOIN categories c ON c.category_id = p.category_id "
                + "LEFT JOIN inventory_batches b ON b.product_id = p.product_id "
                + "WHERE p.product_id = ? AND p.status = 'HOAT_DONG' "
                + "GROUP BY p.product_id, p.product_name, p.sku, c.category_name, "
                + "p.product_type, p.selling_unit LIMIT 1";
        try {
            connection = getConnection();
            if (connection == null) {
                return null;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, productId);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                ProductInventoryRow row = new ProductInventoryRow(
                        resultSet.getLong("product_id"),
                        resultSet.getString("product_name"),
                        resultSet.getString("sku"),
                        resultSet.getString("category_name"),
                        resultSet.getInt("batch_count"),
                        resultSet.getInt("on_hand"),
                        resultSet.getInt("reserved")
                );
                row.setProductType(resultSet.getString("product_type"));
                row.setSellingUnit(resultSet.getString("selling_unit"));
                return row;
            }
            return null;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findProductInventorySummary failed", ex);
            return null;
        } finally {
            closeResources();
        }
    }

    /* ==================== batch-level ==================== */
    /**
     * All batches of one product, FEFO order (expiry ASC, batch_number ASC).
     */
    public List<InventoryBatch> findBatchesByProduct(long productId) {
        String sql = "SELECT b.*, p.product_name, p.sku, s.supplier_name, "
                + "b.source_goods_receipt_id "
                + "FROM inventory_batches b "
                + "JOIN products p ON p.product_id = b.product_id "
                + "LEFT JOIN suppliers s ON s.supplier_id = b.supplier_id "
                + "WHERE b.product_id = ? "
                + "ORDER BY b.expiry_date ASC, b.batch_number ASC";
        List<InventoryBatch> out = new ArrayList<>();
        try {
            connection = getConnection();
            if (connection == null) {
                return out;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, productId);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                out.add(getBatchFromResultSet(resultSet));
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findBatchesByProduct failed", ex);
        } finally {
            closeResources();
        }
        return out;
    }

    /**
     * Single batch by PK with product + supplier names joined.
     */
    public InventoryBatch findBatchById(long batchId) {
        String sql = "SELECT b.*, p.product_name, p.sku, s.supplier_name "
                + "FROM inventory_batches b "
                + "JOIN products p ON p.product_id = b.product_id "
                + "LEFT JOIN suppliers s ON s.supplier_id = b.supplier_id "
                + "WHERE b.batch_id = ? LIMIT 1";
        try {
            connection = getConnection();
            if (connection == null) {
                return null;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, batchId);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return getBatchFromResultSet(resultSet);
            }
            return null;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findBatchById failed", ex);
            return null;
        } finally {
            closeResources();
        }
    }

    /* ==================== movements ==================== */
    /**
     * Inventory History — newest first, optional filters.
     * Joins products + batches + users for readable display.
     */
    public List<InventoryMovement> findMovements(
            String productKeyword, String batchNumber, String movementType,
            Long productId, Long batchId,
            java.sql.Date fromDate, java.sql.Date toDate,
            int offset, int limit) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT m.*, p.product_name, p.sku, b.batch_number, ");
        sql.append("u.full_name AS performed_by_name ");
        sql.append("FROM inventory_movements m ");
        sql.append("JOIN inventory_batches b ON b.batch_id = m.batch_id ");
        sql.append("JOIN products p ON p.product_id = b.product_id ");
        sql.append("LEFT JOIN users u ON u.user_id = m.performed_by ");
        sql.append("WHERE 1=1 ");
        if (productKeyword != null && !productKeyword.isEmpty()) {
            sql.append("AND (p.product_name LIKE ? OR p.sku LIKE ?) ");
        }
        if (batchNumber != null && !batchNumber.isEmpty()) {
            sql.append("AND b.batch_number LIKE ? ");
        }
        if (movementType != null && !movementType.isEmpty()) {
            sql.append("AND m.movement_type = ? ");
        }
        if (productId != null) {
            sql.append("AND b.product_id = ? ");
        }
        if (batchId != null) {
            sql.append("AND m.batch_id = ? ");
        }
        if (fromDate != null) {
            sql.append("AND m.created_at >= ? ");
        }
        if (toDate != null) {
            sql.append("AND m.created_at < DATE_ADD(?, INTERVAL 1 DAY) ");
        }
        sql.append("ORDER BY m.created_at DESC, m.movement_id DESC LIMIT ? OFFSET ?");

        List<InventoryMovement> out = new ArrayList<>();
        try {
            connection = getConnection();
            if (connection == null) {
                return out;
            }
            statement = connection.prepareStatement(sql.toString());
            int i = 1;
            if (productKeyword != null && !productKeyword.isEmpty()) {
                statement.setString(i++, "%" + productKeyword + "%");
                statement.setString(i++, "%" + productKeyword + "%");
            }
            if (batchNumber != null && !batchNumber.isEmpty()) {
                statement.setString(i++, "%" + batchNumber + "%");
            }
            if (movementType != null && !movementType.isEmpty()) {
                statement.setString(i++, movementType);
            }
            if (productId != null) {
                statement.setLong(i++, productId);
            }
            if (batchId != null) {
                statement.setLong(i++, batchId);
            }
            if (fromDate != null) {
                statement.setDate(i++, fromDate);
            }
            if (toDate != null) {
                statement.setDate(i++, toDate);
            }
            statement.setInt(i++, limit);
            statement.setInt(i++, offset);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                out.add(getMovementFromResultSet(resultSet));
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findMovements failed", ex);
        } finally {
            closeResources();
        }
        return out;
    }

    /** Total matching rows for history pagination. */
    public int countMovements(
            String productKeyword, String batchNumber, String movementType,
            Long productId, Long batchId,
            java.sql.Date fromDate, java.sql.Date toDate) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) ");
        sql.append("FROM inventory_movements m ");
        sql.append("JOIN inventory_batches b ON b.batch_id = m.batch_id ");
        sql.append("JOIN products p ON p.product_id = b.product_id ");
        sql.append("WHERE 1=1 ");
        if (productKeyword != null && !productKeyword.isEmpty()) {
            sql.append("AND (p.product_name LIKE ? OR p.sku LIKE ?) ");
        }
        if (batchNumber != null && !batchNumber.isEmpty()) {
            sql.append("AND b.batch_number LIKE ? ");
        }
        if (movementType != null && !movementType.isEmpty()) {
            sql.append("AND m.movement_type = ? ");
        }
        if (productId != null) {
            sql.append("AND b.product_id = ? ");
        }
        if (batchId != null) {
            sql.append("AND m.batch_id = ? ");
        }
        if (fromDate != null) {
            sql.append("AND m.created_at >= ? ");
        }
        if (toDate != null) {
            sql.append("AND m.created_at < DATE_ADD(?, INTERVAL 1 DAY) ");
        }

        try {
            connection = getConnection();
            if (connection == null) {
                return 0;
            }
            statement = connection.prepareStatement(sql.toString());
            int i = 1;
            if (productKeyword != null && !productKeyword.isEmpty()) {
                statement.setString(i++, "%" + productKeyword + "%");
                statement.setString(i++, "%" + productKeyword + "%");
            }
            if (batchNumber != null && !batchNumber.isEmpty()) {
                statement.setString(i++, "%" + batchNumber + "%");
            }
            if (movementType != null && !movementType.isEmpty()) {
                statement.setString(i++, movementType);
            }
            if (productId != null) {
                statement.setLong(i++, productId);
            }
            if (batchId != null) {
                statement.setLong(i++, batchId);
            }
            if (fromDate != null) {
                statement.setDate(i++, fromDate);
            }
            if (toDate != null) {
                statement.setDate(i++, toDate);
            }
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
            return 0;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "countMovements failed", ex);
            return 0;
        } finally {
            closeResources();
        }
    }

    /* ==================== block / unblock ==================== */
    /**
     * Result of a block/unblock attempt — a short code the servlet maps to a
     * readable ?err= parameter.
     */
    public static class StatusResult {
        public final boolean ok;
        public final String error;

        private StatusResult(boolean ok, String error) {
            this.ok = ok;
            this.error = error;
        }

        public static StatusResult success() {
            return new StatusResult(true, null);
        }

        public static StatusResult fail(String error) {
            return new StatusResult(false, error);
        }
    }

    /**
     * Block a usable batch in ONE transaction: re-read → validate → update
     * status → insert KHOA movement → commit. Any failure rolls back.
     */
    public StatusResult blockBatch(long batchId, long performedBy, String reason) {
        return changeBatchStatus(batchId, performedBy, "BI_KHOA", "KHOA", reason);
    }

    /**
     * Unblock a batch in ONE transaction: re-read → validate → pick new status
     * from expiry + on_hand → update → insert MO_KHOA movement → commit.
     */
    public StatusResult unblockBatch(long batchId, long performedBy, String reason) {
        return changeBatchStatus(batchId, performedBy, null, "MO_KHOA", reason);
    }

    /**
     * Shared transaction for KHOA and MO_KHOA. targetStatus=null means
     * "compute from expiry + on_hand" (unblock path).
     */
    private StatusResult changeBatchStatus(long batchId, long performedBy,
            String targetStatus, String movementType, String reason) {
        Connection conn = null;
        try {
            conn = getConnection();
            if (conn == null) {
                return StatusResult.fail("db");
            }
            conn.setAutoCommit(false);

            // 1) Re-read the batch row FOR UPDATE — never trust stale reads.
            InventoryBatchRow batch = loadBatchForUpdate(conn, batchId);
            if (batch == null) {
                conn.rollback();
                return StatusResult.fail("notfound");
            }

            // 2) Validate the requested transition against CURRENT status.
            String newStatus;
            java.sql.Date today = new java.sql.Date(System.currentTimeMillis());
            if ("KHOA".equals(movementType)) {
                if ("BI_KHOA".equals(batch.status)) {
                    conn.rollback();
                    return StatusResult.fail("alreadyblocked");
                }
                // expiry_date is the truth (rule §3): reject if today or past,
                // even when the status cache has not flipped to HET_HAN yet.
                if ("HET_HAN".equals(batch.status)
                        || (batch.expiryDate != null && !batch.expiryDate.after(today))) {
                    conn.rollback();
                    return StatusResult.fail("expired");
                }
                // Reserved stock must be released by the order flow first —
                // never strand reserved units on a blocked batch (docs §5).
                if (batch.reservedQuantity > 0) {
                    conn.rollback();
                    return StatusResult.fail("hasreserved");
                }
                newStatus = "BI_KHOA";
            } else {
                // MO_KHOA — only from BI_KHOA
                if (!"BI_KHOA".equals(batch.status)) {
                    conn.rollback();
                    return StatusResult.fail("notblocked");
                }
                // Backend decides the safe post-unblock status.
                newStatus = statusForExpiry(batch.expiryDate, today);
                if ("HET_HAN".equals(newStatus)) {
                    conn.rollback();
                    return StatusResult.fail("expiredbatch");
                }
                if (batch.onHandQuantity <= 0) {
                    newStatus = "HET_HANG";
                }
            }

            // 3) Update status — quantities never change.
            updateBatchStatus(conn, batchId, newStatus);

            // 4) Insert the audit movement.
            insertMovement(conn, batchId, performedBy, movementType,
                    0, 0, batch.onHandQuantity, batch.onHandQuantity,
                    batch.reservedQuantity, batch.reservedQuantity,
                    "LO_HANG", batchId, reason);

            conn.commit();
            return StatusResult.success();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "changeBatchStatus failed — rolled back", ex);
            rollbackQuietly(conn);
            return StatusResult.fail("db");
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    LOG.log(Level.WARNING, "closing tx connection failed", ex);
                }
            }
        }
    }

    /** Near-expiry window in days — batches inside it are SAP_HET_HAN. */
    public static final int NEAR_EXPIRY_DAYS = 90;

    /**
     * Pick the batch status from its expiry date alone: HET_HAN when the date
     * is today or past, SAP_HET_HAN within {@link #NEAR_EXPIRY_DAYS} of today,
     * otherwise CO_SAN. Self-contained — caller handles zero-stock and
     * BI_KHOA before consulting this (a block is manual, dates can't clear it).
     */
    static String statusForExpiry(java.sql.Date expiryDate, java.sql.Date today) {
        if (expiryDate == null) {
            return "CO_SAN";
        }
        if (!expiryDate.after(today)) {
            return "HET_HAN";
        }
        long diffMs = expiryDate.getTime() - today.getTime();
        long diffDays = diffMs / (1000L * 60 * 60 * 24);
        if (diffDays <= NEAR_EXPIRY_DAYS) {
            return "SAP_HET_HAN";
        }
        return "CO_SAN";
    }

    /* ==================== transaction internals ==================== */
    /** Small row holder for the batch being locked inside the transaction. */
    private static class InventoryBatchRow {
        long batchId;
        String status;
        java.sql.Date expiryDate;
        int onHandQuantity;
        int reservedQuantity;
    }

    /** SELECT ... FOR UPDATE so a second block/unblock on the same batch waits. */
    private InventoryBatchRow loadBatchForUpdate(Connection conn, long batchId)
            throws SQLException {
        String sql = "SELECT batch_id, status, expiry_date, on_hand_quantity, reserved_quantity "
                + "FROM inventory_batches WHERE batch_id = ? LIMIT 1 FOR UPDATE";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(1, batchId);
            rs = ps.executeQuery();
            if (rs.next()) {
                InventoryBatchRow row = new InventoryBatchRow();
                row.batchId = rs.getLong("batch_id");
                row.status = rs.getString("status");
                row.expiryDate = rs.getDate("expiry_date");
                row.onHandQuantity = rs.getInt("on_hand_quantity");
                row.reservedQuantity = rs.getInt("reserved_quantity");
                return row;
            }
            return null;
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    private void updateBatchStatus(Connection conn, long batchId, String status)
            throws SQLException {
        String sql = "UPDATE inventory_batches SET status = ? WHERE batch_id = ?";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setString(1, status);
            ps.setLong(2, batchId);
            ps.executeUpdate();
        } finally {
            closeQuietly(ps);
        }
    }

    /** One movement row per block/unblock — quantity changes are always 0. */
    private void insertMovement(Connection conn, long batchId, long performedBy,
            String movementType, int onHandChange, int reservedChange,
            int onHandBefore, int onHandAfter,
            int reservedBefore, int reservedAfter,
            String referenceType, long referenceId, String reason)
            throws SQLException {
        String sql = "INSERT INTO inventory_movements "
                + "(batch_id, performed_by, movement_type, on_hand_change, "
                + " reserved_change, on_hand_before, on_hand_after, "
                + " reserved_before, reserved_after, reference_type, reference_id, reason) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(1, batchId);
            ps.setLong(2, performedBy);
            ps.setString(3, movementType);
            ps.setInt(4, onHandChange);
            ps.setInt(5, reservedChange);
            ps.setInt(6, onHandBefore);
            ps.setInt(7, onHandAfter);
            ps.setInt(8, reservedBefore);
            ps.setInt(9, reservedAfter);
            ps.setString(10, referenceType);
            ps.setLong(11, referenceId);
            ps.setString(12, reason);
            ps.executeUpdate();
        } finally {
            closeQuietly(ps);
        }
    }

    /* ==================== small helpers ==================== */
    private static void rollbackQuietly(Connection conn) {
        if (conn == null) {
            return;
        }
        try {
            conn.rollback();
        } catch (SQLException ex) {
            LOG.log(Level.WARNING, "rollback failed", ex);
        }
    }

    private static void closeQuietly(java.lang.AutoCloseable r) {
        if (r == null) {
            return;
        }
        try {
            r.close();
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "close failed", ex);
        }
    }

    /* ==================== row holder for product-level inventory ==================== */
    /**
     * One row of the Inventory List / Product Inventory Detail summary.
     * Not an entity — a derived aggregate the JSP renders directly.
     */
    public static class ProductInventoryRow {
        private final long productId;
        private final String productName;
        private final String sku;
        private final String categoryName;
        private final int batchCount;
        private final int onHand;
        private final int reserved;

        private String productType;
        private String sellingUnit;

        public ProductInventoryRow(long productId, String productName, String sku,
                String categoryName, int batchCount, int onHand, int reserved) {
            this.productId = productId;
            this.productName = productName;
            this.sku = sku;
            this.categoryName = categoryName;
            this.batchCount = batchCount;
            this.onHand = onHand;
            this.reserved = reserved;
        }

        public long getProductId() {
            return productId;
        }

        public String getProductName() {
            return productName;
        }

        public String getSku() {
            return sku;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public int getBatchCount() {
            return batchCount;
        }

        public int getOnHand() {
            return onHand;
        }

        public int getReserved() {
            return reserved;
        }

        /** available = on_hand - reserved. Never negative on screen. */
        public int getAvailable() {
            int avail = onHand - reserved;
            if (avail < 0) {
                return 0;
            }
            return avail;
        }

        /**
         * Derived product-level inventory status — NOT the batch status.
         * OUT_OF_STOCK when available <= 0, LOW_STOCK when <= threshold, else NORMAL.
         */
        public String getInventoryStatus() {
            int avail = getAvailable();
            if (avail <= 0) {
                return "OUT_OF_STOCK";
            }
            if (avail <= LOW_STOCK_THRESHOLD) {
                return "LOW_STOCK";
            }
            return "NORMAL";
        }

        public String getInventoryStatusCss() {
            return "inv-" + getInventoryStatus().toLowerCase().replace('_', '-');
        }

        public String getInventoryStatusLabel() {
            String s = getInventoryStatus();
            if ("OUT_OF_STOCK".equals(s)) {
                return "Hết hàng";
            }
            if ("LOW_STOCK".equals(s)) {
                return "Tồn kho thấp";
            }
            return "Bình thường";
        }

        public String getProductType() {
            return productType;
        }

        public void setProductType(String productType) {
            this.productType = productType;
        }

        public String getSellingUnit() {
            return sellingUnit;
        }

        public void setSellingUnit(String sellingUnit) {
            this.sellingUnit = sellingUnit;
        }
    }
}
