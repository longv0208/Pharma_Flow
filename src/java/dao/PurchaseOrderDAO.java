package dao;

import db.DBContext;
import model.PurchaseOrder;
import model.PurchaseOrderItem;
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
 * Access to `purchase_orders` + `purchase_order_items` — admin (OWNER_ADMIN)
 * purchase order management. Extends DBContext per rule.md §20.
 *
 * PO writes (create / updateDraft) run inside one JDBC transaction so an order
 * is never saved without its items or with a wrong total. Inventory is NEVER
 * touched here — stock changes belong to the later Receive Stock flow.
 */
public class PurchaseOrderDAO extends DBContext {

    private static final Logger LOG = Logger.getLogger(PurchaseOrderDAO.class.getName());

    /* ==================== mapping ==================== */
    /**
     * Maps one ResultSet row to a PurchaseOrder entity. Expects joined columns
     * supplier_name and created_by_name to be present.
     */
    public PurchaseOrder getFromResultSet(ResultSet rs) throws SQLException {
        PurchaseOrder po = new PurchaseOrder();
        po.setPurchaseOrderId(rs.getLong("purchase_order_id"));
        po.setSupplierId(rs.getLong("supplier_id"));
        po.setCreatedBy(rs.getLong("created_by"));
        po.setOrderDate(rs.getDate("order_date"));
        po.setExpectedDeliveryDate(rs.getDate("expected_delivery_date"));
        po.setStatus(rs.getString("status"));
        po.setSourceType(rs.getString("source_type"));
        po.setTotalAmount(rs.getBigDecimal("total_amount"));
        po.setNote(rs.getString("note"));
        po.setCreatedAt(rs.getTimestamp("created_at"));
        po.setUpdatedAt(rs.getTimestamp("updated_at"));
        po.setSupplierName(rs.getString("supplier_name"));
        po.setCreatedByName(rs.getString("created_by_name"));
        return po;
    }

    /**
     * Maps one ResultSet row to a PurchaseOrderItem. Expects joined columns
     * product_name and sku.
     */
    public PurchaseOrderItem getItemFromResultSet(ResultSet rs) throws SQLException {
        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setPurchaseOrderItemId(rs.getLong("purchase_order_item_id"));
        item.setPurchaseOrderId(rs.getLong("purchase_order_id"));
        item.setProductId(rs.getLong("product_id"));
        item.setOrderedQuantity(rs.getInt("ordered_quantity"));
        item.setReceivedQuantity(rs.getInt("received_quantity"));
        item.setUnitCost(rs.getBigDecimal("unit_cost"));
        item.setSubtotal(rs.getBigDecimal("subtotal"));
        item.setProductName(rs.getString("product_name"));
        item.setSku(rs.getString("sku"));
        return item;
    }

    /* ==================== list / read ==================== */
    /**
     * Admin PO list — newest first. Optional filters: status + keyword matching
     * PO id (exact) or supplier name (LIKE).
     */
    public List<PurchaseOrder> findAll(String keyword, String status) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT po.*, s.supplier_name, u.full_name AS created_by_name ");
        sql.append("FROM purchase_orders po ");
        sql.append("JOIN suppliers s ON s.supplier_id = po.supplier_id ");
        sql.append("JOIN users u ON u.user_id = po.created_by ");
        sql.append("WHERE 1=1 ");
        if (status != null && !status.isEmpty()) {
            sql.append("AND po.status = ? ");
        }
        if (keyword != null && !keyword.isEmpty()) {
            sql.append("AND (s.supplier_name LIKE ? OR po.purchase_order_id = ?) ");
        }
        sql.append("ORDER BY po.purchase_order_id DESC");

