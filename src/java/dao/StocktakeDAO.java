package dao;

import db.DBContext;
import model.Stocktake;
import model.StocktakeItem;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Access to `stocktakes` + `stocktake_items` plus (inside the complete
 * transaction) `inventory_batches` and `inventory_movements`.
 * Extends DBContext per rule.md §20.
 *
 * Status machine: DRAFT -> IN_PROGRESS -> COMPLETED, one direction only.
 * DRAFT holds no items; Start snapshots every inventory batch into
 * stocktake_items (scope = all batches at that moment, blocked/expired
 * included — physical counting ignores saleability). Save only writes
 * actual_quantity on items. Complete re-locks every batch and reconciles
 * against the LIVE on_hand, never the stale snapshot, then writes one
 * STOCKTAKE_ADJUSTMENT movement per non-zero difference.
 *
 * Everything that changes stock happens inside ONE JDBC transaction per
 * action (start / save / complete) — commit or rollback, never partial.
 */
public class StocktakeDAO extends DBContext {

    private static final Logger LOG = Logger.getLogger(StocktakeDAO.class.getName());

    /** The only status values the list filter accepts. */
    private static final Set<String> STATUSES = new HashSet<>(Arrays.asList(
            "DRAFT", "IN_PROGRESS", "COMPLETED"));

    /* ==================== result type ==================== */
    /**
     * Result of a stocktake action — a short code the servlet maps to a
     * readable ?err= parameter, plus an optional ready-made detail message
     * (used for the reserved-stock rejection, which needs batch numbers).
     */
    public static class StocktakeResult {
        public final boolean ok;
        public final String error;
        public final String detail;

        private StocktakeResult(boolean ok, String error, String detail) {
            this.ok = ok;
            this.error = error;
            this.detail = detail;
        }

        public static StocktakeResult success() {
            return new StocktakeResult(true, null, null);
        }

        public static StocktakeResult fail(String error) {
            return new StocktakeResult(false, error, null);
        }

        public static StocktakeResult fail(String error, String detail) {
            return new StocktakeResult(false, error, detail);
        }
    }

    /* ==================== mapping ==================== */
    /**
     * Maps one row to a Stocktake. Expects all stocktakes columns plus
     * created_by_name and the item-count aggregates.
     */
    public Stocktake getFromResultSet(ResultSet rs) throws SQLException {
        Stocktake s = new Stocktake();
        s.setStocktakeId(rs.getLong("stocktake_id"));
        s.setCreatedBy(rs.getLong("created_by"));
        s.setStatus(rs.getString("status"));
        s.setCreatedAt(rs.getTimestamp("created_at"));
        s.setCompletedAt(rs.getTimestamp("completed_at"));
        s.setCreatedByName(rs.getString("created_by_name"));
        s.setItemCount(rs.getInt("item_count"));
        s.setCountedCount(rs.getInt("counted_count"));
        s.setDifferenceCount(rs.getInt("difference_count"));
        return s;
    }

    /**
     * Maps one row to a StocktakeItem. Expects all stocktake_items columns
     * plus joined product_name, sku, batch_number, expiry_date, batch_status,
     * reserved_quantity.
     */
    public StocktakeItem getItemFromResultSet(ResultSet rs) throws SQLException {
        StocktakeItem i = new StocktakeItem();
        i.setStocktakeItemId(rs.getLong("stocktake_item_id"));
        i.setStocktakeId(rs.getLong("stocktake_id"));
        i.setBatchId(rs.getLong("batch_id"));
        i.setSystemQuantity(rs.getInt("system_quantity"));
        int actual = rs.getInt("actual_quantity");
        if (rs.wasNull()) {
            i.setActualQuantity(null);
        } else {
            i.setActualQuantity(actual);
        }
        int diff = rs.getInt("difference_quantity");
        if (rs.wasNull()) {
            i.setDifferenceQuantity(null);
        } else {
            i.setDifferenceQuantity(diff);
        }
        i.setProductName(rs.getString("product_name"));
        i.setSku(rs.getString("sku"));
        i.setBatchNumber(rs.getString("batch_number"));
        i.setExpiryDate(rs.getDate("expiry_date"));
        i.setBatchStatus(rs.getString("batch_status"));
        i.setReservedQuantity(rs.getInt("reserved_quantity"));
        return i;
    }

