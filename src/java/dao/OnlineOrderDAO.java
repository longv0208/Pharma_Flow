package dao;

import db.DBContext;
import model.CartItem;
import model.OnlineOrder;
import model.OnlineOrderItem;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Access to everything a customer online order touches: `customer_profiles`
 * (customer_id resolution), `carts` + `cart_items` (what is being ordered),
 * `products` (authoritative status/type/flag/price under lock),
 * `inventory_batches` + `inventory_reservations` + `inventory_movements`
 * (FEFO reservation), and `online_orders` + `online_order_items`.
 *
 * placeOrder(...) and cancelOrder(...) each run in ONE JDBC transaction — an
 * order can never exist without its items, its reservations, the batch
 * reserved-quantity updates and the audit movements (rule.md §28). Nothing the
 * browser sent is trusted: price, stock and sellability are re-read under lock.
 *
 * Placing an order NEVER changes on_hand — it only raises reserved_quantity
 * (GIU_HANG_ONLINE). The real stock-out belongs to the later fulfillment flow.
 */
public class OnlineOrderDAO extends DBContext {

    private static final Logger LOG = Logger.getLogger(OnlineOrderDAO.class.getName());

    /**
     * Result of a placeOrder / cancelOrder attempt — a machine-readable error
     * code the servlet maps to ?err= plus the new order id on success.
     */
    public static class OrderResult {
        public final boolean ok;
        public final String error;
        public final String detail;
        public final long orderId;

        private OrderResult(boolean ok, String error, String detail, long orderId) {
            this.ok = ok;
            this.error = error;
            this.detail = detail;
            this.orderId = orderId;
        }

        public static OrderResult success(long orderId) {
            return new OrderResult(true, null, null, orderId);
        }

        public static OrderResult fail(String error) {
            return new OrderResult(false, error, null, -1);
        }

        public static OrderResult fail(String error, String detail) {
            return new OrderResult(false, error, detail, -1);
        }
    }

