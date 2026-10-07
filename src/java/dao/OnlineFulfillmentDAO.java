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
 * Staff-side access to online order fulfillment — the NHAN_VIEN preparation
 * flow. Read side: the staff order list (all statuses, paginated + filtered)
 * and the picking detail (order + items + the DANG_GIU batch reservations the
 * customer already allocated). Write side: the four transitions this module
 * owns, each in ONE JDBC transaction.
 *
 *   CHO_XU_LY     -> DA_XAC_NHAN    (confirm)
 *   DA_XAC_NHAN   -> DANG_CHUAN_BI  (prepare)
 *   DANG_CHUAN_BI -> SAN_SANG       (ready)
 *   CHO_XU_LY     -> TU_CHOI        (reject + release reservations)
 *
 * The transition set is deliberately closed — DANG_GIAO / HOAN_TAT belong to
 * the later shipper module and are never written here. Confirm/prepare/ready
 * move status only; they never touch on_hand, reserved, or the reservation
 * rows (those stay DANG_GIU through SAN_SANG). Reject is the only path that
 * releases inventory: reserved_quantity down, reservations -> DA_GIAI_PHONG,
 * one GIAI_PHONG_GIU_HANG movement per reservation — all or nothing.
 *
 * Every transition locks the online_orders row FOR UPDATE and re-reads its
 * status under the lock, so a racing customer cancel (which locks the same
 * row) and a staff action can never both succeed.
 */
public class OnlineFulfillmentDAO extends DBContext {

    private static final Logger LOG = Logger.getLogger(OnlineFulfillmentDAO.class.getName());

    /**
     * Result of a transition attempt — a machine-readable error code the
     * servlet maps to ?err= (ORDER_NOT_FOUND | INVALID_STATUS |
     * RESERVATION_MISMATCH | RESERVATION_NOT_FOUND | DB_ERROR).
     */
    public static class FulfillmentResult {
        public final boolean ok;
        public final String error;
        public final String detail;

        private FulfillmentResult(boolean ok, String error, String detail) {
            this.ok = ok;
            this.error = error;
            this.detail = detail;
        }

        public static FulfillmentResult success() {
            return new FulfillmentResult(true, null, null);
        }

        public static FulfillmentResult fail(String error) {
            return new FulfillmentResult(false, error, null);
        }

        public static FulfillmentResult fail(String error, String detail) {
            return new FulfillmentResult(false, error, detail);
        }
    }

