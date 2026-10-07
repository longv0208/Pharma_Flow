package dao;

import db.DBContext;
import model.Cart;
import model.CartItem;
import model.Product;
import model.ProductType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Access to `carts` + `cart_items` for the KHACH_HANG storefront cart.
 *
 * One customer has at most one DANG_HOAT_DONG cart — enforced by
 * getOrCreateActiveCart which locks the parent customer_profiles row before
 * checking/inserting (the DB has no unique constraint for this). Read methods
 * use the inherited connection + closeResources(); the cart-creation write
 * runs in a small dedicated transaction like PosDAO's (rule.md §28).
 *
 * The cart page never trusts cached prices/stock — findItems joins products
 * and the live saleable aggregate fresh on every render.
 */
public class CartDAO extends DBContext {

    private static final Logger LOG = Logger.getLogger(CartDAO.class.getName());

    /* ==================== mapping ==================== */
    public Cart getFromResultSet(ResultSet rs) throws SQLException {
        Cart c = new Cart();
        c.setCartId(rs.getLong("cart_id"));
        c.setCustomerId(rs.getLong("customer_id"));
        c.setStatus(rs.getString("status"));
        c.setCreatedAt(rs.getTimestamp("created_at"));
        c.setUpdatedAt(rs.getTimestamp("updated_at"));
        return c;
    }