    /* ==================== customer_id resolution ==================== */
    /**
     * customer_profiles.customer_id for a login — orders belong to the profile,
     * never to users.user_id taken from the request. Returns null when the
     * account has no profile row (a setup error — never faked).
     */
    public Long resolveCustomerId(long userId) {
        String sql = "SELECT customer_id FROM customer_profiles WHERE user_id = ? LIMIT 1";
        try {
            connection = getConnection();
            if (connection == null) {
                return null;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, userId);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getLong("customer_id");
            }
            return null;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "resolveCustomerId failed", ex);
            return null;
        } finally {
            closeResources();
        }
    }

    /* ==================== order history / detail (read side) ==================== */
    /** Total orders of one customer — paging helper for /orders. */
    public int countOrders(long customerId) {
        String sql = "SELECT COUNT(*) FROM online_orders WHERE customer_id = ?";
        try {
            connection = getConnection();
            if (connection == null) {
                return 0;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, customerId);
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

    /** One page of the customer's orders — newest first, own orders only. */
    public List<OnlineOrder> findOrders(long customerId, int offset, int limit) {
        String sql = "SELECT * FROM online_orders WHERE customer_id = ? "
                + "ORDER BY online_order_id DESC LIMIT ? OFFSET ?";
        List<OnlineOrder> out = new ArrayList<>();
        try {
            connection = getConnection();
            if (connection == null) {
                return out;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, customerId);
            statement.setInt(2, limit);
            statement.setInt(3, offset);
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

    /**
     * One order by PK scoped to the owning customer — the IDOR guard. Returns
     * null both when the order does not exist and when it belongs to someone
     * else, so existence of other customers' orders is never leaked.
     */
    public OnlineOrder findOrderForCustomer(long orderId, long customerId) {
        String sql = "SELECT * FROM online_orders "
                + "WHERE online_order_id = ? AND customer_id = ? LIMIT 1";
        try {
            connection = getConnection();
            if (connection == null) {
                return null;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, orderId);
            statement.setLong(2, customerId);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return mapOrder(resultSet);
            }
            return null;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findOrderForCustomer failed", ex);
            return null;
        } finally {
            closeResources();
        }
    }

    /** All item rows of one order with product display fields joined. */
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

    /* ==================== place order (the critical transaction) ==================== */
    /**
     * Place the customer's online order in ONE JDBC transaction.
     *
     * Steps, all inside the tx:
     * 1) Resolve customer_id, lock the customer's DANG_HOAT_DONG cart FOR
     *    UPDATE and load its items — empty cart refuses.
     * 2) Lock the cart products in product_id order (SELECT ... FOR UPDATE),
     *    re-read status / product_type / online_sale_allowed / selling_price /
     *    selling_unit from the DB. Anything no longer HOAT_DONG + KHONG_KE_DON
     *    + online_sale_allowed fails the whole order.
     * 3) Total is computed from the locked DB prices — the cart display copy
     *    is ignored entirely.
     * 4) FEFO-allocate each line over allocatable batches locked FOR UPDATE
     *    (expiry ASC, batch_id ASC); short stock anywhere rolls everything
     *    back and the cart stays DANG_HOAT_DONG.
     * 5) Insert online_orders (CHO_XU_LY, COD, KHONG_YEU_CAU) + items +
     *    inventory_reservations (DANG_GIU), raise each batch's
     *    reserved_quantity — on_hand is NEVER touched — and write one
     *    GIU_HANG_ONLINE movement per affected batch.
     * 6) Flip the cart DANG_HOAT_DONG -> DA_CHUYEN_THANH_DON and commit.
     */
    public OrderResult placeOrder(long userId, long customerId,
            String customerName, String customerPhone, String provinceCity,
            String district, String ward, String detailedAddress) {
        Connection conn = null;
        try {
            conn = getConnection();
            if (conn == null) {
                return OrderResult.fail("DB_ERROR");
            }
            conn.setAutoCommit(false);

            // 1) Lock the active cart — the order consumes exactly this cart.
            CartRow cart = lockActiveCart(conn, customerId);
            if (cart == null) {
                conn.rollback();
                return OrderResult.fail("EMPTY_CART");
            }
            List<CartItemRow> items = findCartItems(conn, cart.cartId);
            if (items.isEmpty()) {
                conn.rollback();
                return OrderResult.fail("EMPTY_CART");
            }

            // 2) Lock products in id order — consistent lock order avoids
            //    deadlocks between concurrent checkouts of the same items.
            List<Long> ids = new ArrayList<>();
            for (CartItemRow line : items) {
                ids.add(line.productId);
            }
            Collections.sort(ids);
            Map<Long, ProductRow> products = lockProducts(conn, ids);

            BigDecimal total = BigDecimal.ZERO;
            for (CartItemRow line : items) {
                ProductRow p = products.get(line.productId);
                if (p == null) {
                    conn.rollback();
                    return OrderResult.fail("PRODUCT_NOT_FOUND",
                            String.valueOf(line.productId));
                }
                if (!"HOAT_DONG".equals(p.status)) {
                    conn.rollback();
                    return OrderResult.fail("PRODUCT_INACTIVE", p.productName);
                }
                if ("KE_DON".equals(p.productType)) {
                    conn.rollback();
                    return OrderResult.fail("PRESCRIPTION_REQUIRED", p.productName);
                }
                if ("HAN_CHE".equals(p.productType)) {
                    conn.rollback();
                    return OrderResult.fail("RESTRICTED_NOT_ALLOWED", p.productName);
                }
                if (!p.onlineSaleAllowed) {
                    conn.rollback();
                    return OrderResult.fail("NOT_SELLABLE_ONLINE", p.productName);
                }
                line.unitPrice = p.sellingPrice;
                line.sellingUnit = p.sellingUnit;
                line.productName = p.productName;
                total = total.add(p.sellingPrice.multiply(
                        BigDecimal.valueOf(line.quantity)));
            }

            // 4) FEFO allocation per line — batches locked in expiry order.
            //    allocations[lineIndex][batchId] = qty so one item can split
            //    across several batches; a shared batch still gets one UPDATE.
            List<Map<Long, Integer>> allocations = new ArrayList<>();
            Map<Long, BatchRow> touchedBatches = new HashMap<>();
            for (int i = 0; i < items.size(); i++) {
                allocations.add(new HashMap<>());
            }
            for (int i = 0; i < items.size(); i++) {
                CartItemRow line = items.get(i);
                int remaining = line.quantity;
                List<BatchRow> batches = lockAllocatableBatches(conn, line.productId);
                for (BatchRow b : batches) {
                    if (remaining <= 0) {
                        break;
                    }
                    int free = b.onHandQuantity - b.reservedQuantity;
                    if (free <= 0) {
                        continue;
                    }
                    int take = Math.min(free, remaining);
                    Integer planned = allocations.get(i).get(b.batchId);
                    if (planned == null) {
                        planned = 0;
                    }
                    allocations.get(i).put(b.batchId, planned + take);
                    BatchRow shared = touchedBatches.get(b.batchId);
                    if (shared == null) {
                        touchedBatches.put(b.batchId, b);
                        shared = b;
                    }
                    shared.plannedReserved = shared.plannedReserved + take;
                    remaining = remaining - take;
                }
                if (remaining > 0) {
                    conn.rollback();
                    return OrderResult.fail("INSUFFICIENT_STOCK",
                            line.productName + " (thiếu " + remaining + ")");
                }
            }

            // 5) Order header + items + reservations + stock + movements.
            long orderId = insertOrder(conn, customerId, customerName,
                    customerPhone, provinceCity, district, ward, detailedAddress,
                    total);
            if (orderId <= 0) {
                conn.rollback();
                return OrderResult.fail("DB_ERROR");
            }

            for (int i = 0; i < items.size(); i++) {
                CartItemRow line = items.get(i);
                BigDecimal subtotal = line.unitPrice.multiply(
                        BigDecimal.valueOf(line.quantity));
                long orderItemId = insertOrderItem(conn, orderId, line.productId,
                        line.quantity, line.sellingUnit, line.unitPrice, subtotal);
                if (orderItemId <= 0) {
                    conn.rollback();
                    return OrderResult.fail("DB_ERROR");
                }
                for (Map.Entry<Long, Integer> a : allocations.get(i).entrySet()) {
                    insertReservation(conn, orderId, orderItemId,
                            a.getKey(), a.getValue());
                }
            }

            for (Map.Entry<Long, BatchRow> e : touchedBatches.entrySet()) {
                BatchRow b = e.getValue();
                int reservedAfter = b.reservedQuantity + b.plannedReserved;
                raiseReserved(conn, b.batchId, b.plannedReserved);
                insertReserveMovement(conn, b.batchId, userId, b.plannedReserved,
                        b.onHandQuantity, b.reservedQuantity, reservedAfter, orderId);
            }

            // 6) The cart is consumed — a new one is created lazily on next add.
            markCartConverted(conn, cart.cartId);
            conn.commit();
            return OrderResult.success(orderId);
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "placeOrder failed — rolled back", ex);
            rollbackQuietly(conn);
            return OrderResult.fail("DB_ERROR");
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

    /* ==================== customer cancellation ==================== */
    /**
     * Customer cancel of a CHO_XU_LY order in ONE JDBC transaction.
     *
     * Steps, all inside the tx:
     * 1) SELECT the order FOR UPDATE — it must belong to this customer and be
     *    CHO_XU_LY; anything else is rejected (covers both IDOR and
     *    double-cancel).
     * 2) Lock the order's DANG_GIU reservations and their batches.
     * 3) Per reservation: batch.reserved_quantity -= reserved (never below 0),
     *    reservation -> DA_GIAI_PHONG with released_at, and one
     *    GIAI_PHONG_GIU_HANG movement recording the reserved delta. on_hand is
     *    NEVER touched.
     * 4) Order CHO_XU_LY -> DA_HUY, commit.
     */
    public OrderResult cancelOrder(long userId, long customerId, long orderId) {
        Connection conn = null;
        try {
            conn = getConnection();
            if (conn == null) {
                return OrderResult.fail("DB_ERROR");
            }
            conn.setAutoCommit(false);

            // 1) Lock the order — ownership + status checked under the lock so
            //    a racing staff confirm or a second cancel can't slip through.
            OrderRow order = lockOrder(conn, orderId);
            if (order == null || order.customerId != customerId) {
                conn.rollback();
                return OrderResult.fail("ORDER_NOT_FOUND");
            }
            if (!"CHO_XU_LY".equals(order.orderStatus)) {
                conn.rollback();
                return OrderResult.fail("NOT_CANCELLABLE");
            }

            // 2) Live reservations of this order.
            List<ReservationRow> reservations = lockReservations(conn, orderId);
            List<Long> batchIds = new ArrayList<>();
            for (ReservationRow r : reservations) {
                if (!batchIds.contains(r.batchId)) {
                    batchIds.add(r.batchId);
                }
            }
            Collections.sort(batchIds);
            Map<Long, BatchRow> batches = lockBatchesById(conn, batchIds);

            // 3) Release each reservation — batch reserved down, movement out.
            for (ReservationRow r : reservations) {
                BatchRow b = batches.get(r.batchId);
                if (b == null) {
                    conn.rollback();
                    return OrderResult.fail("DB_ERROR");
                }
                int reservedAfter = b.reservedQuantity - r.reservedQuantity;
                if (reservedAfter < 0) {
                    conn.rollback();
                    return OrderResult.fail("DB_ERROR");
                }
                lowerReserved(conn, r.batchId, r.reservedQuantity);
                releaseReservation(conn, r.reservationId);
                insertReleaseMovement(conn, r.batchId, userId, r.reservedQuantity,
                        b.onHandQuantity, b.reservedQuantity, reservedAfter, orderId);
                b.reservedQuantity = reservedAfter;
            }

            // 4) Terminal state for this module.
            updateOrderStatus(conn, orderId, "DA_HUY");
            conn.commit();
            return OrderResult.success(orderId);
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "cancelOrder failed — rolled back", ex);
            rollbackQuietly(conn);
            return OrderResult.fail("DB_ERROR");
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
    /** Cart row locked inside the place-order tx. */
    private static class CartRow {
        long cartId;
    }

    /** Cart line locked inside the tx; price/unit/name filled from the product lock. */
    private static class CartItemRow {
        long productId;
        int quantity;
        String productName;
        String sellingUnit;
        BigDecimal unitPrice;
    }

    /** Product snapshot locked inside the tx. */
    private static class ProductRow {
        long productId;
        String productName;
        String productType;
        String status;
        boolean onlineSaleAllowed;
        String sellingUnit;
        BigDecimal sellingPrice;
    }

    /** Batch snapshot locked inside the tx; plannedReserved accumulates. */
    private static class BatchRow {
        long batchId;
        int onHandQuantity;
        int reservedQuantity;
        int plannedReserved;
    }

    /** Order row locked inside the cancel tx. */
    private static class OrderRow {
        long onlineOrderId;
        long customerId;
        String orderStatus;
    }

    /** Reservation row locked inside the cancel tx. */
    private static class ReservationRow {
        long reservationId;
        long batchId;
        int reservedQuantity;
    }

    /** The customer's DANG_HOAT_DONG cart, locked FOR UPDATE. */
    private CartRow lockActiveCart(Connection conn, long customerId) throws SQLException {
        String sql = "SELECT cart_id FROM carts "
                + "WHERE customer_id = ? AND status = 'DANG_HOAT_DONG' "
                + "ORDER BY cart_id DESC LIMIT 1 FOR UPDATE";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(1, customerId);
            rs = ps.executeQuery();
            if (rs.next()) {
                CartRow c = new CartRow();
                c.cartId = rs.getLong("cart_id");
                return c;
            }
            return null;
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    /** The locked cart's item rows — ids + quantities only; price comes from
     *  the product lock, never from anything the browser saw. */
    private List<CartItemRow> findCartItems(Connection conn, long cartId)
            throws SQLException {
        String sql = "SELECT product_id, quantity FROM cart_items "
                + "WHERE cart_id = ? ORDER BY cart_item_id ASC";
        List<CartItemRow> out = new ArrayList<>();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(1, cartId);
            rs = ps.executeQuery();
            while (rs.next()) {
                CartItemRow row = new CartItemRow();
                row.productId = rs.getLong("product_id");
                row.quantity = rs.getInt("quantity");
                out.add(row);
            }
            return out;
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    /** Lock all cart products at once, ordered by PK — the lock-order rule. */
    private Map<Long, ProductRow> lockProducts(Connection conn, List<Long> ids)
            throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT product_id, product_name, product_type, status, ");
        sql.append("online_sale_allowed, selling_unit, selling_price ");
        sql.append("FROM products WHERE product_id IN (");
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                sql.append(",");
            }
            sql.append("?");
        }
        sql.append(") ORDER BY product_id ASC FOR UPDATE");
        Map<Long, ProductRow> out = new HashMap<>();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql.toString());
            for (int i = 0; i < ids.size(); i++) {
                ps.setLong(i + 1, ids.get(i));
            }
            rs = ps.executeQuery();
            while (rs.next()) {
                ProductRow row = new ProductRow();
                row.productId = rs.getLong("product_id");
                row.productName = rs.getString("product_name");
                row.productType = rs.getString("product_type");
                row.status = rs.getString("status");
                row.onlineSaleAllowed = rs.getBoolean("online_sale_allowed");
                row.sellingUnit = rs.getString("selling_unit");
                row.sellingPrice = rs.getBigDecimal("selling_price");
                out.put(row.productId, row);
            }
            return out;
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    /**
     * FEFO batch list for one product — allocatable batches with sellable
     * stock, locked FOR UPDATE in expiry/batch_id order so concurrent
     * checkouts walk the rows in the same order.
     */
    private List<BatchRow> lockAllocatableBatches(Connection conn, long productId)
            throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT b.batch_id, b.on_hand_quantity, b.reserved_quantity ");
        sql.append("FROM inventory_batches b ");
        sql.append("WHERE b.product_id = ? AND ").append(InventoryDAO.ALLOCATABLE);
        sql.append(" AND b.on_hand_quantity - b.reserved_quantity > 0 ");
        sql.append("ORDER BY b.expiry_date ASC, b.batch_id ASC FOR UPDATE");
        List<BatchRow> out = new ArrayList<>();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql.toString());
            ps.setLong(1, productId);
            rs = ps.executeQuery();
            while (rs.next()) {
                BatchRow row = new BatchRow();
                row.batchId = rs.getLong("batch_id");
                row.onHandQuantity = rs.getInt("on_hand_quantity");
                row.reservedQuantity = rs.getInt("reserved_quantity");
                row.plannedReserved = 0;
                out.add(row);
            }
            return out;
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    /** The order header — CHO_XU_LY + COD + KHONG_YEU_CAU per this module. */
    private long insertOrder(Connection conn, long customerId, String customerName,
            String customerPhone, String provinceCity, String district, String ward,
            String detailedAddress, BigDecimal total) throws SQLException {
        String sql = "INSERT INTO online_orders "
                + "(customer_id, customer_name, customer_phone, province_city, district, "
                + " ward, detailed_address, payment_method, payment_status, "
                + " total_amount, order_status) "
                + "VALUES (?,?,?,?,?,?,?,'THANH_TOAN_KHI_NHAN_HANG','KHONG_YEU_CAU',?,'CHO_XU_LY')";
        PreparedStatement ps = null;
        ResultSet keys = null;
        try {
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, customerId);
            ps.setString(2, customerName);
            ps.setString(3, customerPhone);
            ps.setString(4, provinceCity);
            ps.setString(5, district);
            ps.setString(6, ward);
            ps.setString(7, detailedAddress);
            ps.setBigDecimal(8, total);
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

    private long insertOrderItem(Connection conn, long orderId, long productId,
            int quantity, String sellingUnit, BigDecimal unitPrice, BigDecimal subtotal)
            throws SQLException {
        String sql = "INSERT INTO online_order_items "
                + "(online_order_id, product_id, quantity, selling_unit, unit_price, subtotal) "
                + "VALUES (?,?,?,?,?,?)";
        PreparedStatement ps = null;
        ResultSet keys = null;
        try {
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, orderId);
            ps.setLong(2, productId);
            ps.setInt(3, quantity);
            ps.setString(4, sellingUnit);
            ps.setBigDecimal(5, unitPrice);
            ps.setBigDecimal(6, subtotal);
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

    /** One DANG_GIU reservation row — the item-to-batch allocation record. */
    private void insertReservation(Connection conn, long orderId, long orderItemId,
            long batchId, int quantity) throws SQLException {
        String sql = "INSERT INTO inventory_reservations "
                + "(online_order_id, online_order_item_id, batch_id, reserved_quantity, status) "
                + "VALUES (?,?,?,?,'DANG_GIU')";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(1, orderId);
            ps.setLong(2, orderItemId);
            ps.setLong(3, batchId);
            ps.setInt(4, quantity);
            ps.executeUpdate();
        } finally {
            closeQuietly(ps);
        }
    }

    /** reserved += qty — on_hand is deliberately untouched (online flow). */
    private void raiseReserved(Connection conn, long batchId, int quantity)
            throws SQLException {
        String sql = "UPDATE inventory_batches "
                + "SET reserved_quantity = reserved_quantity + ? WHERE batch_id = ?";
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

    /** One GIU_HANG_ONLINE movement per batch — on_hand flat, reserved up. */
    private void insertReserveMovement(Connection conn, long batchId, long performedBy,
            int quantity, int onHand, int reservedBefore, int reservedAfter, long orderId)
            throws SQLException {
        String sql = "INSERT INTO inventory_movements "
                + "(batch_id, performed_by, movement_type, on_hand_change, "
                + " reserved_change, on_hand_before, on_hand_after, "
                + " reserved_before, reserved_after, reference_type, reference_id, reason) "
                + "VALUES (?,?,'GIU_HANG_ONLINE',0,?,?,?,?,?,'DON_HANG_ONLINE',?,?)";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(1, batchId);
            ps.setLong(2, performedBy);
            ps.setInt(3, quantity);
            ps.setInt(4, onHand);
            ps.setInt(5, onHand);
            ps.setInt(6, reservedBefore);
            ps.setInt(7, reservedAfter);
            ps.setLong(8, orderId);
            ps.setString(9, "Giữ hàng cho đơn trực tuyến #" + orderId);
            ps.executeUpdate();
        } finally {
            closeQuietly(ps);
        }
    }

    /** Cart consumed by the order — DANG_HOAT_DONG -> DA_CHUYEN_THANH_DON. */
    private void markCartConverted(Connection conn, long cartId) throws SQLException {
        String sql = "UPDATE carts SET status = 'DA_CHUYEN_THANH_DON' "
                + "WHERE cart_id = ? AND status = 'DANG_HOAT_DONG'";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(1, cartId);
            ps.executeUpdate();
        } finally {
            closeQuietly(ps);
        }
    }

    /* ==================== cancel internals ==================== */
    /** Order row locked FOR UPDATE inside the cancel tx. */
    private OrderRow lockOrder(Connection conn, long orderId) throws SQLException {
        String sql = "SELECT online_order_id, customer_id, order_status "
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
                row.customerId = rs.getLong("customer_id");
                row.orderStatus = rs.getString("order_status");
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

    /** reserved -= qty — on_hand is deliberately untouched (online flow). */
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

    /** One GIAI_PHONG_GIU_HANG movement per released reservation. */
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
            ps.setString(9, "Khách hàng hủy đơn trực tuyến #" + orderId);
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

    /* ==================== mapping helpers ==================== */
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