        List<PurchaseOrder> out = new ArrayList<>();
        try {
            connection = getConnection();
            if (connection == null) {
                return out;
            }
            statement = connection.prepareStatement(sql.toString());
            int i = 1;
            if (status != null && !status.isEmpty()) {
                statement.setString(i++, status);
            }
            if (keyword != null && !keyword.isEmpty()) {
                statement.setString(i++, "%" + keyword + "%");
                // keyword used as PO id too — non-numeric input becomes -1 (no match)
                long idKeyword = parseLongOrMinusOne(keyword);
                statement.setLong(i++, idKeyword);
            }
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

    /**
     * Single PO by PK with supplier + creator names joined.
     */
    public PurchaseOrder findById(long purchaseOrderId) {
        String sql = "SELECT po.*, s.supplier_name, u.full_name AS created_by_name "
                + "FROM purchase_orders po "
                + "JOIN suppliers s ON s.supplier_id = po.supplier_id "
                + "JOIN users u ON u.user_id = po.created_by "
                + "WHERE po.purchase_order_id = ? LIMIT 1";
        try {
            connection = getConnection();
            if (connection == null) {
                return null;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, purchaseOrderId);
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
     * POs that can still receive stock — ORDERED or PARTIALLY_RECEIVED only.
     * Used to fill the Receive Stock "Purchase Order" picker.
     */
    public List<PurchaseOrder> findReceivable() {
        String sql = "SELECT po.*, s.supplier_name, u.full_name AS created_by_name "
                + "FROM purchase_orders po "
                + "JOIN suppliers s ON s.supplier_id = po.supplier_id "
                + "JOIN users u ON u.user_id = po.created_by "
                + "WHERE po.status IN ('ORDERED','PARTIALLY_RECEIVED') "
                + "ORDER BY po.purchase_order_id DESC";
        List<PurchaseOrder> out = new ArrayList<>();
        try {
            connection = getConnection();
            if (connection == null) {
                return out;
            }
            statement = connection.prepareStatement(sql);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                out.add(getFromResultSet(resultSet));
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findReceivable failed", ex);
        } finally {
            closeResources();
        }
        return out;
    }

    /**
     * All line items of one PO with product name + SKU joined.
     */
    public List<PurchaseOrderItem> findItems(long purchaseOrderId) {
        String sql = "SELECT i.*, p.product_name, p.sku "
                + "FROM purchase_order_items i "
                + "JOIN products p ON p.product_id = i.product_id "
                + "WHERE i.purchase_order_id = ? "
                + "ORDER BY i.purchase_order_item_id ASC";
        List<PurchaseOrderItem> out = new ArrayList<>();
        try {
            connection = getConnection();
            if (connection == null) {
                return out;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, purchaseOrderId);
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

    /**
     * Products offered by one supplier (via supplier_products) — used to fill
     * the item picker on the PO form. Returns items carrying productId,
     * productName, sku, supplierProductCode and unitCost (= last_cost_price).
     */
    public List<PurchaseOrderItem> findProductsBySupplier(long supplierId) {
        String sql = "SELECT p.product_id, p.product_name, p.sku, "
                + "sp.supplier_product_code, sp.last_cost_price "
                + "FROM supplier_products sp "
                + "JOIN products p ON p.product_id = sp.product_id "
                + "WHERE sp.supplier_id = ? AND p.status = 'ACTIVE' "
                + "ORDER BY p.product_name ASC";
        List<PurchaseOrderItem> out = new ArrayList<>();
        try {
            connection = getConnection();
            if (connection == null) {
                return out;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, supplierId);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                PurchaseOrderItem item = new PurchaseOrderItem();
                item.setProductId(resultSet.getLong("product_id"));
                item.setProductName(resultSet.getString("product_name"));
                item.setSku(resultSet.getString("sku"));
                item.setSupplierProductCode(resultSet.getString("supplier_product_code"));
                item.setUnitCost(resultSet.getBigDecimal("last_cost_price"));
                out.add(item);
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findProductsBySupplier failed", ex);
        } finally {
            closeResources();
        }
        return out;
    }

    /**
     * True when the product is linked to the supplier in supplier_products.
     */
    public boolean isProductOfSupplier(long supplierId, long productId) {
        String sql = "SELECT 1 FROM supplier_products WHERE supplier_id = ? AND product_id = ? LIMIT 1";
        try {
            connection = getConnection();
            if (connection == null) {
                return false;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, supplierId);
            statement.setLong(2, productId);
            resultSet = statement.executeQuery();
            return resultSet.next();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "isProductOfSupplier failed", ex);
            return false;
        } finally {
            closeResources();
        }
    }

    /* ==================== writes ==================== */
    /**
     * INSERT the PO header + all items in one transaction. total_amount is
     * always recalculated here from item qty * unit_cost — values sent by the
     * browser are ignored. Returns the generated PO id, or -1 on failure
     * (transaction rolled back).
     */
    public long create(PurchaseOrder po, List<PurchaseOrderItem> items) {
        String insertPo = "INSERT INTO purchase_orders "
                + "(supplier_id, created_by, order_date, expected_delivery_date, "
                + " status, source_type, total_amount, note) "
                + "VALUES (?,?,?,?,?,?,?,?)";
        String insertItem = "INSERT INTO purchase_order_items "
                + "(purchase_order_id, product_id, ordered_quantity, received_quantity, unit_cost, subtotal) "
                + "VALUES (?,?,?,0,?,?)";
        String updateTotal = "UPDATE purchase_orders SET total_amount = ? WHERE purchase_order_id = ?";

        Connection conn = null;
        PreparedStatement psPo = null;
        PreparedStatement psItem = null;
        PreparedStatement psTotal = null;
        ResultSet keys = null;
        try {
            conn = getConnection();
            if (conn == null) {
                return -1;
            }
            conn.setAutoCommit(false);

            // 1) header — total written as 0 first, fixed after items are in
            psPo = conn.prepareStatement(insertPo, Statement.RETURN_GENERATED_KEYS);
            psPo.setLong(1, po.getSupplierId());
            psPo.setLong(2, po.getCreatedBy());
            psPo.setDate(3, po.getOrderDate());
            psPo.setDate(4, po.getExpectedDeliveryDate());
            psPo.setString(5, po.getStatus());
            psPo.setString(6, po.getSourceType());
            psPo.setBigDecimal(7, java.math.BigDecimal.ZERO);
            psPo.setString(8, po.getNote());
            psPo.executeUpdate();

            keys = psPo.getGeneratedKeys();
            long poId = -1;
            if (keys.next()) {
                poId = keys.getLong(1);
            }
            if (poId <= 0) {
                conn.rollback();
                return -1;
            }

            // 2) items — subtotal recalculated server-side per line
            java.math.BigDecimal total = java.math.BigDecimal.ZERO;
            psItem = conn.prepareStatement(insertItem);
            for (PurchaseOrderItem item : items) {
                java.math.BigDecimal qty = new java.math.BigDecimal(item.getOrderedQuantity());
                java.math.BigDecimal subtotal = qty.multiply(item.getUnitCost());
                psItem.setLong(1, poId);
                psItem.setLong(2, item.getProductId());
                psItem.setInt(3, item.getOrderedQuantity());
                psItem.setBigDecimal(4, item.getUnitCost());
                psItem.setBigDecimal(5, subtotal);
                psItem.executeUpdate();
                total = total.add(subtotal);
            }

            // 3) final total on the header
            psTotal = conn.prepareStatement(updateTotal);
            psTotal.setBigDecimal(1, total);
            psTotal.setLong(2, poId);
            psTotal.executeUpdate();

            conn.commit();
            return poId;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "create failed — rolled back", ex);
            rollbackQuietly(conn);
            return -1;
        } finally {
            closeQuietly(keys);
            closeQuietly(psPo);
            closeQuietly(psItem);
            closeQuietly(psTotal);
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
     * Update a DRAFT PO: rewrite header fields, delete old items, insert the
     * new list, recalculate total — all in one transaction. Returns false when
     * the PO is not a DRAFT anymore (nothing is written then).
     */
    public boolean updateDraft(PurchaseOrder po, List<PurchaseOrderItem> items) {
        String updatePo = "UPDATE purchase_orders SET supplier_id=?, "
                + "expected_delivery_date=?, note=? "
                + "WHERE purchase_order_id=? AND status='DRAFT'";
        String deleteItems = "DELETE FROM purchase_order_items WHERE purchase_order_id=?";
        String insertItem = "INSERT INTO purchase_order_items "
                + "(purchase_order_id, product_id, ordered_quantity, received_quantity, unit_cost, subtotal) "
                + "VALUES (?,?,?,0,?,?)";
        String updateTotal = "UPDATE purchase_orders SET total_amount=? WHERE purchase_order_id=?";

        Connection conn = null;
        PreparedStatement psPo = null;
        PreparedStatement psDel = null;
        PreparedStatement psItem = null;
        PreparedStatement psTotal = null;
        try {
            conn = getConnection();
            if (conn == null) {
                return false;
            }
            conn.setAutoCommit(false);

            // 1) header — the status='DRAFT' guard makes the update a no-op
            //    when someone else already placed/cancelled the order
            psPo = conn.prepareStatement(updatePo);
            psPo.setLong(1, po.getSupplierId());
            psPo.setDate(2, po.getExpectedDeliveryDate());
            psPo.setString(3, po.getNote());
            psPo.setLong(4, po.getPurchaseOrderId());
            int touched = psPo.executeUpdate();
            if (touched == 0) {
                conn.rollback();
                return false;
            }

            // 2) replace items wholesale (draft = fully editable)
            psDel = conn.prepareStatement(deleteItems);
            psDel.setLong(1, po.getPurchaseOrderId());
            psDel.executeUpdate();

            java.math.BigDecimal total = java.math.BigDecimal.ZERO;
            psItem = conn.prepareStatement(insertItem);
            for (PurchaseOrderItem item : items) {
                java.math.BigDecimal qty = new java.math.BigDecimal(item.getOrderedQuantity());
                java.math.BigDecimal subtotal = qty.multiply(item.getUnitCost());
                psItem.setLong(1, po.getPurchaseOrderId());
                psItem.setLong(2, item.getProductId());
                psItem.setInt(3, item.getOrderedQuantity());
                psItem.setBigDecimal(4, item.getUnitCost());
                psItem.setBigDecimal(5, subtotal);
                psItem.executeUpdate();
                total = total.add(subtotal);
            }

            psTotal = conn.prepareStatement(updateTotal);
            psTotal.setBigDecimal(1, total);
            psTotal.setLong(2, po.getPurchaseOrderId());
            psTotal.executeUpdate();

            conn.commit();
            return true;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "updateDraft failed — rolled back", ex);
            rollbackQuietly(conn);
            return false;
        } finally {
            closeQuietly(psPo);
            closeQuietly(psDel);
            closeQuietly(psItem);
            closeQuietly(psTotal);
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
     * Status transition guarded by the CURRENT status in the DB — the
     * "expected" current status is part of the WHERE clause so a stale request
     * (double submit, already moved on) can not flip the row.
     *
     * DRAFT → ORDERED (place) and DRAFT/ORDERED → CANCELLED are the only
     * transitions this module performs.
     */
    public boolean changeStatus(long purchaseOrderId, String fromStatus, String toStatus) {
        String sql = "UPDATE purchase_orders SET status=? "
                + "WHERE purchase_order_id=? AND status=?";
        try {
            connection = getConnection();
            if (connection == null) {
                return false;
            }
            statement = connection.prepareStatement(sql);
            statement.setString(1, toStatus);
            statement.setLong(2, purchaseOrderId);
            statement.setString(3, fromStatus);
            return statement.executeUpdate() == 1;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "changeStatus failed", ex);
            return false;
        } finally {
            closeResources();
        }
    }

    /**
     * True when every item of the PO still has received_quantity = 0 — required
     * before an ORDERED po can be cancelled.
     */
    public boolean allItemsUnreceived(long purchaseOrderId) {
        String sql = "SELECT COUNT(*) FROM purchase_order_items "
                + "WHERE purchase_order_id=? AND received_quantity > 0";
        try {
            connection = getConnection();
            if (connection == null) {
                return false;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, purchaseOrderId);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1) == 0;
            }
            return false;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "allItemsUnreceived failed", ex);
            return false;
        } finally {
            closeResources();
        }
    }

    /* ==================== small helpers ==================== */
    private static long parseLongOrMinusOne(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return -1;
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
