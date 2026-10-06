package controller;

import dao.GoodsReceiptDAO;
import dao.GoodsReceiptDAO.ConfirmResult;
import dao.PurchaseOrderDAO;
import model.GoodsReceipt;
import model.GoodsReceiptItem;
import model.PurchaseOrder;
import model.PurchaseOrderItem;
import model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * /inventory/receipts — Stock Receiving (goods receipts against purchase
 * orders). Dispatch via ?action= param (rule.md §22).
 *
 * Actions (GET):  list (default), new, edit, detail
 * Actions (POST): create, update, confirm, cancel
 *
 * Roles: OWNER_ADMIN and STAFF may receive medicine — everything else is
 * bounced to login. The gate runs on every request, not just hidden links.
 *
 * A DRAFT receipt touches nothing outside its own tables. Only "confirm" moves
 * stock — inside one JDBC transaction in GoodsReceiptDAO.
 */
@WebServlet(name = "GoodsReceiptServlet", urlPatterns = {"/inventory/receipts"})
public class GoodsReceiptServlet extends HttpServlet {

    private static final String LIST_URL = "/inventory/receipts";
    private static final String FORM_JSP = "/WEB-INF/views/admin/goods-receipt-form.jsp";
    private static final String LIST_JSP = "/WEB-INF/views/admin/goods-receipt-list.jsp";
    private static final String DETAIL_JSP = "/WEB-INF/views/admin/goods-receipt-detail.jsp";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = requireReceiver(req, resp);
        if (user == null) {
            return;
        }
        String action = req.getParameter("action");
        if (action == null) {
            action = "list";
        }

