package dao;

import db.DBContext;
import model.InventoryReservation;
import model.OnlineOrder;
import model.OnlineOrderItem;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Shipper-side access to online orders — the NHAN_VIEN_GIAO_HANG delivery
 * queue. This module owns the last two transitions of the online order
 * lifecycle, each in ONE JDBC transaction:
 *
 *   SAN_SANG  -> DANG_GIAO   (start delivery — the stock-out point)
 *   DANG_GIAO -> HOAN_TAT    (complete delivery — status only)
 *
 * Stock-out happens ONLY at SAN_SANG -> DANG_GIAO: for every DANG_GIU
 * reservation the batch loses on_hand AND reserved together (saleable stock =
 * on_hand - reserved stays constant across dispatch), the reservation flips
 * to DA_HOAN_TAT, and one BAN_ONLINE movement is written per reservation.
 * HOAN_TAT never touches inventory again — it only verifies the DA_HOAN_TAT
 * rows still sum to the ordered quantities.
 *
 * This is a shared queue: there is no shipment/assignment table, so the first
 * shipper whose transaction locks the order row wins. A racing second attempt
 * re-reads the status under the lock and fails INVALID_STATUS — exactly one
 * transition can ever succeed.
 *
 * Reservations stay the source of truth for which batches leave the shelf —
 * this module never re-runs FEFO or ALLOCATABLE. It refuses to dispatch a
 * reservation whose batch is expired (expiry_date <= CURDATE()), HET_HAN, or
 * BI_KHOA (BATCH_NOT_DISPATCHABLE) or whose quantities no longer cover the
 * reserved amount (BATCH_INSUFFICIENT).
 */
public class DeliveryDAO extends DBContext {

    private static final Logger LOG = Logger.getLogger(DeliveryDAO.class.getName());

    /**
     * Result of a transition attempt — a machine-readable error code the
     * servlet maps to ?err= (ORDER_NOT_FOUND | INVALID_STATUS |
     * RESERVATION_NOT_FOUND | RESERVATION_MISMATCH | BATCH_NOT_FOUND |
     * BATCH_INSUFFICIENT | BATCH_NOT_DISPATCHABLE | FULFILLMENT_MISMATCH |
     * DB_ERROR).
     */
    public static class DeliveryResult {
        public final boolean ok;
        public final String error;
        public final String detail;

        private DeliveryResult(boolean ok, String error, String detail) {
            this.ok = ok;
            this.error = error;
            this.detail = detail;
        }

        public static DeliveryResult success() {
            return new DeliveryResult(true, null, null);
        }

        public static DeliveryResult fail(String error) {
            return new DeliveryResult(false, error, null);
        }

        public static DeliveryResult fail(String error, String detail) {
            return new DeliveryResult(false, error, detail);
        }
    }