    /* ==================== list / detail reads ==================== */
    /**
     * Stocktake List — newest first, optional status filter. One LEFT JOIN
     * aggregate supplies item/counted/difference counts (no N+1).
     */
    public List<Stocktake> findAll(String status, int offset, int limit) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT s.*, u.full_name AS created_by_name, ");
        sql.append("COUNT(i.stocktake_item_id) AS item_count, ");
        sql.append("COALESCE(SUM(i.actual_quantity IS NOT NULL),0) AS counted_count, ");
        sql.append("COALESCE(SUM(i.difference_quantity IS NOT NULL AND i.difference_quantity <> 0),0) AS difference_count ");
        sql.append("FROM stocktakes s ");
        sql.append("JOIN users u ON u.user_id = s.created_by ");
        sql.append("LEFT JOIN stocktake_items i ON i.stocktake_id = s.stocktake_id ");
        sql.append("WHERE 1=1 ");
        if (status != null && STATUSES.contains(status)) {
            sql.append("AND s.status = ? ");
        }
        sql.append("GROUP BY s.stocktake_id ");
        sql.append("ORDER BY s.created_at DESC, s.stocktake_id DESC LIMIT ? OFFSET ?");

        List<Stocktake> out = new ArrayList<>();
        try {
            connection = getConnection();
            if (connection == null) {
                return out;
            }
            statement = connection.prepareStatement(sql.toString());
            int i = 1;
            if (status != null && STATUSES.contains(status)) {
                statement.setString(i++, status);
            }
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

    /** Total matching stocktakes for pagination. */
    public int countAll(String status) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM stocktakes s WHERE 1=1 ");
        if (status != null && STATUSES.contains(status)) {
            sql.append("AND s.status = ? ");
        }
        try {
            connection = getConnection();
            if (connection == null) {
                return 0;
            }
            statement = connection.prepareStatement(sql.toString());
            if (status != null && STATUSES.contains(status)) {
                statement.setString(1, status);
            }
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

    /** Single stocktake with creator name + item aggregates, or null. */
    public Stocktake findById(long stocktakeId) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT s.*, u.full_name AS created_by_name, ");
        sql.append("COUNT(i.stocktake_item_id) AS item_count, ");
        sql.append("COALESCE(SUM(i.actual_quantity IS NOT NULL),0) AS counted_count, ");
        sql.append("COALESCE(SUM(i.difference_quantity IS NOT NULL AND i.difference_quantity <> 0),0) AS difference_count ");
        sql.append("FROM stocktakes s ");
        sql.append("JOIN users u ON u.user_id = s.created_by ");
        sql.append("LEFT JOIN stocktake_items i ON i.stocktake_id = s.stocktake_id ");
        sql.append("WHERE s.stocktake_id = ? ");
        sql.append("GROUP BY s.stocktake_id LIMIT 1");
        try {
            connection = getConnection();
            if (connection == null) {
                return null;
            }
            statement = connection.prepareStatement(sql.toString());
            statement.setLong(1, stocktakeId);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return getFromResultSet(resultSet);
            }
            return null;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findById failed", ex);
            return null;
        } finally {
            closeResources();
        }
    }

    /**
     * All counting lines of one stocktake, joined to batch + product for the
     * count/review screen. Optional keyword filters product name, SKU or
     * batch number.
     */
    public List<StocktakeItem> findItems(long stocktakeId, String keyword) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT i.*, p.product_name, p.sku, b.batch_number, ");
        sql.append("b.expiry_date, b.status AS batch_status, b.reserved_quantity ");
        sql.append("FROM stocktake_items i ");
        sql.append("JOIN inventory_batches b ON b.batch_id = i.batch_id ");
        sql.append("JOIN products p ON p.product_id = b.product_id ");
        sql.append("WHERE i.stocktake_id = ? ");
        if (keyword != null && !keyword.isEmpty()) {
            sql.append("AND (p.product_name LIKE ? OR p.sku LIKE ? OR b.batch_number LIKE ?) ");
        }
        sql.append("ORDER BY p.product_name ASC, b.expiry_date ASC, b.batch_number ASC");