        switch (action) {
            case "new":
                handleNewForm(req, resp);
                break;
            case "edit":
                handleEditForm(req, resp);
                break;
            case "detail":
                handleDetail(req, resp);
                break;
            default:
                handleList(req, resp);
                break;
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = requireReceiver(req, resp);
        if (user == null) {
            return;
        }
        String action = req.getParameter("action");
        if (action == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        switch (action) {
            case "create":
                handleCreate(req, resp, user);
                break;
            case "update":
                handleUpdate(req, resp);
                break;
            case "confirm":
                handleConfirm(req, resp, user);
                break;
            case "cancel":
                handleCancel(req, resp);
                break;
            default:
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                break;
        }
    }

    /* ==================== GET handlers ==================== */
    private void handleList(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String keyword = trim(req.getParameter("q"));
        String status = trimOrNull(req.getParameter("status"));

        GoodsReceiptDAO dao = new GoodsReceiptDAO();
        req.setAttribute("receipts", dao.findAll(keyword, status));
        req.getRequestDispatcher(LIST_JSP).forward(req, resp);
    }

    private void handleNewForm(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setAttribute("mode", "create");
        req.setAttribute("receivableOrders", new PurchaseOrderDAO().findReceivable());

        // A PO was picked? The form reloads via GET (?po=N) so the item lines
        // are rebuilt server-side with ordered/received/remaining context.
        GoodsReceipt draft = new GoodsReceipt();
        draft.setPurchaseOrderId(parseIdOrNull(req.getParameter("po")));
        draft.setReceiptDate(parseDate(req.getParameter("receiptDate")));
        draft.setInvoiceNumber(trim(req.getParameter("invoiceNumber")));
        draft.setNote(trim(req.getParameter("note")));
        req.setAttribute("receipt", draft);

        if (draft.getPurchaseOrderId() != null && draft.getPurchaseOrderId() > 0) {
            loadPoContext(req, draft.getPurchaseOrderId());
        }
        req.getRequestDispatcher(FORM_JSP).forward(req, resp);
    }

    private void handleEditForm(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long id = parseId(req.getParameter("id"));
        GoodsReceiptDAO dao = new GoodsReceiptDAO();
        GoodsReceipt receipt = null;
        if (id > 0) {
            receipt = dao.findById(id);
        }
        if (receipt == null) {
            resp.sendRedirect(req.getContextPath() + LIST_URL + "?err=notfound");
            return;
        }
        if (!receipt.isEditable()) {
            resp.sendRedirect(req.getContextPath() + LIST_URL + "?action=detail&id=" + id
                    + "&err=noteditable");
            return;
        }

        req.setAttribute("mode", "edit");
        req.setAttribute("receipt", receipt);
        req.setAttribute("items", dao.findItems(id));
        if (receipt.getPurchaseOrderId() != null) {
            loadPoContext(req, receipt.getPurchaseOrderId());
        }
        req.getRequestDispatcher(FORM_JSP).forward(req, resp);
    }

    private void handleDetail(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long id = parseId(req.getParameter("id"));
        GoodsReceiptDAO dao = new GoodsReceiptDAO();
        GoodsReceipt receipt = null;
        if (id > 0) {
            receipt = dao.findById(id);
        }
        if (receipt == null) {
            resp.sendRedirect(req.getContextPath() + LIST_URL + "?err=notfound");
            return;
        }
        req.setAttribute("receipt", receipt);
        req.setAttribute("items", dao.findItems(id));
        if (receipt.getPurchaseOrderId() != null) {
            req.setAttribute("order", new PurchaseOrderDAO().findById(receipt.getPurchaseOrderId()));
        }
        req.getRequestDispatcher(DETAIL_JSP).forward(req, resp);
    }

    /* ==================== POST handlers ==================== */
    /**
     * Save Draft for a NEW receipt. Items are kept raw — the strict inspection
     * rules only apply at confirm time, a draft may still hold PENDING lines.
     */
    private void handleCreate(HttpServletRequest req, HttpServletResponse resp, User user)
            throws ServletException, IOException {
        GoodsReceipt receipt = readReceiptForm(req);
        List<GoodsReceiptItem> items = readItemsForm(req);

        Map<String, String> errors = validateReceipt(receipt, items, false);
        if (!errors.isEmpty()) {
            repopulateForm(req, resp, "create", receipt, items, errors);
            return;
        }

        receipt.setStatus("DRAFT");
        receipt.setReceivedBy(user.getUserId());

        long receiptId = new GoodsReceiptDAO().createDraft(receipt, items);
        if (receiptId <= 0) {
            errors.put("form", "Could not save the receipt. Please try again.");
            repopulateForm(req, resp, "create", receipt, items, errors);
            return;
        }
        resp.sendRedirect(req.getContextPath() + LIST_URL + "?ok=created");
    }

    /**
     * Save changes on a DRAFT receipt. The DRAFT guard lives in the DAO's
     * WHERE clause too — a stale form cannot rewrite a confirmed receipt.
     */
    private void handleUpdate(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long id = parseId(req.getParameter("goodsReceiptId"));
        GoodsReceiptDAO dao = new GoodsReceiptDAO();
        GoodsReceipt existing = null;
        if (id > 0) {
            existing = dao.findById(id);
        }
        if (existing == null) {
            resp.sendRedirect(req.getContextPath() + LIST_URL + "?err=notfound");
            return;
        }
        if (!existing.isEditable()) {
            resp.sendRedirect(req.getContextPath() + LIST_URL + "?action=detail&id=" + id
                    + "&err=noteditable");
            return;
        }

        GoodsReceipt receipt = readReceiptForm(req);
        receipt.setGoodsReceiptId(id);
        // PO + supplier + receiver stay as originally saved.
        receipt.setPurchaseOrderId(existing.getPurchaseOrderId());
        receipt.setSupplierId(existing.getSupplierId());
        receipt.setReceivedBy(existing.getReceivedBy());
        List<GoodsReceiptItem> items = readItemsForm(req);

        Map<String, String> errors = validateReceipt(receipt, items, false);
        if (!errors.isEmpty()) {
            repopulateForm(req, resp, "edit", receipt, items, errors);
            return;
        }

        boolean saved = dao.updateDraft(receipt, items);
        if (!saved) {
            resp.sendRedirect(req.getContextPath() + LIST_URL + "?action=detail&id=" + id
                    + "&err=noteditable");
            return;
        }
        resp.sendRedirect(req.getContextPath() + LIST_URL + "?ok=updated");
    }

    /**
     * Confirm Receipt — the only action that moves stock. The browser posts
     * the full form (items included), so we save the draft first (the DRAFT
     * guard in the DAO makes this a no-op when the receipt was already
     * confirmed), then run the confirm transaction.
     */
    private void handleConfirm(HttpServletRequest req, HttpServletResponse resp, User user)
            throws ServletException, IOException {
        long id = parseId(req.getParameter("goodsReceiptId"));
        GoodsReceiptDAO dao = new GoodsReceiptDAO();
        GoodsReceipt receipt = null;
        if (id > 0) {
            receipt = dao.findById(id);
        }
        if (receipt == null) {
            resp.sendRedirect(req.getContextPath() + LIST_URL + "?err=notfound");
            return;
        }
        if (!receipt.isEditable()) {
            // Already confirmed/cancelled — duplicate submit lands here and
            // must NOT receive the medicine twice.
            resp.sendRedirect(req.getContextPath() + LIST_URL + "?action=detail&id=" + id);
            return;
        }

        // Persist the form's item edits first — the confirm transaction reads
        // items from the DB, not from request parameters.
        GoodsReceipt updated = readReceiptForm(req);
        updated.setGoodsReceiptId(id);
        updated.setPurchaseOrderId(receipt.getPurchaseOrderId());
        updated.setSupplierId(receipt.getSupplierId());
        updated.setReceivedBy(receipt.getReceivedBy());
        List<GoodsReceiptItem> items = readItemsForm(req);

        Map<String, String> errors = validateReceipt(updated, items, false);
        if (!errors.isEmpty()) {
            repopulateForm(req, resp, "edit", updated, items, errors);
            return;
        }

        boolean saved = dao.updateDraft(updated, items);
        if (!saved) {
            resp.sendRedirect(req.getContextPath() + LIST_URL + "?action=detail&id=" + id
                    + "&err=noteditable");
            return;
        }

        ConfirmResult result = dao.confirmReceipt(id, user.getUserId());
        if (result.ok) {
            resp.sendRedirect(req.getContextPath() + LIST_URL
                    + "?action=detail&id=" + id + "&ok=confirmed");
        } else {
            resp.sendRedirect(req.getContextPath() + LIST_URL
                    + "?action=edit&id=" + id + "&err=" + result.error);
        }
    }

    /**
     * DRAFT → CANCELLED. Never touches inventory — a draft has none.
     */
    private void handleCancel(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        long id = parseId(req.getParameter("goodsReceiptId"));
        GoodsReceiptDAO dao = new GoodsReceiptDAO();
        GoodsReceipt receipt = null;
        if (id > 0) {
            receipt = dao.findById(id);
        }
        if (receipt == null) {
            resp.sendRedirect(req.getContextPath() + LIST_URL + "?err=notfound");
            return;
        }
        if (!receipt.isCancellable()) {
            resp.sendRedirect(req.getContextPath() + LIST_URL + "?action=detail&id=" + id
                    + "&err=notcancellable");
            return;
        }
        dao.cancelDraft(id);
        resp.sendRedirect(req.getContextPath() + LIST_URL + "?ok=cancelled");
    }

    /* ==================== form reading ==================== */
    private GoodsReceipt readReceiptForm(HttpServletRequest req) {
        GoodsReceipt r = new GoodsReceipt();
        r.setPurchaseOrderId(parseIdOrNull(req.getParameter("purchaseOrderId")));
        r.setReceiptDate(parseDate(req.getParameter("receiptDate")));
        r.setInvoiceNumber(trim(req.getParameter("invoiceNumber")));
        r.setNote(trim(req.getParameter("note")));
        return r;
    }

    /**
     * Reads received-batch rows: parallel arrays poItemId[] / batchNumber[] /
     * expiryDate[] / quantity[] / costPrice[] / inspectionResult[] /
     * rejectionReason[]. Rows without a PO item picked are skipped; everything
     * else is kept raw — validation lives in validateReceipt.
     */
    private List<GoodsReceiptItem> readItemsForm(HttpServletRequest req) {
        String[] poItemIds = req.getParameterValues("poItemId");
        String[] batchNumbers = req.getParameterValues("batchNumber");
        String[] expiryDates = req.getParameterValues("expiryDate");
        String[] quantities = req.getParameterValues("quantity");
        String[] costPrices = req.getParameterValues("costPrice");
        String[] inspections = req.getParameterValues("inspectionResult");
        String[] reasons = req.getParameterValues("rejectionReason");

        List<GoodsReceiptItem> items = new ArrayList<>();
        if (poItemIds == null) {
            return items;
        }
        for (int i = 0; i < poItemIds.length; i++) {
            long poItemId = parseId(poItemIds[i]);
            if (poItemId <= 0) {
                continue;   // empty row — added a line but picked no product
            }
            GoodsReceiptItem item = new GoodsReceiptItem();
            item.setPurchaseOrderItemId(poItemId);
            item.setBatchNumber(trim(at(batchNumbers, i)));
            item.setExpiryDate(parseDate(at(expiryDates, i)));
            item.setQuantity((int) parseId(at(quantities, i)));
            String costRaw = trim(at(costPrices, i));
            try {
                item.setCostPrice(new BigDecimal(costRaw));
            } catch (NumberFormatException e) {
                item.setCostPrice(new BigDecimal("-1"));  // invalid → caught by validation
            }
            String inspection = trim(at(inspections, i));
            if (inspection.isEmpty()) {
                inspection = "PENDING";
            }
            item.setInspectionResult(inspection);
            item.setRejectionReason(trim(at(reasons, i)));
            items.add(item);
        }
        return items;
    }

    /* ==================== validation ==================== */
    /**
     * Backend-authoritative validation for saving a receipt draft.
     * `strict` is reserved for confirm-time checks — those actually run inside
     * GoodsReceiptDAO.confirmReceipt against freshly read DB state.
     */
    private Map<String, String> validateReceipt(GoodsReceipt receipt,
            List<GoodsReceiptItem> items, boolean strict) {
        Map<String, String> errors = new HashMap<>();
        PurchaseOrderDAO poDao = new PurchaseOrderDAO();

        // --- purchase order: required + receivable ---
        PurchaseOrder po = null;
        if (receipt.getPurchaseOrderId() == null || receipt.getPurchaseOrderId() <= 0) {
            errors.put("purchaseOrderId", "Please choose a purchase order.");
        } else {
            po = poDao.findById(receipt.getPurchaseOrderId());
            if (po == null) {
                errors.put("purchaseOrderId", "Purchase order not found.");
            } else {
                boolean open = "ORDERED".equals(po.getStatus())
                        || "PARTIALLY_RECEIVED".equals(po.getStatus());
                if (!open) {
                    errors.put("purchaseOrderId",
                            "This purchase order is not open for receiving.");
                }
            }
        }
        if (po != null) {
            receipt.setSupplierId(po.getSupplierId());
        }

        // --- receipt date: required, not in the future ---
        if (receipt.getReceiptDate() == null) {
            errors.put("receiptDate", "Receipt date is required.");
        } else {
            Date today = new Date(System.currentTimeMillis());
            if (receipt.getReceiptDate().after(today)) {
                errors.put("receiptDate", "Receipt date cannot be in the future.");
            }
        }

        // --- items: at least one batch line ---
        if (items.isEmpty()) {
            errors.put("items", "Add at least one received batch line.");
        }

        // --- per-item rules ---
        Map<Long, PurchaseOrderItem> poItemById = new HashMap<>();
        if (po != null) {
            List<PurchaseOrderItem> poItems = poDao.findItems(po.getPurchaseOrderId());
            for (PurchaseOrderItem poItem : poItems) {
                poItemById.put(poItem.getPurchaseOrderItemId(), poItem);
            }
        }

        boolean itemNotInPo = false;
        boolean batchMissing = false;
        boolean expiryMissing = false;
        boolean expired = false;
        boolean badQty = false;
        boolean badCost = false;
        boolean badInspection = false;
        boolean reasonMissing = false;
        Map<Long, Integer> acceptedByPoItem = new HashMap<>();

        for (GoodsReceiptItem item : items) {
            PurchaseOrderItem poItem = poItemById.get(item.getPurchaseOrderItemId());
            if (poItem == null) {
                itemNotInPo = true;
                continue;
            }
            item.setProductId(poItem.getProductId());   // trust the PO, not the form

            if (item.getBatchNumber() == null || item.getBatchNumber().isEmpty()) {
                batchMissing = true;
            }
            if (item.getExpiryDate() == null) {
                expiryMissing = true;
            } else if (receipt.getReceiptDate() != null
                    && !item.getExpiryDate().after(receipt.getReceiptDate())
                    && "ACCEPTED".equals(item.getInspectionResult())) {
                expired = true;   // expired medicine must be REJECTED, not accepted
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                badQty = true;
            }
            if (item.getCostPrice() == null || item.getCostPrice().signum() < 0) {
                badCost = true;
            }
            String inspection = item.getInspectionResult();
            if (!"PENDING".equals(inspection) && !"ACCEPTED".equals(inspection)
                    && !"REJECTED".equals(inspection)) {
                badInspection = true;
            }
            if ("REJECTED".equals(inspection)
                    && (item.getRejectionReason() == null
                    || item.getRejectionReason().isEmpty())) {
                reasonMissing = true;
            }
            if ("ACCEPTED".equals(inspection) && item.getQuantity() != null
                    && item.getQuantity() > 0) {
                int planned = 0;
                Integer already = acceptedByPoItem.get(item.getPurchaseOrderItemId());
                if (already != null) {
                    planned = already;
                }
                planned = planned + item.getQuantity();
                acceptedByPoItem.put(item.getPurchaseOrderItemId(), planned);
            }
        }

        // accepted-now must fit inside what the PO still expects
        boolean overdelivered = false;
        for (Map.Entry<Long, Integer> entry : acceptedByPoItem.entrySet()) {
            PurchaseOrderItem poItem = poItemById.get(entry.getKey());
            if (poItem == null) {
                continue;
            }
            int remaining = poItem.getOrderedQuantity() - poItem.getReceivedQuantity();
            if (entry.getValue() > remaining) {
                overdelivered = true;
            }
        }

        if (itemNotInPo) {
            errors.put("items", "Every line must be a product of the selected purchase order.");
        } else if (batchMissing) {
            errors.put("items", "Batch number is required on every line.");
        } else if (expiryMissing) {
            errors.put("items", "Expiry date is required on every line.");
        } else if (expired) {
            errors.put("items",
                    "Medicine already expired on the receipt date cannot be accepted — mark it REJECTED.");
        } else if (badQty) {
            errors.put("items", "Quantity must be greater than 0.");
        } else if (badCost) {
            errors.put("items", "Cost price must be 0 or greater.");
        } else if (badInspection) {
            errors.put("items", "Inspection result must be Pending, Accepted or Rejected.");
        } else if (reasonMissing) {
            errors.put("items", "A rejection reason is required for every rejected line.");
        } else if (overdelivered) {
            errors.put("items",
                    "Accepted quantity exceeds the remaining quantity on the purchase order.");
        }
        return errors;
    }

    /* ==================== helpers ==================== */
    /**
     * Gate: must be logged in as OWNER_ADMIN or STAFF. Returns null after
     * redirect — callers return immediately.
     */
    private User requireReceiver(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession session = req.getSession(false);
        Object u = null;
        if (session != null) {
            u = session.getAttribute("currentUser");
        }
        if (u instanceof User) {
            User user = (User) u;
            String role = user.getRoleName();
            if ("OWNER_ADMIN".equals(role) || "STAFF".equals(role)) {
                return user;
            }
        }
        resp.sendRedirect(req.getContextPath() + "/authen?action=login");
        return null;
    }

    /**
     * Loads the picked PO + its items (with remaining context) for the form.
     */
    private void loadPoContext(HttpServletRequest req, long purchaseOrderId) {
        PurchaseOrderDAO poDao = new PurchaseOrderDAO();
        PurchaseOrder po = poDao.findById(purchaseOrderId);
        req.setAttribute("order", po);
        if (po != null) {
            List<PurchaseOrderItem> poItems = poDao.findItems(purchaseOrderId);
            // Lines still expecting stock come first-class — fully received
            // ones are kept so repopulated rows still render, but flagged.
            req.setAttribute("poItems", poItems);
        }
    }

    /**
     * Forward back to the form with errors + user input preserved.
     */
    private void repopulateForm(HttpServletRequest req, HttpServletResponse resp,
            String mode, GoodsReceipt receipt, List<GoodsReceiptItem> items,
            Map<String, String> errors)
            throws ServletException, IOException {
        req.setAttribute("errors", errors);
        req.setAttribute("mode", mode);
        req.setAttribute("receipt", receipt);
        req.setAttribute("items", items);
        req.setAttribute("receivableOrders", new PurchaseOrderDAO().findReceivable());
        if (receipt.getPurchaseOrderId() != null && receipt.getPurchaseOrderId() > 0) {
            loadPoContext(req, receipt.getPurchaseOrderId());
        }
        req.getRequestDispatcher(FORM_JSP).forward(req, resp);
    }

    private static String at(String[] arr, int i) {
        if (arr == null || i >= arr.length) {
            return "";
        }
        return arr[i];
    }

    private static long parseId(String s) {
        if (s == null) {
            return -1;
        }
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static Long parseIdOrNull(String s) {
        long v = parseId(s);
        if (v > 0) {
            return v;
        }
        return null;
    }

    private static Date parseDate(String s) {
        String t = trim(s);
        if (t.isEmpty()) {
            return null;
        }
        try {
            return Date.valueOf(t);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String trimOrNull(String s) {
        String t = trim(s);
        if (t.isEmpty()) {
            return null;
        }
        return t;
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
