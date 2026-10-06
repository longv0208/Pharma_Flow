package dao;

import db.DBContext;
import model.InventoryAdjustment;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Access to `inventory_adjustments` plus the adjustment write transaction.
 * Extends DBContext per rule.md §20.
 *
 * Reads use the inherited connection/statement/resultSet + closeResources().
 * createAdjustment opens a dedicated Connection with setAutoCommit(false):
 * SELECT FOR UPDATE -> validate -> insert adjustment -> update batch
 * (on_hand + status) -> insert ADJUSTMENT movement -> commit. Any failure
 * rolls the whole thing back, so the batch, the audit record and the movement
 * can never disagree.
 *
 * quantityBefore/After are NEVER taken from the caller — always computed from
 * the locked batch row inside the transaction.
 */
public class InventoryAdjustmentDAO extends DBContext {

    private static final Logger LOG = Logger.getLogger(InventoryAdjustmentDAO.class.getName());

    /** The only reason values the UI/DB accept — enforced server-side. */
    private static final Set<String> REASONS = new HashSet<>(Arrays.asList(
            "DAMAGED", "LOST", "EXPIRED", "COUNT_CORRECTION", "DATA_CORRECTION", "OTHER"));

    /* ==================== mapping ==================== */
    /**
     * Maps one row to an InventoryAdjustment. Expects all adjustment columns
     * plus joined product_name, sku, batch_number, performed_by_name.
     */
    public InventoryAdjustment getFromResultSet(ResultSet rs) throws SQLException {
        InventoryAdjustment a = new InventoryAdjustment();
        a.setAdjustmentId(rs.getLong("adjustment_id"));
        a.setBatchId(rs.getLong("batch_id"));
        Long performedBy = rs.getLong("performed_by");
        if (rs.wasNull()) {
            performedBy = null;
        }
        a.setPerformedBy(performedBy);
        a.setQuantityChange(rs.getInt("quantity_change"));
        a.setQuantityBefore(rs.getInt("quantity_before"));
        a.setQuantityAfter(rs.getInt("quantity_after"));
        a.setReason(rs.getString("reason"));
        a.setNote(rs.getString("note"));
        a.setCreatedAt(rs.getTimestamp("created_at"));
        a.setProductName(rs.getString("product_name"));
        a.setSku(rs.getString("sku"));
        a.setBatchNumber(rs.getString("batch_number"));
        a.setPerformedByName(rs.getString("performed_by_name"));
        return a;
    }

    /* ==================== list / history ==================== */
    /**
     * Adjustment History — newest first, optional filters. Joins
     * batches + products + users for readable display.
     */
    public List<InventoryAdjustment> findAll(
            String productKeyword, String batchNumber, String reason,
            java.sql.Date fromDate, java.sql.Date toDate,
            int offset, int limit) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT a.*, p.product_name, p.sku, b.batch_number, ");
        sql.append("u.full_name AS performed_by_name ");
        sql.append("FROM inventory_adjustments a ");
        sql.append("JOIN inventory_batches b ON b.batch_id = a.batch_id ");
        sql.append("JOIN products p ON p.product_id = b.product_id ");
        sql.append("LEFT JOIN users u ON u.user_id = a.performed_by ");
        sql.append("WHERE 1=1 ");
        appendFilters(sql, productKeyword, batchNumber, reason, fromDate, toDate);
        sql.append("ORDER BY a.created_at DESC, a.adjustment_id DESC LIMIT ? OFFSET ?");