        List<StocktakeItem> out = new ArrayList<>();
        try {
            connection = getConnection();
            if (connection == null) {
                return out;
            }
            statement = connection.prepareStatement(sql.toString());
            int i = 1;
            statement.setLong(i++, stocktakeId);
            if (keyword != null && !keyword.isEmpty()) {
                statement.setString(i++, "%" + keyword + "%");
                statement.setString(i++, "%" + keyword + "%");
                statement.setString(i++, "%" + keyword + "%");
            }
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                out.add(getItemFromResultSet(resultSet));
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findItems failed", ex);
        } finally {
            closeResources();
        }
        return out;
    }

    /* ==================== create ==================== */
    /** Insert a DRAFT stocktake; returns the new id or -1 on failure. */
    public long createStocktake(long createdBy) {
        String sql = "INSERT INTO stocktakes (created_by, status) VALUES (?, 'DRAFT')";
        try {
            connection = getConnection();
            if (connection == null) {
                return -1;
            }
            statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, createdBy);
            statement.executeUpdate();
            resultSet = statement.getGeneratedKeys();
            if (resultSet.next()) {
                return resultSet.getLong(1);
            }
            return -1;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "createStocktake failed", ex);
            return -1;
        } finally {
            closeResources();
        }
    }

    /* ==================== start ==================== */
    /**
     * DRAFT -> IN_PROGRESS in ONE transaction: lock the stocktake, verify
     * DRAFT, snapshot every current batch into stocktake_items, flip status.
     * Double-start fails on the status check, so duplicate items can never
     * be inserted (the unique key is a second safety net).
     */
    public StocktakeResult startStocktake(long stocktakeId) {
        Connection conn = null;
        try {
            conn = getConnection();
            if (conn == null) {
                return StocktakeResult.fail("db");
            }
            conn.setAutoCommit(false);

            StocktakeRow st = loadStocktakeForUpdate(conn, stocktakeId);
            if (st == null) {
                conn.rollback();
                return StocktakeResult.fail("notfound");
            }
            if (!"DRAFT".equals(st.status)) {
                conn.rollback();
                return StocktakeResult.fail("notdraft");
            }

            List<BatchSnapshot> batches = loadAllBatches(conn);
            if (batches.isEmpty()) {
                conn.rollback();
                return StocktakeResult.fail("empty");
            }
            for (BatchSnapshot b : batches) {
                insertItem(conn, stocktakeId, b);
            }
            updateStocktakeStatus(conn, stocktakeId, "IN_PROGRESS");

            conn.commit();
            return StocktakeResult.success();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "startStocktake failed — rolled back", ex);
            rollbackQuietly(conn);
            return StocktakeResult.fail("db");
        } finally {
            closeTxConnection(conn);
        }
    }

    /* ==================== save counts ==================== */
    /**
     * Save partial counting progress in ONE transaction. Only
     * stocktake_item_id + actual_quantity are trusted from the caller, and
     * every submitted id is checked against this stocktake's own item set —
     * counts belonging to another stocktake are rejected outright.
     * Blank inputs never reach this method (the servlet skips them), so a
     * submitted value always overwrites actual_quantity and refreshes the
     * provisional difference against the snapshot system_quantity.
     */
    public StocktakeResult saveCounts(long stocktakeId, Map<Long, Integer> counts) {
        Connection conn = null;
        try {
            conn = getConnection();
            if (conn == null) {
                return StocktakeResult.fail("db");
            }
            conn.setAutoCommit(false);

            StocktakeRow st = loadStocktakeForUpdate(conn, stocktakeId);
            if (st == null) {
                conn.rollback();
                return StocktakeResult.fail("notfound");
            }
            if (!"IN_PROGRESS".equals(st.status)) {
                conn.rollback();
                return StocktakeResult.fail("notinprogress");
            }

            Set<Long> ownedItemIds = loadItemIds(conn, stocktakeId);
            for (Map.Entry<Long, Integer> entry : counts.entrySet()) {
                long itemId = entry.getKey();
                int actual = entry.getValue();
                if (!ownedItemIds.contains(itemId)) {
                    conn.rollback();
                    return StocktakeResult.fail("baditem");
                }
                if (actual < 0) {
                    conn.rollback();
                    return StocktakeResult.fail("badquantity");
                }
                updateItemCount(conn, itemId, actual);
            }

            conn.commit();
            return StocktakeResult.success();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "saveCounts failed — rolled back", ex);
            rollbackQuietly(conn);
            return StocktakeResult.fail("db");
        } finally {
            closeTxConnection(conn);
        }
    }

    /* ==================== complete & reconcile ==================== */
    /**
     * IN_PROGRESS -> COMPLETED in ONE transaction:
     * lock stocktake -> verify status -> load items -> every item must have
     * an actual_quantity -> lock each batch FOR UPDATE -> reconcile against
     * the LIVE on_hand (not the start snapshot) -> refresh the item's
     * system/difference -> if difference != 0 set batch on_hand + safe
     * status and write a STOCKTAKE_ADJUSTMENT movement -> mark COMPLETED.
     * actual < reserved is rejected — reservations belong to the order
     * flow and are never auto-released here.
     */
    public StocktakeResult completeStocktake(long stocktakeId, long performedBy) {
        Connection conn = null;
        try {
            conn = getConnection();
            if (conn == null) {
                return StocktakeResult.fail("db");
            }
            conn.setAutoCommit(false);

            StocktakeRow st = loadStocktakeForUpdate(conn, stocktakeId);
            if (st == null) {
                conn.rollback();
                return StocktakeResult.fail("notfound");
            }
            if (!"IN_PROGRESS".equals(st.status)) {
                conn.rollback();
                return StocktakeResult.fail("notinprogress");
            }

            List<ItemRow> items = loadItems(conn, stocktakeId);
            for (ItemRow item : items) {
                if (item.actualQuantity == null) {
                    conn.rollback();
                    return StocktakeResult.fail("missingcounts");
                }
            }

            java.sql.Date today = new java.sql.Date(System.currentTimeMillis());
            for (ItemRow item : items) {
                BatchRow batch = loadBatchForUpdate(conn, item.batchId);
                if (batch == null) {
                    conn.rollback();
                    return StocktakeResult.fail("db");
                }
                int actual = item.actualQuantity;
                if (actual < 0) {
                    conn.rollback();
                    return StocktakeResult.fail("badquantity");
                }
                if (actual < batch.reservedQuantity) {
                    conn.rollback();
                    return StocktakeResult.fail("belowreserved",
                            "Batch " + batch.batchNumber + " has " + batch.reservedQuantity
                            + " reserved units but the physical count is only " + actual
                            + ". Resolve the reservation before completing the stocktake.");
                }
                int systemBefore = batch.onHandQuantity;
                int difference = actual - systemBefore;

                // Audit columns always reflect the locked live quantity.
                updateItemReconciled(conn, item.stocktakeItemId, systemBefore, difference);

                if (difference != 0) {
                    String newStatus = reconciledStatus(batch, actual, today);
                    updateBatchOnHand(conn, item.batchId, actual, newStatus);
                    insertMovement(conn, item.batchId, performedBy, difference,
                            systemBefore, actual,
                            batch.reservedQuantity, batch.reservedQuantity,
                            stocktakeId);
                }
            }

            completeStocktakeRow(conn, stocktakeId);

            conn.commit();
            return StocktakeResult.success();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "completeStocktake failed — rolled back", ex);
            rollbackQuietly(conn);
            return StocktakeResult.fail("db");
        } finally {
            closeTxConnection(conn);
        }
    }

    /* ==================== transaction internals ==================== */
    /** Small row holder for the locked stocktakes row. */
    private static class StocktakeRow {
        long stocktakeId;
        String status;
    }

    /** Small row holder for the batch snapshot at Start time. */
    private static class BatchSnapshot {
        long batchId;
        int onHandQuantity;
    }

    /** Small row holder for a stocktake_items row during save/complete. */
    private static class ItemRow {
        long stocktakeItemId;
        long batchId;
        Integer actualQuantity;
    }

    /** Small row holder for the batch being reconciled inside the tx. */
    private static class BatchRow {
        long batchId;
        String batchNumber;
        String status;
        java.sql.Date expiryDate;
        int onHandQuantity;
        int reservedQuantity;
    }

    /** SELECT ... FOR UPDATE — a second start/complete on it must wait. */
    private StocktakeRow loadStocktakeForUpdate(Connection conn, long stocktakeId)
            throws SQLException {
        String sql = "SELECT stocktake_id, status FROM stocktakes "
                + "WHERE stocktake_id = ? LIMIT 1 FOR UPDATE";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(1, stocktakeId);
            rs = ps.executeQuery();
            if (rs.next()) {
                StocktakeRow row = new StocktakeRow();
                row.stocktakeId = rs.getLong("stocktake_id");
                row.status = rs.getString("status");
                return row;
            }
            return null;
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    /** Every batch is in scope at Start — blocked and expired included. */
    private List<BatchSnapshot> loadAllBatches(Connection conn) throws SQLException {
        String sql = "SELECT batch_id, on_hand_quantity FROM inventory_batches";
        List<BatchSnapshot> out = new ArrayList<>();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                BatchSnapshot row = new BatchSnapshot();
                row.batchId = rs.getLong("batch_id");
                row.onHandQuantity = rs.getInt("on_hand_quantity");
                out.add(row);
            }
            return out;
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    /** The item ids owned by this stocktake — guards against forged ids. */
    private Set<Long> loadItemIds(Connection conn, long stocktakeId)
            throws SQLException {
        String sql = "SELECT stocktake_item_id FROM stocktake_items WHERE stocktake_id = ?";
        Set<Long> out = new HashSet<>();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(1, stocktakeId);
            rs = ps.executeQuery();
            while (rs.next()) {
                out.add(rs.getLong("stocktake_item_id"));
            }
            return out;
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    /** All items of the stocktake for the complete transaction. */
    private List<ItemRow> loadItems(Connection conn, long stocktakeId)
            throws SQLException {
        String sql = "SELECT stocktake_item_id, batch_id, actual_quantity "
                + "FROM stocktake_items WHERE stocktake_id = ?";
        List<ItemRow> out = new ArrayList<>();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(1, stocktakeId);
            rs = ps.executeQuery();
            while (rs.next()) {
                ItemRow row = new ItemRow();
                row.stocktakeItemId = rs.getLong("stocktake_item_id");
                row.batchId = rs.getLong("batch_id");
                int actual = rs.getInt("actual_quantity");
                if (rs.wasNull()) {
                    row.actualQuantity = null;
                } else {
                    row.actualQuantity = actual;
                }
                out.add(row);
            }
            return out;
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    /** SELECT ... FOR UPDATE on the batch — the live on_hand is the baseline. */
    private BatchRow loadBatchForUpdate(Connection conn, long batchId)
            throws SQLException {
        String sql = "SELECT batch_id, batch_number, status, expiry_date, "
                + "on_hand_quantity, reserved_quantity "
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
                row.batchNumber = rs.getString("batch_number");
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

    /** One stocktake_items row per batch in scope at Start. */
    private void insertItem(Connection conn, long stocktakeId, BatchSnapshot batch)
            throws SQLException {
        String sql = "INSERT INTO stocktake_items (stocktake_id, batch_id, system_quantity) "
                + "VALUES (?,?,?)";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(1, stocktakeId);
            ps.setLong(2, batch.batchId);
            ps.setInt(3, batch.onHandQuantity);
            ps.executeUpdate();
        } finally {
            closeQuietly(ps);
        }
    }

    /** Save a count + its provisional difference against the snapshot. */
    private void updateItemCount(Connection conn, long stocktakeItemId, int actual)
            throws SQLException {
        String sql = "UPDATE stocktake_items "
                + "SET actual_quantity = ?, difference_quantity = ? - system_quantity "
                + "WHERE stocktake_item_id = ?";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setInt(1, actual);
            ps.setInt(2, actual);
            ps.setLong(3, stocktakeItemId);
            ps.executeUpdate();
        } finally {
            closeQuietly(ps);
        }
    }

    /** At Complete the item stores the live baseline and final difference. */
    private void updateItemReconciled(Connection conn, long stocktakeItemId,
            int systemQuantity, int difference) throws SQLException {
        String sql = "UPDATE stocktake_items "
                + "SET system_quantity = ?, difference_quantity = ? "
                + "WHERE stocktake_item_id = ?";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setInt(1, systemQuantity);
            ps.setInt(2, difference);
            ps.setLong(3, stocktakeItemId);
            ps.executeUpdate();
        } finally {
            closeQuietly(ps);
        }
    }

    /**
     * Status after reconciliation — same rules as a manual adjustment:
     * BLOCKED is manual and stays; an expired date always wins; zero count
     * on a sellable batch is OUT_OF_STOCK; otherwise expiry decides.
     */
    private static String reconciledStatus(BatchRow batch, int actual,
            java.sql.Date today) {
        if ("BLOCKED".equals(batch.status)) {
            return "BLOCKED";
        }
        String status = InventoryDAO.statusForExpiry(batch.expiryDate, today);
        if (!"EXPIRED".equals(status) && actual <= 0) {
            return "OUT_OF_STOCK";
        }
        return status;
    }

    /** Write the reconciled on_hand and refreshed status onto the batch. */
    private void updateBatchOnHand(Connection conn, long batchId,
            int actual, String newStatus) throws SQLException {
        String sql = "UPDATE inventory_batches SET on_hand_quantity = ?, status = ? "
                + "WHERE batch_id = ?";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setInt(1, actual);
            ps.setString(2, newStatus);
            ps.setLong(3, batchId);
            ps.executeUpdate();
        } finally {
            closeQuietly(ps);
        }
    }

    /** One STOCKTAKE_ADJUSTMENT movement per non-zero difference. */
    private void insertMovement(Connection conn, long batchId, long performedBy,
            int difference, int onHandBefore, int onHandAfter,
            int reservedBefore, int reservedAfter,
            long stocktakeId) throws SQLException {
        String sql = "INSERT INTO inventory_movements "
                + "(batch_id, performed_by, movement_type, on_hand_change, "
                + " reserved_change, on_hand_before, on_hand_after, "
                + " reserved_before, reserved_after, reference_type, reference_id, reason) "
                + "VALUES (?,?, 'STOCKTAKE_ADJUSTMENT', ?,0,?,?,?,?, 'STOCKTAKE', ?, ?)";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(1, batchId);
            ps.setLong(2, performedBy);
            ps.setInt(3, difference);
            ps.setInt(4, onHandBefore);
            ps.setInt(5, onHandAfter);
            ps.setInt(6, reservedBefore);
            ps.setInt(7, reservedAfter);
            ps.setLong(8, stocktakeId);
            ps.setString(9, "Stocktake reconciliation #" + stocktakeId);
            ps.executeUpdate();
        } finally {
            closeQuietly(ps);
        }
    }

    private void updateStocktakeStatus(Connection conn, long stocktakeId,
            String status) throws SQLException {
        String sql = "UPDATE stocktakes SET status = ? WHERE stocktake_id = ?";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setString(1, status);
            ps.setLong(2, stocktakeId);
            ps.executeUpdate();
        } finally {
            closeQuietly(ps);
        }
    }

    private void completeStocktakeRow(Connection conn, long stocktakeId)
            throws SQLException {
        String sql = "UPDATE stocktakes SET status = 'COMPLETED', completed_at = NOW() "
                + "WHERE stocktake_id = ?";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(1, stocktakeId);
            ps.executeUpdate();
        } finally {
            closeQuietly(ps);
        }
    }

    /* ==================== small helpers ==================== */
    private void closeTxConnection(Connection conn) {
        if (conn == null) {
            return;
        }
        try {
            conn.setAutoCommit(true);
            conn.close();
        } catch (SQLException ex) {
            LOG.log(Level.WARNING, "closing tx connection failed", ex);
        }
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
