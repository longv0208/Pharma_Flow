package dao;

import db.DBContext;
import model.GoodsReceipt;
import model.GoodsReceiptItem;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Access to `goods_receipts` + `goods_receipt_items` + (inside the confirm
 * transaction) `inventory_batches`, `inventory_movements`,
 * `purchase_order_items` and `purchase_orders`.
 *
 * Everything that moves stock happens inside confirmReceipt(...) — ONE JDBC
 * transaction so a confirmed receipt can never exist without its inventory
 * update, its movement rows and its PO quantity update (rule.md §28).
 *
 * Draft save / update / cancel NEVER touch inventory.
 */
public class GoodsReceiptDAO extends DBContext {

    private static final Logger LOG = Logger.getLogger(GoodsReceiptDAO.class.getName());

    /**
     * Result of a confirmReceipt attempt. A short machine-readable code so the
     * servlet can map it to a readable ?err= without parsing message text.
     */
    public static class ConfirmResult {
        public final boolean ok;
        public final String error;

        private ConfirmResult(boolean ok, String error) {
            this.ok = ok;
            this.error = error;
        }

        public static ConfirmResult success() {
            return new ConfirmResult(true, null);
        }

        public static ConfirmResult fail(String error) {
            return new ConfirmResult(false, error);
        }
    }

    /* ==================== mapping ==================== */
    /**
     * Maps one ResultSet row to a GoodsReceipt. Expects joined columns
     * supplier_name and received_by_name to be present.
     */
    public GoodsReceipt getFromResultSet(ResultSet rs) throws SQLException {
        GoodsReceipt r = new GoodsReceipt();
        r.setGoodsReceiptId(rs.getLong("goods_receipt_id"));
        r.setSupplierId(rs.getLong("supplier_id"));
        Long poId = rs.getLong("purchase_order_id");
        if (rs.wasNull()) {
            poId = null;
        }
        r.setPurchaseOrderId(poId);
        r.setReceivedBy(rs.getLong("received_by"));
        r.setReceiptDate(rs.getDate("receipt_date"));
        r.setInvoiceNumber(rs.getString("invoice_number"));
        r.setNote(rs.getString("note"));
        r.setStatus(rs.getString("status"));
        r.setCreatedAt(rs.getTimestamp("created_at"));
        r.setUpdatedAt(rs.getTimestamp("updated_at"));
        r.setSupplierName(rs.getString("supplier_name"));
        r.setReceivedByName(rs.getString("received_by_name"));
        return r;
    }

    /**
     * Maps one ResultSet row to a GoodsReceiptItem. Expects joined columns
     * product_name and sku.
     */
    public GoodsReceiptItem getItemFromResultSet(ResultSet rs) throws SQLException {
        GoodsReceiptItem item = new GoodsReceiptItem();
        item.setGoodsReceiptItemId(rs.getLong("goods_receipt_item_id"));
        item.setGoodsReceiptId(rs.getLong("goods_receipt_id"));
        Long poItemId = rs.getLong("purchase_order_item_id");
        if (rs.wasNull()) {
            poItemId = null;
        }
        item.setPurchaseOrderItemId(poItemId);
        item.setProductId(rs.getLong("product_id"));
        Long batchId = rs.getLong("batch_id");
        if (rs.wasNull()) {
            batchId = null;
        }
        item.setBatchId(batchId);
        item.setBatchNumber(rs.getString("batch_number"));
        item.setExpiryDate(rs.getDate("expiry_date"));
        item.setQuantity(rs.getInt("quantity"));
        item.setCostPrice(rs.getBigDecimal("cost_price"));
        item.setInspectionResult(rs.getString("inspection_result"));
        item.setRejectionReason(rs.getString("rejection_reason"));
        item.setProductName(rs.getString("product_name"));
        item.setSku(rs.getString("sku"));
        return item;
    }

    /* ==================== list / read ==================== */
    /**
     * Receipt list — newest first. Optional filters: status + keyword matching
     * receipt id (exact) or supplier name (LIKE).
     */
    public List<GoodsReceipt> findAll(String keyword, String status) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT gr.*, s.supplier_name, u.full_name AS received_by_name ");
        sql.append("FROM goods_receipts gr ");
        sql.append("JOIN suppliers s ON s.supplier_id = gr.supplier_id ");
        sql.append("JOIN users u ON u.user_id = gr.received_by ");
        sql.append("WHERE 1=1 ");
        if (status != null && !status.isEmpty()) {
            sql.append("AND gr.status = ? ");
        }
        if (keyword != null && !keyword.isEmpty()) {
            sql.append("AND (s.supplier_name LIKE ? OR gr.goods_receipt_id = ?) ");
        }
        sql.append("ORDER BY gr.goods_receipt_id DESC");