    /* ==================== cart lookup / creation ==================== */
    /**
     * The customer's DANG_HOAT_DONG cart, or null when none exists. Read-only —
     * used for the cart page so plain viewing never creates a row.
     */
    public Cart findActiveCart(long customerId) {
        String sql = "SELECT cart_id, customer_id, status, created_at, updated_at "
                + "FROM carts WHERE customer_id = ? AND status = 'DANG_HOAT_DONG' "
                + "ORDER BY cart_id DESC LIMIT 1";
        try {
            connection = getConnection();
            if (connection == null) {
                return null;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, customerId);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return getFromResultSet(resultSet);
            }
            return null;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findActiveCart failed", ex);
            return null;
        } finally {
            closeResources();
        }
    }

    /**
     * The customer's single active cart — created lazily on first use. The
     * customer_profiles row is locked FOR UPDATE first so two concurrent
     * requests can never create two DANG_HOAT_DONG carts for one customer.
     * Returns null on DB failure.
     */
    public Cart getOrCreateActiveCart(long customerId) {
        Connection conn = null;
        try {
            conn = getConnection();
            if (conn == null) {
                return null;
            }
            conn.setAutoCommit(false);

            // Serialize cart creation per customer — no unique key on carts.
            lockCustomerProfile(conn, customerId);

            Cart cart = selectActiveCart(conn, customerId);
            if (cart == null) {
                long cartId = insertCart(conn, customerId);
                if (cartId <= 0) {
                    conn.rollback();
                    return null;
                }
                cart = new Cart();
                cart.setCartId(cartId);
                cart.setCustomerId(customerId);
                cart.setStatus("DANG_HOAT_DONG");
            }
            conn.commit();
            return cart;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "getOrCreateActiveCart failed — rolled back", ex);
            rollbackQuietly(conn);
            return null;
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

    /* ==================== cart items ==================== */
    /**
     * All items of one cart with product display fields + live saleable stock
     * joined in one query (no N+1). sellableOnline is re-derived per row from
     * the current product status/type/flag so the page can warn about items
     * that became un-orderable after they were added.
     */
    public List<CartItem> findItems(long cartId) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ci.cart_item_id, ci.cart_id, ci.product_id, ci.quantity, ");
        sql.append("p.product_name, p.sku, p.selling_unit, p.selling_price, ");
        sql.append("p.status, p.product_type, p.online_sale_allowed, ");
        sql.append("COALESCE(SUM(CASE WHEN ").append(InventoryDAO.ALLOCATABLE);
        sql.append(" THEN b.on_hand_quantity - b.reserved_quantity ELSE 0 END),0) AS saleable ");
        sql.append("FROM cart_items ci ");
        sql.append("JOIN products p ON p.product_id = ci.product_id ");
        sql.append("LEFT JOIN inventory_batches b ON b.product_id = ci.product_id ");
        sql.append("WHERE ci.cart_id = ? ");
        sql.append("GROUP BY ci.cart_item_id, ci.cart_id, ci.product_id, ci.quantity, ");
        sql.append("p.product_name, p.sku, p.selling_unit, p.selling_price, ");
        sql.append("p.status, p.product_type, p.online_sale_allowed ");
        sql.append("ORDER BY ci.cart_item_id ASC");

        List<CartItem> out = new ArrayList<>();
        try {
            connection = getConnection();
            if (connection == null) {
                return out;
            }
            statement = connection.prepareStatement(sql.toString());
            statement.setLong(1, cartId);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                CartItem item = new CartItem();
                item.setCartItemId(resultSet.getLong("cart_item_id"));
                item.setCartId(resultSet.getLong("cart_id"));
                item.setProductId(resultSet.getLong("product_id"));
                item.setQuantity(resultSet.getInt("quantity"));
                item.setProductName(resultSet.getString("product_name"));
                item.setSku(resultSet.getString("sku"));
                item.setSellingUnit(resultSet.getString("selling_unit"));
                item.setSellingPrice(resultSet.getBigDecimal("selling_price"));
                item.setSaleableQuantity(resultSet.getLong("saleable"));
                boolean sellable = "HOAT_DONG".equals(resultSet.getString("status"))
                        && "KHONG_KE_DON".equals(resultSet.getString("product_type"))
                        && resultSet.getBoolean("online_sale_allowed");
                item.setSellableOnline(sellable);
                out.add(item);
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findItems failed", ex);
        } finally {
            closeResources();
        }
        return out;
    }

    /**
     * One product for the add/update validation — current status, type,
     * online flag and live saleable quantity. NGUNG_HOAT_DONG / KE_DON /
     * HAN_CHE products are returned too so the servlet can give a specific
     * reason instead of a bare "not found".
     */
    public Product findProductForCart(long productId) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT p.product_id, p.product_name, p.sku, p.product_type, ");
        sql.append("p.selling_unit, p.selling_price, p.online_sale_allowed, p.status, ");
        sql.append("COALESCE(SUM(CASE WHEN ").append(InventoryDAO.ALLOCATABLE);
        sql.append(" THEN b.on_hand_quantity - b.reserved_quantity ELSE 0 END),0) AS saleable ");
        sql.append("FROM products p ");
        sql.append("LEFT JOIN inventory_batches b ON b.product_id = p.product_id ");
        sql.append("WHERE p.product_id = ? ");
        sql.append("GROUP BY p.product_id, p.product_name, p.sku, p.product_type, ");
        sql.append("p.selling_unit, p.selling_price, p.online_sale_allowed, p.status");
        try {
            connection = getConnection();
            if (connection == null) {
                return null;
            }
            statement = connection.prepareStatement(sql.toString());
            statement.setLong(1, productId);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                Product p = new Product();
                p.setProductId(resultSet.getLong("product_id"));
                p.setProductName(resultSet.getString("product_name"));
                p.setSku(resultSet.getString("sku"));
                p.setProductType(ProductType.fromString(resultSet.getString("product_type")));
                p.setSellingUnit(resultSet.getString("selling_unit"));
                p.setSellingPrice(resultSet.getBigDecimal("selling_price"));
                p.setOnlineSaleAllowed(resultSet.getBoolean("online_sale_allowed"));
                p.setStatus(resultSet.getString("status"));
                p.setAvailableQuantity(resultSet.getLong("saleable"));
                return p;
            }
            return null;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findProductForCart failed", ex);
            return null;
        } finally {
            closeResources();
        }
    }

    /**
     * Add `delta` units of a product — insert a new row, or bump the existing
     * one (uq_cart_product). Caller has already validated the resulting total
     * against live saleable stock.
     */
    public int upsertItem(long cartId, long productId, int delta) {
        String sql = "INSERT INTO cart_items (cart_id, product_id, quantity) VALUES (?,?,?) "
                + "ON DUPLICATE KEY UPDATE quantity = quantity + VALUES(quantity)";
        try {
            connection = getConnection();
            if (connection == null) {
                return 0;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, cartId);
            statement.setLong(2, productId);
            statement.setInt(3, delta);
            return statement.executeUpdate();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "upsertItem failed", ex);
            return 0;
        } finally {
            closeResources();
        }
    }

    /** Set the exact quantity of one line — keyed by cart_id so one customer
     *  can never touch another customer's items. */
    public int updateItemQuantity(long cartId, long productId, int quantity) {
        String sql = "UPDATE cart_items SET quantity = ? "
                + "WHERE cart_id = ? AND product_id = ?";
        try {
            connection = getConnection();
            if (connection == null) {
                return 0;
            }
            statement = connection.prepareStatement(sql);
            statement.setInt(1, quantity);
            statement.setLong(2, cartId);
            statement.setLong(3, productId);
            return statement.executeUpdate();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "updateItemQuantity failed", ex);
            return 0;
        } finally {
            closeResources();
        }
    }

    /** Remove one line — same cart-scoped ownership guard as the update. */
    public int removeItem(long cartId, long productId) {
        String sql = "DELETE FROM cart_items WHERE cart_id = ? AND product_id = ?";
        try {
            connection = getConnection();
            if (connection == null) {
                return 0;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, cartId);
            statement.setLong(2, productId);
            return statement.executeUpdate();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "removeItem failed", ex);
            return 0;
        } finally {
            closeResources();
        }
    }

    /**
     * Clear the whole cart by retiring it — DANG_HOAT_DONG -> DA_XOA. The next
     * add lazily creates a fresh cart via getOrCreateActiveCart.
     */
    public int clearCart(long cartId) {
        String sql = "UPDATE carts SET status = 'DA_XOA' "
                + "WHERE cart_id = ? AND status = 'DANG_HOAT_DONG'";
        try {
            connection = getConnection();
            if (connection == null) {
                return 0;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, cartId);
            return statement.executeUpdate();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "clearCart failed", ex);
            return 0;
        } finally {
            closeResources();
        }
    }

    /* ==================== transaction internals ==================== */
    /** Lock the parent profile row — serializes cart creation per customer. */
    private void lockCustomerProfile(Connection conn, long customerId) throws SQLException {
        String sql = "SELECT customer_id FROM customer_profiles WHERE customer_id = ? FOR UPDATE";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(1, customerId);
            rs = ps.executeQuery();
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    /** The active cart on the tx connection — same lookup as findActiveCart. */
    private Cart selectActiveCart(Connection conn, long customerId) throws SQLException {
        String sql = "SELECT cart_id, customer_id, status, created_at, updated_at "
                + "FROM carts WHERE customer_id = ? AND status = 'DANG_HOAT_DONG' "
                + "ORDER BY cart_id DESC LIMIT 1";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(1, customerId);
            rs = ps.executeQuery();
            if (rs.next()) {
                Cart c = new Cart();
                c.setCartId(rs.getLong("cart_id"));
                c.setCustomerId(rs.getLong("customer_id"));
                c.setStatus(rs.getString("status"));
                c.setCreatedAt(rs.getTimestamp("created_at"));
                c.setUpdatedAt(rs.getTimestamp("updated_at"));
                return c;
            }
            return null;
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    /** New DANG_HOAT_DONG cart row — returns the generated cart_id. */
    private long insertCart(Connection conn, long customerId) throws SQLException {
        String sql = "INSERT INTO carts (customer_id, status) VALUES (?,'DANG_HOAT_DONG')";
        PreparedStatement ps = null;
        ResultSet keys = null;
        try {
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, customerId);
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
