package dao;

import db.DBContext;
import java.math.BigDecimal;
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
 * Owns the DB side of a VNPAY return: lock the order + its online_payments row,
 * validate the signed result against persisted state, then either mark the
 * payment THANH_CONG (order payment_status DA_THANH_TOAN) or run the
 * failed-payment transaction — release every DANG_GIU reservation exactly like
 * a customer cancel (reserved down, DA_GIAI_PHONG, GIAI_PHONG_GIU_HANG
 * movement) and mark payment THAT_BAI + order DA_HUY.
 *
 * Success NEVER touches inventory — stock stays reserved until delivery.
 * Failure NEVER touches on_hand — only reserved_quantity is released.
 *
 * Lock order everywhere: online_orders → online_payments → DANG_GIU
 * reservations → inventory_batches sorted ASC — identical to the existing
 * cancel/reject paths so transactions can never deadlock against each other.
 */
public class VnpayPaymentDAO extends DBContext {

    private static final Logger LOG = Logger.getLogger(VnpayPaymentDAO.class.getName());

    /** Machine-readable result of processing one VNPAY return. */
    public static class PaymentResult {
        public final String code;     // SUCCESS | PAYMENT_FAILED | ALREADY_* | error codes
        public final long orderId;    // resolved order id, -1 when unknown

        private PaymentResult(String code, long orderId) {
            this.code = code;
            this.orderId = orderId;
        }
    }