    /* ==================== staff order list ==================== */
    /** Count matching orders for the staff list pagination. */
    public int countOrders(String status, String search) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM online_orders o WHERE 1=1 ");
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
            LOG.log(Level.SEVERE, "countOrders failed", ex);
            return 0;
        } finally {
            closeResources();
        }
    }

    /**
     * One page of orders for staff — newest first. Filter by order_status and
     * a single search box matched against order id (exact), customer_name or
     * customer_phone (LIKE). All statuses are listed for visibility; the JSP
     * only renders action buttons on the states that allow them.
     */
    public List<OnlineOrder> findOrders(String status, String search,
            int offset, int limit) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT o.* FROM online_orders o WHERE 1=1 ");
        appendFilters(sql, status, search);
        sql.append("ORDER BY o.online_order_id DESC LIMIT ? OFFSET ?");
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
            LOG.log(Level.SEVERE, "findOrders failed", ex);
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

    /** Bind the filter params in the same order appendFilters emitted them.
     *  Returns the next free parameter index. */
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

    /* ==================== staff order detail ==================== */
    /** One order by PK — staff can view any order (no customer scoping). */
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
     * The order's reserved batch allocations — the picking list. One row per
     * reservation joined to its batch (number, expiry, storage location) and
     * the product (name, sku). DANG_GIU rows are what staff physically picks;
     * released rows are shown for history on cancelled/rejected orders. One
     * JOIN query — no per-item DAO loop.
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
    /** CHO_XU_LY -> DA_XAC_NHAN. Status only — stock and reservations untouched. */
    public FulfillmentResult confirmOrder(long orderId) {
        return transition(orderId, "CHO_XU_LY", "DA_XAC_NHAN");
    }

    /** DA_XAC_NHAN -> DANG_CHUAN_BI. Status only — staff starts picking. */
    public FulfillmentResult prepareOrder(long orderId) {
        return transition(orderId, "DA_XAC_NHAN", "DANG_CHUAN_BI");
    }

    /** DANG_CHUAN_BI -> SAN_SANG. Status only — ready for the shipper module. */
    public FulfillmentResult readyOrder(long orderId) {
        return transition(orderId, "DANG_CHUAN_BI", "SAN_SANG");
    }

    /**
     * Shared lock -> validate -> verify-reservation -> update -> commit flow
     * for the three status-only transitions.
     *
     * Reservation integrity is verified before every transition: for each
     * online_order_item the DANG_GIU reservations must sum to the ordered
     * quantity. A mismatch is manual/bad data — the order is left untouched
     * and RESERVATION_MISMATCH / RESERVATION_NOT_FOUND is returned instead of
     * silently repairing it.
     */
    private FulfillmentResult transition(long orderId,
            String expectedStatus, String newStatus) {
        Connection conn = null;
        try {
            conn = getConnection();
            if (conn == null) {
                return FulfillmentResult.fail("DB_ERROR");
            }
            conn.setAutoCommit(false);

            // Lock the order row — racing customer cancels and double-submits
            // both serialize here; exactly one transition wins.
            OrderRow order = lockOrder(conn, orderId);
            if (order == null) {
                conn.rollback();
                return FulfillmentResult.fail("ORDER_NOT_FOUND");
            }
            if (!expectedStatus.equals(order.orderStatus)) {
                conn.rollback();
                return FulfillmentResult.fail("INVALID_STATUS");
            }

            // The customer-facing reservations are the source of truth for
            // picking — refuse to advance an order whose allocation is broken.
            String integrity = verifyReservationIntegrity(conn, orderId);
            if (integrity != null) {
                conn.rollback();
                return FulfillmentResult.fail(integrity);
            }

            updateOrderStatus(conn, orderId, newStatus);
            conn.commit();
            return FulfillmentResult.success();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "transition to " + newStatus + " failed — rolled back", ex);
            rollbackQuietly(conn);
            return FulfillmentResult.fail("DB_ERROR");
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
     * CHO_XU_LY -> TU_CHOI with reservation release — the only multi-write
     * transaction in this module.
     *
     * Steps under one lock sequence (order -> reservations -> batches sorted
     * ASC, matching the customer cancel path so the two never deadlock):
     * verify status, lock DANG_GIU reservations, lock their batches in id
     * order, lower each batch's reserved_quantity (on_hand NEVER touched),
     * flip each reservation to DA_GIAI_PHONG with released_at, write one
     * GIAI_PHONG_GIU_HANG movement per reservation (performed_by = the staff
     * users.user_id), then mark the order TU_CHOI. Any failure rolls back the
     * whole thing — no half-released order can exist.
     */
    public FulfillmentResult rejectOrder(long orderId, long staffUserId) {
        Connection conn = null;
        try {
            conn = getConnection();
            if (conn == null) {
                return FulfillmentResult.fail("DB_ERROR");
            }
            conn.setAutoCommit(false);

            OrderRow order = lockOrder(conn, orderId);
            if (order == null) {
                conn.rollback();
                return FulfillmentResult.fail("ORDER_NOT_FOUND");
            }
            if (!"CHO_XU_LY".equals(order.orderStatus)) {
                conn.rollback();
                return FulfillmentResult.fail("INVALID_STATUS");
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
                    return FulfillmentResult.fail("DB_ERROR");
                }
                int reservedAfter = b.reservedQuantity - r.reservedQuantity;
                if (reservedAfter < 0) {
                    conn.rollback();
                    return FulfillmentResult.fail("DB_ERROR");
                }
                lowerReserved(conn, r.batchId, r.reservedQuantity);
                releaseReservation(conn, r.reservationId);
                insertReleaseMovement(conn, r.batchId, staffUserId, r.reservedQuantity,
                        b.onHandQuantity, b.reservedQuantity, reservedAfter, orderId);
                b.reservedQuantity = reservedAfter;
            }

            updateOrderStatus(conn, orderId, "TU_CHOI");
            conn.commit();
            return FulfillmentResult.success();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "rejectOrder failed — rolled back", ex);
            rollbackQuietly(conn);
            return FulfillmentResult.fail("DB_ERROR");
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

    /** Reservation row locked inside the reject tx. */
    private static class ReservationRow {
        long reservationId;
        long batchId;
        int reservedQuantity;
    }

    /** Batch row locked inside the reject tx. */
    private static class BatchRow {
        long batchId;
        int onHandQuantity;
        int reservedQuantity;
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
     * Reservation-integrity check inside the tx: every order item's DANG_GIU
     * reservations must sum to the ordered quantity. Returns null when the
     * allocation is consistent, otherwise the error code.
     */
    private String verifyReservationIntegrity(Connection conn, long orderId)
            throws SQLException {
        String sql = "SELECT i.online_order_item_id, i.quantity, "
                + "COALESCE(SUM(r.reserved_quantity),0) AS reserved "
                + "FROM online_order_items i "
                + "LEFT JOIN inventory_reservations r "
                + "  ON r.online_order_item_id = i.online_order_item_id "
                + " AND r.status = 'DANG_GIU' "
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
        sql.append("SELECT batch_id, on_hand_quantity, reserved_quantity ");
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
                row.onHandQuantity = rs.getInt("on_hand_quantity");
                row.reservedQuantity = rs.getInt("reserved_quantity");
                out.put(row.batchId, row);
            }
            return out;
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    /** reserved -= qty — on_hand is deliberately untouched in this module. */
    private void lowerReserved(Connection conn, long batchId, int quantity)
            throws SQLException {
        String sql = "UPDATE inventory_batches "
                + "SET reserved_quantity = reserved_quantity - ? WHERE batch_id = ?";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setInt(1, quantity);
            ps.setLong(2, batchId);
            ps.executeUpdate();
        } finally {
            closeQuietly(ps);
        }
    }

    /** Reservation -> DA_GIAI_PHONG with the release timestamp. */
    private void releaseReservation(Connection conn, long reservationId)
            throws SQLException {
        String sql = "UPDATE inventory_reservations "
                + "SET status = 'DA_GIAI_PHONG', released_at = CURRENT_TIMESTAMP "
                + "WHERE reservation_id = ?";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(1, reservationId);
            ps.executeUpdate();
        } finally {
            closeQuietly(ps);
        }
    }

    /** One GIAI_PHONG_GIU_HANG movement per released reservation — staff actor. */
    private void insertReleaseMovement(Connection conn, long batchId, long performedBy,
            int quantity, int onHand, int reservedBefore, int reservedAfter, long orderId)
            throws SQLException {
        String sql = "INSERT INTO inventory_movements "
                + "(batch_id, performed_by, movement_type, on_hand_change, "
                + " reserved_change, on_hand_before, on_hand_after, "
                + " reserved_before, reserved_after, reference_type, reference_id, reason) "
                + "VALUES (?,?,'GIAI_PHONG_GIU_HANG',0,?,?,?,?,?,'DON_HANG_ONLINE',?,?)";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(1, batchId);
            ps.setLong(2, performedBy);
            ps.setInt(3, -quantity);
            ps.setInt(4, onHand);
            ps.setInt(5, onHand);
            ps.setInt(6, reservedBefore);
            ps.setInt(7, reservedAfter);
            ps.setLong(8, orderId);
            ps.setString(9, "Nhân viên từ chối đơn trực tuyến #" + orderId);
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