        List<GoodsReceipt> out = new ArrayList<>();
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
     * Single receipt by PK with supplier + receiver names joined.
     */
    public GoodsReceipt findById(long goodsReceiptId) {
        String sql = "SELECT gr.*, s.supplier_name, u.full_name AS received_by_name "
                + "FROM goods_receipts gr "
                + "JOIN suppliers s ON s.supplier_id = gr.supplier_id "
                + "JOIN users u ON u.user_id = gr.received_by "
                + "WHERE gr.goods_receipt_id = ? LIMIT 1";
        try {
            connection = getConnection();
            if (connection == null) {
                return null;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, goodsReceiptId);
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
     * All item rows of one receipt with product name + SKU joined.
     */
    public List<GoodsReceiptItem> findItems(long goodsReceiptId) {
        String sql = "SELECT i.*, p.product_name, p.sku "
                + "FROM goods_receipt_items i "
                + "JOIN products p ON p.product_id = i.product_id "
                + "WHERE i.goods_receipt_id = ? "
                + "ORDER BY i.goods_receipt_item_id ASC";
        List<GoodsReceiptItem> out = new ArrayList<>();
        try {
            connection = getConnection();
            if (connection == null) {
                return out;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, goodsReceiptId);
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

    /* ==================== draft writes ==================== */
    /**
     * INSERT the receipt header (status BAN_NHAP) + all item rows in one
     * transaction. Nothing here touches inventory or the PO — a draft is just
     * unconfirmed receiving work. Returns the generated receipt id, or -1.
     */
    public long createDraft(GoodsReceipt receipt, List<GoodsReceiptItem> items) {
        String insertReceipt = "INSERT INTO goods_receipts "
                + "(supplier_id, purchase_order_id, received_by, receipt_date, "
                + " invoice_number, note, status) "
                + "VALUES (?,?,?,?,?,?,'BAN_NHAP')";
        String insertItem = "INSERT INTO goods_receipt_items "
                + "(goods_receipt_id, purchase_order_item_id, product_id, batch_id, "
                + " batch_number, expiry_date, quantity, cost_price, "
                + " inspection_result, rejection_reason) "
                + "VALUES (?,?,?,NULL,?,?,?,?,?,?)";

        Connection conn = null;
        PreparedStatement psReceipt = null;
        PreparedStatement psItem = null;
        ResultSet keys = null;
        try {
            conn = getConnection();
            if (conn == null) {
                return -1;
            }
            conn.setAutoCommit(false);

            psReceipt = conn.prepareStatement(insertReceipt, Statement.RETURN_GENERATED_KEYS);
            psReceipt.setLong(1, receipt.getSupplierId());
            psReceipt.setLong(2, receipt.getPurchaseOrderId());
            psReceipt.setLong(3, receipt.getReceivedBy());
            psReceipt.setDate(4, receipt.getReceiptDate());
            psReceipt.setString(5, receipt.getInvoiceNumber());
            psReceipt.setString(6, receipt.getNote());
            psReceipt.executeUpdate();

            keys = psReceipt.getGeneratedKeys();
            long receiptId = -1;
            if (keys.next()) {
                receiptId = keys.getLong(1);
            }
            if (receiptId <= 0) {
                conn.rollback();
                return -1;
            }

            psItem = conn.prepareStatement(insertItem);
            for (GoodsReceiptItem item : items) {
                psItem.setLong(1, receiptId);
                psItem.setLong(2, item.getPurchaseOrderItemId());
                psItem.setLong(3, item.getProductId());
                psItem.setString(4, item.getBatchNumber());
                psItem.setDate(5, item.getExpiryDate());
                psItem.setInt(6, item.getQuantity());
                psItem.setBigDecimal(7, item.getCostPrice());
                psItem.setString(8, item.getInspectionResult());
                psItem.setString(9, item.getRejectionReason());
                psItem.executeUpdate();
            }

            conn.commit();
            return receiptId;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "createDraft failed — rolled back", ex);
            rollbackQuietly(conn);
            return -1;
        } finally {
            closeQuietly(keys);
            closeQuietly(psReceipt);
            closeQuietly(psItem);
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
     * Rewrite a BAN_NHAP receipt: header fields + wholesale item replacement,
     * one transaction. Returns false when the receipt is not a BAN_NHAP
     * anymore — the status='BAN_NHAP' guard in the UPDATE makes a stale form
     * a no-op.
     */
    public boolean updateDraft(GoodsReceipt receipt, List<GoodsReceiptItem> items) {
        String updateReceipt = "UPDATE goods_receipts SET receipt_date=?, "
                + "invoice_number=?, note=? "
                + "WHERE goods_receipt_id=? AND status='BAN_NHAP'";
        String deleteItems = "DELETE FROM goods_receipt_items WHERE goods_receipt_id=?";
        String insertItem = "INSERT INTO goods_receipt_items "
                + "(goods_receipt_id, purchase_order_item_id, product_id, batch_id, "
                + " batch_number, expiry_date, quantity, cost_price, "
                + " inspection_result, rejection_reason) "
                + "VALUES (?,?,?,NULL,?,?,?,?,?,?)";

        Connection conn = null;
        PreparedStatement psReceipt = null;
        PreparedStatement psDel = null;
        PreparedStatement psItem = null;
        try {
            conn = getConnection();
            if (conn == null) {
                return false;
            }
            conn.setAutoCommit(false);

            psReceipt = conn.prepareStatement(updateReceipt);
            psReceipt.setDate(1, receipt.getReceiptDate());
            psReceipt.setString(2, receipt.getInvoiceNumber());
            psReceipt.setString(3, receipt.getNote());
            psReceipt.setLong(4, receipt.getGoodsReceiptId());
            int touched = psReceipt.executeUpdate();
            if (touched == 0) {
                conn.rollback();
                return false;
            }

            psDel = conn.prepareStatement(deleteItems);
            psDel.setLong(1, receipt.getGoodsReceiptId());
            psDel.executeUpdate();

            psItem = conn.prepareStatement(insertItem);
            for (GoodsReceiptItem item : items) {
                psItem.setLong(1, receipt.getGoodsReceiptId());
                psItem.setLong(2, item.getPurchaseOrderItemId());
                psItem.setLong(3, item.getProductId());
                psItem.setString(4, item.getBatchNumber());
                psItem.setDate(5, item.getExpiryDate());
                psItem.setInt(6, item.getQuantity());
                psItem.setBigDecimal(7, item.getCostPrice());
                psItem.setString(8, item.getInspectionResult());
                psItem.setString(9, item.getRejectionReason());
                psItem.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "updateDraft failed — rolled back", ex);
            rollbackQuietly(conn);
            return false;
        } finally {
            closeQuietly(psReceipt);
            closeQuietly(psDel);
            closeQuietly(psItem);
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
     * BAN_NHAP → DA_HUY guarded by the current status. Cancelling a draft never
     * touches inventory (a draft has none).
     */
    public boolean cancelDraft(long goodsReceiptId) {
        String sql = "UPDATE goods_receipts SET status='DA_HUY' "
                + "WHERE goods_receipt_id=? AND status='BAN_NHAP'";
        try {
            connection = getConnection();
            if (connection == null) {
                return false;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, goodsReceiptId);
            return statement.executeUpdate() == 1;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "cancelDraft failed", ex);
            return false;
        } finally {
            closeResources();
        }
    }

    /* ==================== confirm (the critical transaction) ==================== */
    /**
     * Confirm a BAN_NHAP receipt in ONE JDBC transaction. Re-reads and
     * re-validates every row from the DB inside the transaction — nothing the
     * browser sent is trusted.
     *
     * Per CHAP_NHAN item: find-or-create the inventory batch by
     * (product_id, batch_number), update goods_receipt_items.batch_id, increase
     * purchase_order_items.received_quantity, insert one NHAP_KHO
     * inventory_movements row. Then recalculate the PO status and set the
     * receipt status (DA_XAC_NHAN or CHAP_NHAN_MOT_PHAN). Any failure rolls the
     * whole thing back.
     */
    public ConfirmResult confirmReceipt(long goodsReceiptId, long performedBy) {
        Connection conn = null;
        try {
            conn = getConnection();
            if (conn == null) {
                return ConfirmResult.fail("db");
            }
            conn.setAutoCommit(false);

            // 1) Re-read the receipt — must still be a BAN_NHAP.
            GoodsReceipt receipt = loadReceiptForUpdate(conn, goodsReceiptId);
            if (receipt == null) {
                conn.rollback();
                return ConfirmResult.fail("notfound");
            }
            if (!"BAN_NHAP".equals(receipt.getStatus())) {
                conn.rollback();
                return ConfirmResult.fail("noteditable");
            }
            if (receipt.getPurchaseOrderId() == null) {
                conn.rollback();
                return ConfirmResult.fail("nopo");
            }

            // 2) Re-read the PO — must be open for receiving.
            PurchaseOrderRow po = loadPurchaseOrder(conn, receipt.getPurchaseOrderId());
            if (po == null) {
                conn.rollback();
                return ConfirmResult.fail("ponotfound");
            }
            boolean poOpen = "DA_DAT_HANG".equals(po.status) || "DA_NHAN_MOT_PHAN".equals(po.status);
            if (!poOpen) {
                conn.rollback();
                return ConfirmResult.fail("poclosed");
            }
            if (po.supplierId != receipt.getSupplierId()) {
                conn.rollback();
                return ConfirmResult.fail("suppliermismatch");
            }

            // 3) Receipt items + the PO items they belong to.
            List<GoodsReceiptItem> items = loadReceiptItems(conn, goodsReceiptId);
            if (items.isEmpty()) {
                conn.rollback();
                return ConfirmResult.fail("noitems");
            }
            Map<Long, PoItemRow> poItems = loadPoItems(conn, receipt.getPurchaseOrderId());

            // Accepted quantity planned per PO item (several batch rows may
            // point at the same PO item).
            Map<Long, Integer> acceptedByPoItem = new HashMap<>();

            boolean sawAccepted = false;
            boolean sawRejected = false;
            for (GoodsReceiptItem item : items) {
                // Every line must reference a real PO item of this PO.
                Long poItemId = item.getPurchaseOrderItemId();
                PoItemRow poItem = null;
                if (poItemId != null) {
                    poItem = poItems.get(poItemId);
                }
                if (poItem == null || !poItem.productId.equals(item.getProductId())) {
                    conn.rollback();
                    return ConfirmResult.fail("itemnotinpo");
                }

                if (item.getBatchNumber() == null || item.getBatchNumber().trim().isEmpty()) {
                    conn.rollback();
                    return ConfirmResult.fail("batchrequired");
                }
                if (item.getExpiryDate() == null) {
                    conn.rollback();
                    return ConfirmResult.fail("expiryrequired");
                }
                if (item.getQuantity() == null || item.getQuantity() <= 0) {
                    conn.rollback();
                    return ConfirmResult.fail("badquantity");
                }
                if (item.getCostPrice() == null || item.getCostPrice().signum() < 0) {
                    conn.rollback();
                    return ConfirmResult.fail("badcost");
                }

                String inspection = item.getInspectionResult();
                if ("CHAP_NHAN".equals(inspection)) {
                    // Medicine that is already expired on the receipt date can
                    // never be accepted — it should have been rejected instead.
                    if (!item.getExpiryDate().after(receipt.getReceiptDate())) {
                        conn.rollback();
                        return ConfirmResult.fail("expired");
                    }
                    sawAccepted = true;
                    int planned = 0;
                    Integer already = acceptedByPoItem.get(poItemId);
                    if (already != null) {
                        planned = already;
                    }
                    planned = planned + item.getQuantity();
                    acceptedByPoItem.put(poItemId, planned);
                } else if ("TU_CHOI".equals(inspection)) {
                    if (item.getRejectionReason() == null
                            || item.getRejectionReason().trim().isEmpty()) {
                        conn.rollback();
                        return ConfirmResult.fail("reasonrequired");
                    }
                    sawRejected = true;
                } else {
                    // CHO_KIEM_TRA or anything else — inspection must be decided.
                    conn.rollback();
                    return ConfirmResult.fail("pendingitems");
                }
            }

            // Simplified flow: at least one accepted line is required to
            // confirm — an all-rejected receipt stays BAN_NHAP or gets cancelled.
            if (!sawAccepted) {
                conn.rollback();
                return ConfirmResult.fail("allrejected");
            }

            // 4) Accepted quantities must fit inside the PO remaining amounts.
            for (Map.Entry<Long, Integer> entry : acceptedByPoItem.entrySet()) {
                PoItemRow poItem = poItems.get(entry.getKey());
                int remaining = poItem.orderedQuantity - poItem.receivedQuantity;
                if (entry.getValue() > remaining) {
                    conn.rollback();
                    return ConfirmResult.fail("overdelivered");
                }
            }

            // 5) Apply every CHAP_NHAN line: batch, movement, PO quantity.
            for (GoodsReceiptItem item : items) {
                if (!"CHAP_NHAN".equals(item.getInspectionResult())) {
                    continue;
                }
                String batchNumber = item.getBatchNumber().trim();

                InventoryBatchRow batch = findBatch(conn, item.getProductId(), batchNumber);
                long batchId;
                int onHandBefore;
                int reservedBefore;
                if (batch == null) {
                    // Case A — first time we see this product+batch: create it.
                    batchId = insertBatch(conn, item, receipt);
                    if (batchId <= 0) {
                        conn.rollback();
                        return ConfirmResult.fail("db");
                    }
                    onHandBefore = 0;
                    reservedBefore = 0;
                } else {
                    // Case B — same product+batch exists: identity must match,
                    // a different expiry means the batch number was reused for
                    // a different lot → refuse rather than merge wrongly.
                    if (!batch.expiryDate.equals(item.getExpiryDate())) {
                        conn.rollback();
                        return ConfirmResult.fail("batchconflict");
                    }
                    batchId = batch.batchId;
                    onHandBefore = batch.onHandQuantity;
                    reservedBefore = batch.reservedQuantity;
                    increaseOnHand(conn, batch, item.getQuantity());
                }

                int onHandAfter = onHandBefore + item.getQuantity();

                linkReceiptItemToBatch(conn, item.getGoodsReceiptItemId(), batchId);
                increasePoReceived(conn, item.getPurchaseOrderItemId(), item.getQuantity());
                insertMovement(conn, batchId, performedBy, item.getQuantity(),
                        onHandBefore, onHandAfter, reservedBefore, goodsReceiptId);
            }

            // 6) PO status from cumulative received quantities.
            Map<Long, PoItemRow> freshPoItems = loadPoItems(conn, receipt.getPurchaseOrderId());
            boolean allReceived = true;
            boolean anyReceived = false;
            for (PoItemRow poItem : freshPoItems.values()) {
                if (poItem.receivedQuantity < poItem.orderedQuantity) {
                    allReceived = false;
                }
                if (poItem.receivedQuantity > 0) {
                    anyReceived = true;
                }
            }
            if (allReceived) {
                updatePoStatus(conn, receipt.getPurchaseOrderId(), "DA_NHAN_DU");
            } else if (anyReceived) {
                updatePoStatus(conn, receipt.getPurchaseOrderId(), "DA_NHAN_MOT_PHAN");
            }

            // 7) Receipt status: all-accepted → DA_XAC_NHAN, mix → CHAP_NHAN_MOT_PHAN.
            String finalStatus;
            if (sawRejected) {
                finalStatus = "CHAP_NHAN_MOT_PHAN";
            } else {
                finalStatus = "DA_XAC_NHAN";
            }
            updateReceiptStatus(conn, goodsReceiptId, finalStatus);

            conn.commit();
            return ConfirmResult.success();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "confirmReceipt failed — rolled back", ex);
            rollbackQuietly(conn);
            return ConfirmResult.fail("db");
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
    /** Small row holder for purchase_orders inside the confirm transaction. */
    private static class PurchaseOrderRow {
        long purchaseOrderId;
        long supplierId;
        String status;
    }

    /** Small row holder for purchase_order_items inside the confirm transaction. */
    private static class PoItemRow {
        long purchaseOrderItemId;
        Long productId;
        int orderedQuantity;
        int receivedQuantity;
    }

    /** Small row holder for inventory_batches inside the confirm transaction. */
    private static class InventoryBatchRow {
        long batchId;
        Date expiryDate;
        String status;
        int onHandQuantity;
        int reservedQuantity;
    }

    /** SELECT ... FOR UPDATE so a second confirm on the same receipt waits/fails. */
    private GoodsReceipt loadReceiptForUpdate(Connection conn, long goodsReceiptId)
            throws SQLException {
        String sql = "SELECT gr.*, s.supplier_name, u.full_name AS received_by_name "
                + "FROM goods_receipts gr "
                + "JOIN suppliers s ON s.supplier_id = gr.supplier_id "
                + "JOIN users u ON u.user_id = gr.received_by "
                + "WHERE gr.goods_receipt_id = ? LIMIT 1 FOR UPDATE";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(1, goodsReceiptId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return getFromResultSet(rs);
            }
            return null;
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    private PurchaseOrderRow loadPurchaseOrder(Connection conn, long purchaseOrderId)
            throws SQLException {
        String sql = "SELECT purchase_order_id, supplier_id, status "
                + "FROM purchase_orders WHERE purchase_order_id = ? LIMIT 1";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(1, purchaseOrderId);
            rs = ps.executeQuery();
            if (rs.next()) {
                PurchaseOrderRow row = new PurchaseOrderRow();
                row.purchaseOrderId = rs.getLong("purchase_order_id");
                row.supplierId = rs.getLong("supplier_id");
                row.status = rs.getString("status");
                return row;
            }
            return null;
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    private List<GoodsReceiptItem> loadReceiptItems(Connection conn, long goodsReceiptId)
            throws SQLException {
        String sql = "SELECT i.*, p.product_name, p.sku "
                + "FROM goods_receipt_items i "
                + "JOIN products p ON p.product_id = i.product_id "
                + "WHERE i.goods_receipt_id = ?";
        List<GoodsReceiptItem> out = new ArrayList<>();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(1, goodsReceiptId);
            rs = ps.executeQuery();
            while (rs.next()) {
                out.add(getItemFromResultSet(rs));
            }
            return out;
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    /** PO items keyed by purchase_order_item_id — quantities re-read live. */
    private Map<Long, PoItemRow> loadPoItems(Connection conn, long purchaseOrderId)
            throws SQLException {
        String sql = "SELECT purchase_order_item_id, product_id, "
                + "ordered_quantity, received_quantity "
                + "FROM purchase_order_items WHERE purchase_order_id = ?";
        Map<Long, PoItemRow> out = new HashMap<>();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(1, purchaseOrderId);
            rs = ps.executeQuery();
            while (rs.next()) {
                PoItemRow row = new PoItemRow();
                row.purchaseOrderItemId = rs.getLong("purchase_order_item_id");
                row.productId = rs.getLong("product_id");
                row.orderedQuantity = rs.getInt("ordered_quantity");
                row.receivedQuantity = rs.getInt("received_quantity");
                out.put(row.purchaseOrderItemId, row);
            }
            return out;
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    /** Business key lookup: (product_id, batch_number) is the DB unique key. */
    private InventoryBatchRow findBatch(Connection conn, long productId, String batchNumber)
            throws SQLException {
        String sql = "SELECT batch_id, expiry_date, status, on_hand_quantity, reserved_quantity "
                + "FROM inventory_batches "
                + "WHERE product_id = ? AND batch_number = ? LIMIT 1";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(1, productId);
            ps.setString(2, batchNumber);
            rs = ps.executeQuery();
            if (rs.next()) {
                InventoryBatchRow row = new InventoryBatchRow();
                row.batchId = rs.getLong("batch_id");
                row.expiryDate = rs.getDate("expiry_date");
                row.status = rs.getString("status");
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

    /** Insert a new batch for an accepted line; status reflects its expiry. */
    private long insertBatch(Connection conn, GoodsReceiptItem item, GoodsReceipt receipt)
            throws SQLException {
        String sql = "INSERT INTO inventory_batches "
                + "(product_id, supplier_id, source_goods_receipt_id, batch_number, "
                + " expiry_date, on_hand_quantity, reserved_quantity, cost_price, "
                + " storage_location, status) "
                + "VALUES (?,?,?,?,?,?,0,?,NULL,?)";
        PreparedStatement ps = null;
        ResultSet keys = null;
        try {
            java.sql.Date today = new java.sql.Date(System.currentTimeMillis());
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, item.getProductId());
            ps.setLong(2, receipt.getSupplierId());
            ps.setLong(3, receipt.getGoodsReceiptId());
            ps.setString(4, item.getBatchNumber().trim());
            ps.setDate(5, item.getExpiryDate());
            ps.setInt(6, item.getQuantity());
            ps.setBigDecimal(7, item.getCostPrice());
            ps.setString(8, InventoryDAO.statusForExpiry(item.getExpiryDate(), today));
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

    /**
     * Top up an existing batch — reserved_quantity is never touched, but the
     * status cache is refreshed: a top-up can bring a HET_HANG (or a
     * stale HET_HAN) batch back to life. BI_KHOA is a manual state and stays
     * put until someone unblocks it.
     */
    private void increaseOnHand(Connection conn, InventoryBatchRow batch, int quantity)
            throws SQLException {
        String sql = "UPDATE inventory_batches SET on_hand_quantity = on_hand_quantity + ?, "
                + "status = ? WHERE batch_id = ?";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setInt(1, quantity);
            String newStatus;
            if ("BI_KHOA".equals(batch.status)) {
                newStatus = "BI_KHOA";
            } else {
                java.sql.Date today = new java.sql.Date(System.currentTimeMillis());
                newStatus = InventoryDAO.statusForExpiry(batch.expiryDate, today);
            }
            ps.setString(2, newStatus);
            ps.setLong(3, batch.batchId);
            ps.executeUpdate();
        } finally {
            closeQuietly(ps);
        }
    }

    /** Traceability: the receipt line now points at the batch it created/filled. */
    private void linkReceiptItemToBatch(Connection conn, long goodsReceiptItemId, long batchId)
            throws SQLException {
        String sql = "UPDATE goods_receipt_items SET batch_id = ? "
                + "WHERE goods_receipt_item_id = ?";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(1, batchId);
            ps.setLong(2, goodsReceiptItemId);
            ps.executeUpdate();
        } finally {
            closeQuietly(ps);
        }
    }

    /** received_quantity += accepted qty (rejected lines never reach this). */
    private void increasePoReceived(Connection conn, long purchaseOrderItemId, int quantity)
            throws SQLException {
        String sql = "UPDATE purchase_order_items "
                + "SET received_quantity = received_quantity + ? "
                + "WHERE purchase_order_item_id = ?";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setInt(1, quantity);
            ps.setLong(2, purchaseOrderItemId);
            ps.executeUpdate();
        } finally {
            closeQuietly(ps);
        }
    }

    /** One NHAP_KHO movement per accepted line — every on-hand change has one. */
    private void insertMovement(Connection conn, long batchId, long performedBy,
            int quantity, int onHandBefore, int onHandAfter, int reserved,
            long goodsReceiptId) throws SQLException {
        String sql = "INSERT INTO inventory_movements "
                + "(batch_id, performed_by, movement_type, on_hand_change, "
                + " reserved_change, on_hand_before, on_hand_after, "
                + " reserved_before, reserved_after, reference_type, reference_id, reason) "
                + "VALUES (?,?,'NHAP_KHO',?,0,?,?,?,?,'PHIEU_NHAP_KHO',?,?)";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setLong(1, batchId);
            ps.setLong(2, performedBy);
            ps.setInt(3, quantity);
            ps.setInt(4, onHandBefore);
            ps.setInt(5, onHandAfter);
            ps.setInt(6, reserved);
            ps.setInt(7, reserved);
            ps.setLong(8, goodsReceiptId);
            ps.setString(9, "Phiếu nhập kho #" + goodsReceiptId);
            ps.executeUpdate();
        } finally {
            closeQuietly(ps);
        }
    }

    private void updatePoStatus(Connection conn, long purchaseOrderId, String status)
            throws SQLException {
        String sql = "UPDATE purchase_orders SET status = ? WHERE purchase_order_id = ?";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setString(1, status);
            ps.setLong(2, purchaseOrderId);
            ps.executeUpdate();
        } finally {
            closeQuietly(ps);
        }
    }

    private void updateReceiptStatus(Connection conn, long goodsReceiptId, String status)
            throws SQLException {
        String sql = "UPDATE goods_receipts SET status = ? WHERE goods_receipt_id = ?";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setString(1, status);
            ps.setLong(2, goodsReceiptId);
            ps.executeUpdate();
        } finally {
            closeQuietly(ps);
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