        List<InventoryAdjustment> out = new ArrayList<>();
        try {
            connection = getConnection();
            if (connection == null) {
                return out;
            }
            statement = connection.prepareStatement(sql.toString());
            int i = bindFilters(statement, productKeyword, batchNumber, reason, fromDate, toDate);
            statement.setInt(i++, limit);
            statement.setInt(i++, offset);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                out.add(getFromResultSet(resultSet));
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findAll failed", ex);
        } finally {
            closeResources();
        }
        return out;
    }

    /** Total matching rows for history pagination. */
    public int countAll(
            String productKeyword, String batchNumber, String reason,
            java.sql.Date fromDate, java.sql.Date toDate) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) ");
        sql.append("FROM inventory_adjustments a ");
        sql.append("JOIN inventory_batches b ON b.batch_id = a.batch_id ");
        sql.append("JOIN products p ON p.product_id = b.product_id ");
        sql.append("WHERE 1=1 ");
        appendFilters(sql, productKeyword, batchNumber, reason, fromDate, toDate);

        try {
            connection = getConnection();
            if (connection == null) {
                return 0;
            }
            statement = connection.prepareStatement(sql.toString());
            bindFilters(statement, productKeyword, batchNumber, reason, fromDate, toDate);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
            return 0;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "countAll failed", ex);
            return 0;
        } finally {
            closeResources();
        }
    }

    /** Shared WHERE clause for findAll/countAll. */
    private void appendFilters(StringBuilder sql,
            String productKeyword, String batchNumber, String reason,
            java.sql.Date fromDate, java.sql.Date toDate) {
        if (productKeyword != null && !productKeyword.isEmpty()) {
            sql.append("AND (p.product_name LIKE ? OR p.sku LIKE ?) ");
        }
        if (batchNumber != null && !batchNumber.isEmpty()) {
            sql.append("AND b.batch_number LIKE ? ");
        }
        if (reason != null && REASONS.contains(reason)) {
            sql.append("AND a.reason = ? ");
        }
        if (fromDate != null) {
            sql.append("AND a.created_at >= ? ");
        }
        if (toDate != null) {
            sql.append("AND a.created_at < DATE_ADD(?, INTERVAL 1 DAY) ");
        }
    }

    /** Bind the params appendFilters added; returns the next index. */
    private int bindFilters(PreparedStatement ps,
            String productKeyword, String batchNumber, String reason,
            java.sql.Date fromDate, java.sql.Date toDate) throws SQLException {
        int i = 1;
        if (productKeyword != null && !productKeyword.isEmpty()) {
            ps.setString(i++, "%" + productKeyword + "%");
            ps.setString(i++, "%" + productKeyword + "%");
        }
        if (batchNumber != null && !batchNumber.isEmpty()) {
            ps.setString(i++, "%" + batchNumber + "%");
        }
        if (reason != null && REASONS.contains(reason)) {
            ps.setString(i++, reason);
        }
        if (fromDate != null) {
            ps.setDate(i++, fromDate);
        }
        if (toDate != null) {
            ps.setDate(i++, toDate);
        }
        return i;
    }

    /* ==================== create ==================== */
    /**
     * Result of an adjustment attempt — the servlet maps the error code to a
     * readable ?err= parameter.
     */
    public static class AdjustmentResult {
        public final boolean ok;
        public final String error;
        public final Long adjustmentId;

        private AdjustmentResult(boolean ok, String error, Long adjustmentId) {
            this.ok = ok;
            this.error = error;
            this.adjustmentId = adjustmentId;
        }

        public static AdjustmentResult success(long id) {
            return new AdjustmentResult(true, null, id);
        }

        public static AdjustmentResult fail(String error) {
            return new AdjustmentResult(false, error, null);
        }
    }

    /**
     * One manual on_hand correction, end to end in ONE transaction:
     * lock batch -> validate -> insert adjustment -> update batch
     * (on_hand + status) -> insert ADJUSTMENT movement -> commit.
     * quantity_change may be positive or negative but never 0; the result can
     * never take on_hand below 0 or below reserved_quantity.
     */
    public AdjustmentResult createAdjustment(
            long batchId, long performedBy, int quantityChange,
            String reason, String note) {
        // Cheap pre-checks before touching the DB.
        if (quantityChange == 0) {
            return AdjustmentResult.fail("zerochange");
        }
        if (reason == null || !REASONS.contains(reason)) {
            return AdjustmentResult.fail("invalidreason");
        }
        String cleanNote = note == null ? null : note.trim();
        if ("OTHER".equals(reason) && (cleanNote == null || cleanNote.isEmpty())) {
            return AdjustmentResult.fail("noterequired");
        }

        Connection conn = null;
        try {
            conn = getConnection();
            if (conn == null) {
                return AdjustmentResult.fail("db");
            }
            conn.setAutoCommit(false);

            // 1) Lock the batch row — quantity math only ever uses this read.
            BatchRow batch = loadBatchForUpdate(conn, batchId);
            if (batch == null) {
                conn.rollback();
                return AdjustmentResult.fail("notfound");
            }

            // 2) Compute + validate the target quantity.
            int quantityBefore = batch.onHandQuantity;
            int quantityAfter = quantityBefore + quantityChange;
            if (quantityAfter < 0) {
                conn.rollback();
                return AdjustmentResult.fail("negativestock");
            }
            if (quantityAfter < batch.reservedQuantity) {
                conn.rollback();
                return AdjustmentResult.fail("belowreserved");
            }

            // 3) Insert the audit record first — its id feeds the movement.
            long adjustmentId = insertAdjustment(conn, batchId, performedBy,
                    quantityChange, quantityBefore, quantityAfter, reason, cleanNote);
            if (adjustmentId <= 0) {
                conn.rollback();
                return AdjustmentResult.fail("db");
            }

            // 4) Update on_hand and refresh the status cache. BLOCKED stays
            //    BLOCKED — an adjustment never unblocks.
            String newStatus;
            if ("BLOCKED".equals(batch.status)) {
                newStatus = "BLOCKED";
            } else {
                java.sql.Date today = new java.sql.Date(System.currentTimeMillis());
                newStatus = InventoryDAO.statusForExpiry(batch.expiryDate, today);
                if (!"EXPIRED".equals(newStatus) && quantityAfter <= 0) {
                    newStatus = "OUT_OF_STOCK";
                }
            }
            updateBatch(conn, batchId, quantityAfter, newStatus);

            // 5) Movement — on_hand only, reserved_change is always 0.
            insertMovement(conn, batchId, performedBy, quantityChange,
                    quantityBefore, quantityAfter,
                    batch.reservedQuantity, batch.reservedQuantity,
                    adjustmentId, reason, cleanNote);

            conn.commit();
            return AdjustmentResult.success(adjustmentId);
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "createAdjustment failed — rolled back", ex);
            rollbackQuietly(conn);
            return AdjustmentResult.fail("db");
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

    /* ==================== transaction internals ==================== */
    /** Small row holder for the batch being locked inside the transaction. */
    private static class BatchRow {
        long batchId;
        String status;
        java.sql.Date expiryDate;
        int onHandQuantity;
        int reservedQuantity;
    }

    /** SELECT ... FOR UPDATE so a concurrent adjustment on the batch waits. */
    private BatchRow loadBatchForUpdate(Connection conn, long batchId)
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
                BatchRow row = new BatchRow();
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

    /** Insert the audit row; returns generated adjustment_id. */
    private long insertAdjustment(Connection conn, long batchId, long performedBy,
            int quantityChange, int quantityBefore, int quantityAfter,
            String reason, String note) throws SQLException {
        String sql = "INSERT INTO inventory_adjustments "
                + "(batch_id, performed_by, quantity_change, quantity_before, "
                + " quantity_after, reason, note) "
                + "VALUES (?,?,?,?,?,?,?)";
        PreparedStatement ps = null;
        ResultSet keys = null;
        try {
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, batchId);
            ps.setLong(2, performedBy);
            ps.setInt(3, quantityChange);
            ps.setInt(4, quantityBefore);
            ps.setInt(5, quantityAfter);
            ps.setString(6, reason);
            ps.setString(7, note);
            ps.executeUpdate();
            keys = ps.getGeneratedKeys();
            if (keys.next()) {
                return keys.getLong(1);
            }
            return -1;
        } finally {
            closeQuietly(keys);
            closeQuietly(ps);
        }
    }

    /** Write the new on_hand and refreshed status onto the batch. */
    private void updateBatch(Connection conn, long batchId,
            int quantityAfter, String newStatus) throws SQLException {
        String sql = "UPDATE inventory_batches SET on_hand_quantity = ?, status = ? "
                + "WHERE batch_id = ?";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setInt(1, quantityAfter);
            ps.setString(2, newStatus);
            ps.setLong(3, batchId);
            ps.executeUpdate();
        } finally {
            closeQuietly(ps);
        }
    }

    /** One ADJUSTMENT movement referencing the adjustment row. */
    private void insertMovement(Connection conn, long batchId, long performedBy,
            int onHandChange, int onHandBefore, int onHandAfter,
            int reservedBefore, int reservedAfter,
            long adjustmentId, String reason, String note) throws SQLException {
        String sql = "INSERT INTO inventory_movements "
                + "(batch_id, performed_by, movement_type, on_hand_change, "
                + " reserved_change, on_hand_before, on_hand_after, "
                + " reserved_before, reserved_after, reference_type, reference_id, reason) "
                + "VALUES (?,?, 'ADJUSTMENT', ?,?,?,?,?,?, 'INVENTORY_ADJUSTMENT', ?, ?)";
        PreparedStatement ps = null;
        try {
            // Readable reason: "Damaged - <note>" or just "Damaged".
            String readable = readableReason(reason);
            if (note != null && !note.isEmpty()) {
                readable = readable + " - " + note;
            }
            ps = conn.prepareStatement(sql);
            ps.setLong(1, batchId);
            ps.setLong(2, performedBy);
            ps.setInt(3, onHandChange);
            ps.setInt(4, 0);
            ps.setInt(5, onHandBefore);
            ps.setInt(6, onHandAfter);
            ps.setInt(7, reservedBefore);
            ps.setInt(8, reservedAfter);
            ps.setLong(9, adjustmentId);
            ps.setString(10, readable);
            ps.executeUpdate();
        } finally {
            closeQuietly(ps);
        }
    }

    /* ==================== small helpers ==================== */
    /** Title-case the reason enum for the movement audit line. */
    private static String readableReason(String reason) {
        if (reason == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        boolean cap = true;
        for (char c : reason.toCharArray()) {
            if (c == '_') {
                sb.append(' ');
                cap = true;
            } else if (cap) {
                sb.append(Character.toUpperCase(c));
                cap = false;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }

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
}