    /**
     * Authoritative order amount read AFTER the caller verified the signature.
     * Used by the servlet to re-check the signed vnp_Amount before trusting
     * the success/failure branch (integrity check, not a state write).
     *
     * @return the OnlineOrder-lite row, or null when the order does not exist.
     */
    public OrderRowView findOrder(long orderId) {
        String sql = "SELECT online_order_id, payment_method, payment_status, "
                + "order_status, total_amount "
                + "FROM online_orders WHERE online_order_id = ? LIMIT 1";
        try {
            connection = getConnection();
            if (connection == null) {
                return null;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, orderId);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                OrderRowView o = new OrderRowView();
                o.onlineOrderId = resultSet.getLong("online_order_id");
                o.paymentMethod = resultSet.getString("payment_method");
                o.paymentStatus = resultSet.getString("payment_status");
                o.orderStatus = resultSet.getString("order_status");
                o.totalAmount = resultSet.getBigDecimal("total_amount");
                return o;
            }
            return null;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findOrder failed", ex);
            return null;
        } finally {
            closeResources();
        }
    }

    /** Small read-only projection of the order for the amount check. */
    public static class OrderRowView {
        public long onlineOrderId;
        public String paymentMethod;
        public String paymentStatus;
        public String orderStatus;
        public BigDecimal totalAmount;
    }

    /**
     * Successful VNPAY return (signature already verified by the servlet,
     * vnp_ResponseCode = "00" AND vnp_TransactionStatus = "00").
     *
     * One transaction: lock order + payment row, re-check method/status/amount
     * under the locks, mark payment THANH_CONG (+ transaction_reference,
     * paid_at) and order payment_status DA_THANH_TOAN. Order status stays
     * CHO_XU_LY; inventory is untouched.
     *
     * Idempotent: a replayed success on an already-paid row returns
     * ALREADY_SUCCESS without a second write.
     */
    public PaymentResult processSuccess(long orderId, long signedAmountX100,
            String transactionNo) {
        Connection conn = null;
        try {
            conn = getConnection();
            if (conn == null) {
                return new PaymentResult("DB_ERROR", orderId);
            }
            conn.setAutoCommit(false);

            // 1) Lock the order row — method/status/amount re-read under lock.
            OrderRow order = lockOrder(conn, orderId);
            if (order == null) {
                conn.rollback();
                return new PaymentResult("ORDER_NOT_FOUND", orderId);
            }
            if (!"THANH_TOAN_TRUC_TUYEN".equals(order.paymentMethod)) {
                conn.rollback();
                return new PaymentResult("INVALID_PAYMENT_STATE", orderId);
            }

            // 2) Integrity: signed VNPAY amount must equal the DB total x 100.
            long expected = order.totalAmount.multiply(BigDecimal.valueOf(100)).longValue();
            if (signedAmountX100 != expected) {
                conn.rollback();
                LOG.log(Level.WARNING,
                        "VNPAY amount mismatch for order {0}: signed {1} vs DB {2}",
                        new Object[]{orderId, signedAmountX100, expected});
                return new PaymentResult("AMOUNT_MISMATCH", orderId);
            }

            // 3) Lock the order's VNPAY payment row.
            PaymentRow payment = lockPayment(conn, orderId);
            if (payment == null) {
                conn.rollback();
                return new PaymentResult("PAYMENT_NOT_FOUND", orderId);
            }
            if (!"VNPAY".equals(payment.provider)
                    || payment.amount.compareTo(order.totalAmount) != 0) {
                conn.rollback();
                return new PaymentResult("INVALID_PAYMENT_STATE", orderId);
            }

            // 4) Terminal states — idempotent, never move backwards.
            if ("THANH_CONG".equals(payment.paymentStatus)
                    && "DA_THANH_TOAN".equals(order.paymentStatus)) {
                conn.rollback();
                return new PaymentResult("ALREADY_SUCCESS", orderId);
            }
            if ("THAT_BAI".equals(payment.paymentStatus)
                    || "THAT_BAI".equals(order.paymentStatus)
                    || "DA_HUY".equals(order.orderStatus)) {
                conn.rollback();
                return new PaymentResult("INVALID_PAYMENT_STATE", orderId);
            }
            if (!"CHO_XU_LY".equals(payment.paymentStatus)
                    || !"CHO_THANH_TOAN".equals(order.paymentStatus)) {
                conn.rollback();
                return new PaymentResult("INVALID_PAYMENT_STATE", orderId);
            }

            // 5) Money confirmed — flag only, never stock.
            markPaymentSuccess(conn, payment.paymentId, transactionNo);
            updateOrderPaymentStatus(conn, orderId, "DA_THANH_TOAN");
            conn.commit();
            return new PaymentResult("SUCCESS", orderId);
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "processSuccess failed — rolled back", ex);
            rollbackQuietly(conn);
            return new PaymentResult("DB_ERROR", orderId);
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
     * Failed / cancelled VNPAY return (signature verified, result not "00").
     *
     * One transaction: lock order + payment row, verify both still pending,
     * then release every DANG_GIU reservation exactly like customer cancel —
     * reserved_quantity down (on_hand never touched), reservation
     * DA_GIAI_PHONG + released_at, one GIAI_PHONG_GIU_HANG movement per
     * reservation (performed_by NULL — the provider is the actor). Finally
     * payment THAT_BAI, order payment_status THAT_BAI + order_status DA_HUY.
     *
     * Idempotent: replayed failure on the terminal state returns
     * ALREADY_FAILED without releasing stock a second time.
     */
    public PaymentResult processFailure(long orderId, long signedAmountX100,
            String transactionNo) {
        Connection conn = null;
        try {
            conn = getConnection();
            if (conn == null) {
                return new PaymentResult("DB_ERROR", orderId);
            }
            conn.setAutoCommit(false);

            OrderRow order = lockOrder(conn, orderId);
            if (order == null) {
                conn.rollback();
                return new PaymentResult("ORDER_NOT_FOUND", orderId);
            }
            if (!"THANH_TOAN_TRUC_TUYEN".equals(order.paymentMethod)) {
                conn.rollback();
                return new PaymentResult("INVALID_PAYMENT_STATE", orderId);
            }

            long expected = order.totalAmount.multiply(BigDecimal.valueOf(100)).longValue();
            if (signedAmountX100 != expected) {
                conn.rollback();
                LOG.log(Level.WARNING,
                        "VNPAY amount mismatch for order {0}: signed {1} vs DB {2}",
                        new Object[]{orderId, signedAmountX100, expected});
                return new PaymentResult("AMOUNT_MISMATCH", orderId);
            }

            PaymentRow payment = lockPayment(conn, orderId);
            if (payment == null) {
                conn.rollback();
                return new PaymentResult("PAYMENT_NOT_FOUND", orderId);
            }
            if (!"VNPAY".equals(payment.provider)
                    || payment.amount.compareTo(order.totalAmount) != 0) {
                conn.rollback();
                return new PaymentResult("INVALID_PAYMENT_STATE", orderId);
            }

            // Terminal states — idempotent, never move backwards.
            if ("THAT_BAI".equals(payment.paymentStatus)
                    && "THAT_BAI".equals(order.paymentStatus)
                    && "DA_HUY".equals(order.orderStatus)) {
                conn.rollback();
                return new PaymentResult("ALREADY_FAILED", orderId);
            }
            if ("THANH_CONG".equals(payment.paymentStatus)
                    || "DA_THANH_TOAN".equals(order.paymentStatus)) {
                // A failure return can never un-pay a captured payment.
                conn.rollback();
                return new PaymentResult("INVALID_PAYMENT_STATE", orderId);
            }
            if (!"CHO_XU_LY".equals(payment.paymentStatus)
                    || !"CHO_THANH_TOAN".equals(order.paymentStatus)) {
                conn.rollback();
                return new PaymentResult("INVALID_PAYMENT_STATE", orderId);
            }

            // Release stock — same lock order as cancelOrder/rejectOrder.
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
                    return new PaymentResult("DB_ERROR", orderId);
                }
                int reservedAfter = b.reservedQuantity - r.reservedQuantity;
                if (reservedAfter < 0) {
                    conn.rollback();
                    return new PaymentResult("DB_ERROR", orderId);
                }
                lowerReserved(conn, r.batchId, r.reservedQuantity);
                releaseReservation(conn, r.reservationId);
                insertReleaseMovement(conn, r.batchId, r.reservedQuantity,
                        b.onHandQuantity, b.reservedQuantity, reservedAfter, orderId);
                b.reservedQuantity = reservedAfter;
            }

            markPaymentFailed(conn, payment.paymentId, transactionNo);
            updateOrderPaymentStatus(conn, orderId, "THAT_BAI");
            updateOrderStatus(conn, orderId, "DA_HUY");
            conn.commit();
            return new PaymentResult("PAYMENT_FAILED", orderId);
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "processFailure failed — rolled back", ex);
            rollbackQuietly(conn);
            return new PaymentResult("DB_ERROR", orderId);
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
    private static class OrderRow {
        long onlineOrderId;
        String paymentMethod;
        String paymentStatus;
        String orderStatus;
        BigDecimal totalAmount;
    }

    private static class PaymentRow {
        long paymentId;
        String provider;
        BigDecimal amount;
        String paymentStatus;
    }

    private static class ReservationRow {
        long reservationId;
        long batchId;
        int reservedQuantity;
    }

    private static class BatchRow {
        long batchId;
        int onHandQuantity;
        int reservedQuantity;
    }

    /** Order row FOR UPDATE — first in the module-wide lock order. */
    private OrderRow lockOrder(Connection conn, long orderId) throws SQLException {
        String sql = "SELECT online_order_id, payment_method, payment_status, "
                + "order_status, total_amount "
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
                row.paymentMethod = rs.getString("payment_method");
                row.paymentStatus = rs.getString("payment_status");
                row.orderStatus = rs.getString("order_status");
                row.totalAmount = rs.getBigDecimal("total_amount");
                return row;
            }
            return null;
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    /** The order's VNPAY payment row FOR UPDATE — one row is expected. */
    private PaymentRow lockPayment(Connection conn, long orderId) throws SQLException {
        String sql = "SELECT payment_id, provider, amount, payment_status "
                + "FROM online_payments WHERE online_order_id = ? "
                + "ORDER BY payment_id ASC LIMIT 1 FOR UPDATE";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(1, orderId);
            rs = ps.executeQuery();
            if (rs.next()) {
                PaymentRow row = new PaymentRow();
                row.paymentId = rs.getLong("payment_id");
                row.provider = rs.getString("provider");
                row.amount = rs.getBigDecimal("amount");
                row.paymentStatus = rs.getString("payment_status");
                return row;
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

    /** online_payments -> THANH_CONG + provider transaction ref + paid_at. */
    private void markPaymentSuccess(Connection conn, long paymentId, String transactionNo)
            throws SQLException {
        String sql = "UPDATE online_payments SET payment_status = 'THANH_CONG', "
                + "transaction_reference = ?, paid_at = CURRENT_TIMESTAMP "
                + "WHERE payment_id = ?";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setString(1, transactionNo);
            ps.setLong(2, paymentId);
            ps.executeUpdate();
        } finally {
            closeQuietly(ps);
        }
    }

    /** online_payments -> THAT_BAI (+ transaction ref when VNPAY sent one). */
    private void markPaymentFailed(Connection conn, long paymentId, String transactionNo)
            throws SQLException {
        String sql = "UPDATE online_payments SET payment_status = 'THAT_BAI', "
                + "transaction_reference = COALESCE(?, transaction_reference) "
                + "WHERE payment_id = ?";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setString(1, transactionNo);
            ps.setLong(2, paymentId);
            ps.executeUpdate();
        } finally {
            closeQuietly(ps);
        }
    }

    private void updateOrderPaymentStatus(Connection conn, long orderId, String status)
            throws SQLException {
        String sql = "UPDATE online_orders SET payment_status = ? "
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

    /** reserved -= qty — on_hand is deliberately untouched. */
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

    /**
     * One GIAI_PHONG_GIU_HANG movement per released reservation. performed_by
     * is NULL — the actor here is the payment provider, not a staff user.
     */
    private void insertReleaseMovement(Connection conn, long batchId,
            int quantity, int onHand, int reservedBefore, int reservedAfter, long orderId)
            throws SQLException {
        String sql = "INSERT INTO inventory_movements "
                + "(batch_id, performed_by, movement_type, on_hand_change, "
                + " reserved_change, on_hand_before, on_hand_after, "
                + " reserved_before, reserved_after, reference_type, reference_id, reason) "
                + "VALUES (?,NULL,'GIAI_PHONG_GIU_HANG',0,?,?,?,?,?,'DON_HANG_ONLINE',?,?)";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(1, batchId);
            ps.setInt(2, -quantity);
            ps.setInt(3, onHand);
            ps.setInt(4, onHand);
            ps.setInt(5, reservedBefore);
            ps.setInt(6, reservedAfter);
            ps.setLong(7, orderId);
            ps.setString(8, "Giải phóng giữ hàng do thanh toán VNPAY thất bại/hủy, đơn #" + orderId);
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
}