    /* ==================== delivery queue ==================== */
    /**
     * Count matching orders for the queue pagination. The shipper only ever
     * sees SAN_SANG / DANG_GIAO / HOAN_TAT — the status filter is whitelisted
     * in the servlet before it reaches here.
     */
    public int countDeliveryOrders(String status, String search) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM online_orders o ");
        sql.append("WHERE o.order_status IN ('SAN_SANG','DANG_GIAO','HOAN_TAT') ");
        appendFilters(sql, status, search);
        try {
            connection = getConnection();
            if (connection == null) {
                return 0;
            }
            statement = connection.prepareStatement(sql.toString());
            bindFilters(statement, status, search);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
            return 0;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "countDeliveryOrders failed", ex);
            return 0;
        } finally {
            closeResources();
        }
    }

    /**
     * One page of the delivery queue. Work order: orders out on the road
     * first (DANG_GIAO), then the ready-to-dispatch pile (SAN_SANG), then the
     * completed history (HOAN_TAT) — newest first inside each group.
     */
    public List<OnlineOrder> findDeliveryOrders(String status, String search,
            int offset, int limit) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT o.* FROM online_orders o ");
        sql.append("WHERE o.order_status IN ('SAN_SANG','DANG_GIAO','HOAN_TAT') ");
        appendFilters(sql, status, search);
        sql.append("ORDER BY FIELD(o.order_status,'DANG_GIAO','SAN_SANG','HOAN_TAT'), ");
        sql.append("o.online_order_id DESC LIMIT ? OFFSET ?");
        List<OnlineOrder> out = new ArrayList<>();
        try {
            connection = getConnection();
            if (connection == null) {
                return out;
            }
            statement = connection.prepareStatement(sql.toString());
            int i = bindFilters(statement, status, search);
            statement.setInt(i++, limit);
            statement.setInt(i++, offset);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                out.add(mapOrder(resultSet));
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findDeliveryOrders failed", ex);
        } finally {
            closeResources();
        }
        return out;
    }

    /** Shared WHERE fragment for list + count. */
    private void appendFilters(StringBuilder sql, String status, String search) {
        if (status != null && !status.isEmpty()) {
            sql.append("AND o.order_status = ? ");
        }
        if (search != null && !search.isEmpty()) {
            sql.append("AND (o.customer_name LIKE ? OR o.customer_phone LIKE ? ");
            if (isPositiveLong(search)) {
                sql.append("OR o.online_order_id = ? ");
            }
            sql.append(") ");
        }
    }

    /** Bind the filter params in the same order appendFilters emitted them. */
    private int bindFilters(PreparedStatement ps, String status, String search)
            throws SQLException {
        int i = 1;
        if (status != null && !status.isEmpty()) {
            ps.setString(i++, status);
        }
        if (search != null && !search.isEmpty()) {
            ps.setString(i++, "%" + search + "%");
            ps.setString(i++, "%" + search + "%");
            if (isPositiveLong(search)) {
                ps.setLong(i++, Long.parseLong(search));
            }
        }
        return i;
    }

    private static boolean isPositiveLong(String s) {
        try {
            return Long.parseLong(s) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /* ==================== delivery detail ==================== */
    /** One order by PK — shipper can view any order in the delivery states. */
    public OnlineOrder findOrderById(long orderId) {
        String sql = "SELECT * FROM online_orders WHERE online_order_id = ? LIMIT 1";
        try {
            connection = getConnection();
            if (connection == null) {
                return null;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, orderId);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return mapOrder(resultSet);
            }
            return null;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findOrderById failed", ex);
            return null;
        } finally {
            closeResources();
        }
    }

    /** The order's item rows with product name + SKU joined. */
    public List<OnlineOrderItem> findOrderItems(long orderId) {
        String sql = "SELECT i.*, p.product_name, p.sku "
                + "FROM online_order_items i "
                + "JOIN products p ON p.product_id = i.product_id "
                + "WHERE i.online_order_id = ? "
                + "ORDER BY i.online_order_item_id ASC";
        List<OnlineOrderItem> out = new ArrayList<>();
        try {
            connection = getConnection();
            if (connection == null) {
                return out;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, orderId);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                OnlineOrderItem item = new OnlineOrderItem();
                item.setOnlineOrderItemId(resultSet.getLong("online_order_item_id"));
                item.setOnlineOrderId(resultSet.getLong("online_order_id"));
                item.setProductId(resultSet.getLong("product_id"));
                item.setQuantity(resultSet.getInt("quantity"));
                item.setSellingUnit(resultSet.getString("selling_unit"));
                item.setUnitPrice(resultSet.getBigDecimal("unit_price"));
                item.setSubtotal(resultSet.getBigDecimal("subtotal"));
                item.setProductName(resultSet.getString("product_name"));
                item.setSku(resultSet.getString("sku"));
                out.add(item);
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findOrderItems failed", ex);
        } finally {
            closeResources();
        }
        return out;
    }

    /**
     * The order's reserved batch allocations — read-only for the shipper (they
     * never pick batches; the customer's FEFO allocation is authoritative).
     * Shown on the detail page so the shipper knows which parcels to carry.
     */
    public List<InventoryReservation> findOrderReservations(long orderId) {
        String sql = "SELECT r.*, b.batch_number, b.expiry_date, b.storage_location, "
                + "p.product_name, p.sku "
                + "FROM inventory_reservations r "
                + "JOIN inventory_batches b ON b.batch_id = r.batch_id "
                + "JOIN online_order_items oi ON oi.online_order_item_id = r.online_order_item_id "
                + "JOIN products p ON p.product_id = oi.product_id "
                + "WHERE r.online_order_id = ? "
                + "ORDER BY r.online_order_item_id ASC, b.expiry_date ASC, r.reservation_id ASC";
        List<InventoryReservation> out = new ArrayList<>();
        try {
            connection = getConnection();
            if (connection == null) {
                return out;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, orderId);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                InventoryReservation r = new InventoryReservation();
                r.setReservationId(resultSet.getLong("reservation_id"));
                r.setOnlineOrderId(resultSet.getLong("online_order_id"));
                r.setOnlineOrderItemId(resultSet.getLong("online_order_item_id"));
                r.setBatchId(resultSet.getLong("batch_id"));
                r.setReservedQuantity(resultSet.getInt("reserved_quantity"));
                r.setStatus(resultSet.getString("status"));
                r.setReservedAt(resultSet.getTimestamp("reserved_at"));
                r.setReleasedAt(resultSet.getTimestamp("released_at"));
                r.setBatchNumber(resultSet.getString("batch_number"));
                r.setExpiryDate(resultSet.getDate("expiry_date"));
                r.setStorageLocation(resultSet.getString("storage_location"));
                r.setProductName(resultSet.getString("product_name"));
                r.setSku(resultSet.getString("sku"));
                out.add(r);
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findOrderReservations failed", ex);
        } finally {
            closeResources();
        }
        return out;
    }

    /* ==================== transitions ==================== */
    /**
     * SAN_SANG -> DANG_GIAO — the stock-out transaction.
     *
     * Steps under one lock sequence (order -> DANG_GIU reservations in id
     * order -> their batches sorted ASC — the same order every other writer
     * uses, so no deadlock):
     *
     *   1. Lock the order row FOR UPDATE, re-read status — must be SAN_SANG.
     *   2. Verify per-item DANG_GIU reservation sums equal ordered quantities
     *      (RESERVATION_NOT_FOUND / RESERVATION_MISMATCH on bad data).
     *   3. Lock the DANG_GIU reservation rows FOR UPDATE.
     *   4. Lock the referenced batches FOR UPDATE in batch_id order.
     *   5. Per reservation: batch must exist (BATCH_NOT_FOUND), reserved >=
     *      qty and on_hand >= qty (BATCH_INSUFFICIENT), expiry_date >
     *      CURDATE() and status not HET_HAN / BI_KHOA
     *      (BATCH_NOT_DISPATCHABLE).
     *   6. Per reservation: on_hand -= qty AND reserved -= qty (running
     *      per-batch values so two reservations on one batch both see the
     *      post-write numbers), refresh batch status (on_hand 0 -> HET_HANG,
     *      else InventoryDAO.statusForExpiry — BI_KHOA batches already failed
     *      above so they never reach here), reservation -> DA_HOAN_TAT with
     *      released_at left unchanged, one BAN_ONLINE movement with
     *      performed_by = the shipper's users.user_id.
     *   7. Flip the order to DANG_GIAO last, then commit. Any failure rolls
     *      back everything — no half-dispatched order can exist.
     */
    public DeliveryResult startDelivery(long orderId, long shipperUserId) {
        Connection conn = null;
        try {
            conn = getConnection();
            if (conn == null) {
                return DeliveryResult.fail("DB_ERROR");
            }
            conn.setAutoCommit(false);

            OrderRow order = lockOrder(conn, orderId);
            if (order == null) {
                conn.rollback();
                return DeliveryResult.fail("ORDER_NOT_FOUND");
            }
            if (!"SAN_SANG".equals(order.orderStatus)) {
                conn.rollback();
                return DeliveryResult.fail("INVALID_STATUS");
            }

            // The allocation must still be intact before stock leaves the shelf.
            String integrity = verifyReservationIntegrity(conn, orderId, "DANG_GIU");
            if (integrity != null) {
                conn.rollback();
                return DeliveryResult.fail(integrity);
            }

            // Live reservations of this order, locked FOR UPDATE.
            List<ReservationRow> reservations = lockReservations(conn, orderId);
            List<Long> batchIds = new ArrayList<>();
            for (ReservationRow r : reservations) {
                if (!batchIds.contains(r.batchId)) {
                    batchIds.add(r.batchId);
                }
            }
            Collections.sort(batchIds);
            Map<Long, BatchRow> batches = lockBatchesById(conn, batchIds);

            for (ReservationRow r : reservations) {
                BatchRow b = batches.get(r.batchId);
                if (b == null) {
                    conn.rollback();
                    return DeliveryResult.fail("BATCH_NOT_FOUND");
                }
                if (b.reservedQuantity < r.reservedQuantity
                        || b.onHandQuantity < r.reservedQuantity) {
                    conn.rollback();
                    return DeliveryResult.fail("BATCH_INSUFFICIENT");
                }
                // Never dispatch expired, expired-flagged or blocked medicine.
                if (b.expiryDate == null || !b.expiryDate.after(today())
                        || "HET_HAN".equals(b.status) || "BI_KHOA".equals(b.status)) {
                    conn.rollback();
                    return DeliveryResult.fail("BATCH_NOT_DISPATCHABLE");
                }

                int onHandAfter = b.onHandQuantity - r.reservedQuantity;
                int reservedAfter = b.reservedQuantity - r.reservedQuantity;

                updateBatchStockOut(conn, r.batchId, r.reservedQuantity,
                        statusAfterStockOut(onHandAfter, b.expiryDate));
                fulfillReservation(conn, r.reservationId);
                insertSaleMovement(conn, r.batchId, shipperUserId, r.reservedQuantity,
                        b.onHandQuantity, onHandAfter, b.reservedQuantity,
                        reservedAfter, orderId);

                // Keep the running per-batch values — a second reservation on
                // the same batch must see this write.
                b.onHandQuantity = onHandAfter;
                b.reservedQuantity = reservedAfter;
            }

            updateOrderStatus(conn, orderId, "DANG_GIAO");
            conn.commit();
            return DeliveryResult.success();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "startDelivery failed — rolled back", ex);
            rollbackQuietly(conn);
            return DeliveryResult.fail("DB_ERROR");
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

    /**
     * DANG_GIAO -> HOAN_TAT — delivery confirmed. Status only: inventory must
     * NOT be touched again (stock-out already happened at dispatch). The only
     * check is that the DA_HOAN_TAT reservations still sum to the ordered
     * quantities (FULFILLMENT_MISMATCH on tampered data).
     */
    public DeliveryResult completeDelivery(long orderId) {
        Connection conn = null;
        try {
            conn = getConnection();
            if (conn == null) {
                return DeliveryResult.fail("DB_ERROR");
            }
            conn.setAutoCommit(false);

            OrderRow order = lockOrder(conn, orderId);
            if (order == null) {
                conn.rollback();
                return DeliveryResult.fail("ORDER_NOT_FOUND");
            }
            if (!"DANG_GIAO".equals(order.orderStatus)) {
                conn.rollback();
                return DeliveryResult.fail("INVALID_STATUS");
            }

            String integrity = verifyReservationIntegrity(conn, orderId, "DA_HOAN_TAT");
            if (integrity != null) {
                conn.rollback();
                return DeliveryResult.fail("FULFILLMENT_MISMATCH");
            }

            updateOrderStatus(conn, orderId, "HOAN_TAT");
            conn.commit();
            return DeliveryResult.success();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "completeDelivery failed — rolled back", ex);
            rollbackQuietly(conn);
            return DeliveryResult.fail("DB_ERROR");
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
    /** Order row locked inside the tx. */
    private static class OrderRow {
        long onlineOrderId;
        String orderStatus;
    }

    /** Reservation row locked inside the tx. */
    private static class ReservationRow {
        long reservationId;
        long batchId;
        int reservedQuantity;
    }

    /** Batch row locked inside the tx — carries expiry + status for the dispatchable check. */
    private static class BatchRow {
        long batchId;
        java.sql.Date expiryDate;
        int onHandQuantity;
        int reservedQuantity;
        String status;
    }

    /** The order row FOR UPDATE — status re-read under the lock. */
    private OrderRow lockOrder(Connection conn, long orderId) throws SQLException {
        String sql = "SELECT online_order_id, order_status "
                + "FROM online_orders WHERE online_order_id = ? FOR UPDATE";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(1, orderId);
            rs = ps.executeQuery();
            if (rs.next()) {
                OrderRow row = new OrderRow();
                row.onlineOrderId = rs.getLong("online_order_id");
                row.orderStatus = rs.getString("order_status");
                return row;
            }
            return null;
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    /**
     * Per-item allocation check inside the tx: every order item's
     * reservations in the given status must sum to the ordered quantity.
     * Returns null when consistent, otherwise the error code.
     */
    private String verifyReservationIntegrity(Connection conn, long orderId,
            String reservationStatus) throws SQLException {
        String sql = "SELECT i.online_order_item_id, i.quantity, "
                + "COALESCE(SUM(r.reserved_quantity),0) AS reserved "
                + "FROM online_order_items i "
                + "LEFT JOIN inventory_reservations r "
                + "  ON r.online_order_item_id = i.online_order_item_id "
                + " AND r.status = '" + reservationStatus + "' "
                + "WHERE i.online_order_id = ? "
                + "GROUP BY i.online_order_item_id, i.quantity";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(1, orderId);
            rs = ps.executeQuery();
            while (rs.next()) {
                int ordered = rs.getInt("quantity");
                int reserved = rs.getInt("reserved");
                if (reserved == 0) {
                    return "RESERVATION_NOT_FOUND";
                }
                if (reserved != ordered) {
                    return "RESERVATION_MISMATCH";
                }
            }
            return null;
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    /** The order's DANG_GIU reservations, locked FOR UPDATE in id order. */
    private List<ReservationRow> lockReservations(Connection conn, long orderId)
            throws SQLException {
        String sql = "SELECT reservation_id, batch_id, reserved_quantity "
                + "FROM inventory_reservations "
                + "WHERE online_order_id = ? AND status = 'DANG_GIU' "
                + "ORDER BY reservation_id ASC FOR UPDATE";
        List<ReservationRow> out = new ArrayList<>();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(1, orderId);
            rs = ps.executeQuery();
            while (rs.next()) {
                ReservationRow row = new ReservationRow();
                row.reservationId = rs.getLong("reservation_id");
                row.batchId = rs.getLong("batch_id");
                row.reservedQuantity = rs.getInt("reserved_quantity");
                out.add(row);
            }
            return out;
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    /** Lock the reservation batches by PK in sorted order — the lock-order rule. */
    private Map<Long, BatchRow> lockBatchesById(Connection conn, List<Long> ids)
            throws SQLException {
        Map<Long, BatchRow> out = new HashMap<>();
        if (ids.isEmpty()) {
            return out;
        }
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT batch_id, expiry_date, on_hand_quantity, reserved_quantity, status ");
        sql.append("FROM inventory_batches WHERE batch_id IN (");
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                sql.append(",");
            }
            sql.append("?");
        }
        sql.append(") ORDER BY batch_id ASC FOR UPDATE");
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql.toString());
            for (int i = 0; i < ids.size(); i++) {
                ps.setLong(i + 1, ids.get(i));
            }
            rs = ps.executeQuery();
            while (rs.next()) {
                BatchRow row = new BatchRow();
                row.batchId = rs.getLong("batch_id");
                row.expiryDate = rs.getDate("expiry_date");
                row.onHandQuantity = rs.getInt("on_hand_quantity");
                row.reservedQuantity = rs.getInt("reserved_quantity");
                row.status = rs.getString("status");
                out.put(row.batchId, row);
            }
            return out;
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    /**
     * Batch status after stock leaves: on_hand 0 -> HET_HANG, otherwise the
     * expiry-driven status (CO_SAN / SAP_HET_HAN / HET_HAN) via the shared
     * InventoryDAO helper.
     */
    private String statusAfterStockOut(int onHandAfter, java.sql.Date expiryDate) {
        if (onHandAfter <= 0) {
            return "HET_HANG";
        }
        return InventoryDAO.statusForExpiry(expiryDate, today());
    }

    /** on_hand -= qty AND reserved -= qty together, plus the refreshed status. */
    private void updateBatchStockOut(Connection conn, long batchId, int quantity,
            String newStatus) throws SQLException {
        String sql = "UPDATE inventory_batches "
                + "SET on_hand_quantity = on_hand_quantity - ?, "
                + "    reserved_quantity = reserved_quantity - ?, "
                + "    status = ? "
                + "WHERE batch_id = ?";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setInt(1, quantity);
            ps.setInt(2, quantity);
            ps.setString(3, newStatus);
            ps.setLong(4, batchId);
            ps.executeUpdate();
        } finally {
            closeQuietly(ps);
        }
    }

    /** Reservation -> DA_HOAN_TAT. released_at is deliberately NOT touched. */
    private void fulfillReservation(Connection conn, long reservationId)
            throws SQLException {
        String sql = "UPDATE inventory_reservations "
                + "SET status = 'DA_HOAN_TAT' WHERE reservation_id = ?";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(1, reservationId);
            ps.executeUpdate();
        } finally {
            closeQuietly(ps);
        }
    }

    /** One BAN_ONLINE movement per fulfilled reservation — shipper actor. */
    private void insertSaleMovement(Connection conn, long batchId, long performedBy,
            int quantity, int onHandBefore, int onHandAfter,
            int reservedBefore, int reservedAfter, long orderId)
            throws SQLException {
        String sql = "INSERT INTO inventory_movements "
                + "(batch_id, performed_by, movement_type, on_hand_change, "
                + " reserved_change, on_hand_before, on_hand_after, "
                + " reserved_before, reserved_after, reference_type, reference_id, reason) "
                + "VALUES (?,?,'BAN_ONLINE',?,?,?,?,?,?,'DON_HANG_ONLINE',?,?)";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(1, batchId);
            ps.setLong(2, performedBy);
            ps.setInt(3, -quantity);
            ps.setInt(4, -quantity);
            ps.setInt(5, onHandBefore);
            ps.setInt(6, onHandAfter);
            ps.setInt(7, reservedBefore);
            ps.setInt(8, reservedAfter);
            ps.setLong(9, orderId);
            ps.setString(10, "Xuất kho giao đơn trực tuyến #" + orderId);
            ps.executeUpdate();
        } finally {
            closeQuietly(ps);
        }
    }

    private void updateOrderStatus(Connection conn, long orderId, String status)
            throws SQLException {
        String sql = "UPDATE online_orders SET order_status = ? "
                + "WHERE online_order_id = ?";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setString(1, status);
            ps.setLong(2, orderId);
            ps.executeUpdate();
        } finally {
            closeQuietly(ps);
        }
    }

    /* ==================== mapping ==================== */
    private OnlineOrder mapOrder(ResultSet rs) throws SQLException {
        OnlineOrder o = new OnlineOrder();
        o.setOnlineOrderId(rs.getLong("online_order_id"));
        o.setCustomerId(rs.getLong("customer_id"));
        o.setCustomerName(rs.getString("customer_name"));
        o.setCustomerPhone(rs.getString("customer_phone"));
        o.setProvinceCity(rs.getString("province_city"));
        o.setDistrict(rs.getString("district"));
        o.setWard(rs.getString("ward"));
        o.setDetailedAddress(rs.getString("detailed_address"));
        o.setPaymentMethod(rs.getString("payment_method"));
        o.setPaymentStatus(rs.getString("payment_status"));
        o.setTotalAmount(rs.getBigDecimal("total_amount"));
        o.setOrderStatus(rs.getString("order_status"));
        o.setCreatedAt(rs.getTimestamp("created_at"));
        o.setUpdatedAt(rs.getTimestamp("updated_at"));
        return o;
    }

    /* ==================== small helpers ==================== */
    private static java.sql.Date today() {
        return new java.sql.Date(System.currentTimeMillis());
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
