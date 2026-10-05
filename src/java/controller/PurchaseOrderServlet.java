package controller;

import dao.PurchaseOrderDAO;
import dao.SupplierDAO;
import model.PurchaseOrder;
import model.PurchaseOrderItem;
import model.Supplier;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * /admin/purchase-orders — OWNER_ADMIN purchase order management. Dispatch via
 * ?action= param (rule.md §22).
 *
 * Actions (GET):  list (default), new, edit, detail
 * Actions (POST): create, update, place, cancel
 *
 * Status flow allowed in this module:
 *   DRAFT -> ORDERED   (place order)
 *   DRAFT -> CANCELLED
 *   ORDERED -> CANCELLED (only when every item has received_quantity = 0)
 * PARTIALLY_RECEIVED / RECEIVED are read-only here — Receive Stock handles them.
 *
 * A PO never changes inventory — stock moves only when Receive Stock confirms.
 */
@WebServlet(name = "PurchaseOrderServlet", urlPatterns = {"/admin/purchase-orders"})
public class PurchaseOrderServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = requireAdmin(req, resp);
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
        User user = requireAdmin(req, resp);
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
            case "place":
                handlePlace(req, resp);
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

        PurchaseOrderDAO dao = new PurchaseOrderDAO();
        req.setAttribute("orders", dao.findAll(keyword, status));
        req.getRequestDispatcher("/WEB-INF/views/admin/purchase-order-list.jsp").forward(req, resp);
    }

    private void handleNewForm(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setAttribute("mode", "create");
        req.setAttribute("suppliers", findActiveSuppliers());
        // Supplier picked (the form reloads via GET when it changes)? rebuild
        // the product picker for that supplier and keep the other field values.
        PurchaseOrder draft = new PurchaseOrder();
        draft.setSupplierId(parseId(req.getParameter("supplier")));
        draft.setOrderDate(parseDate(req.getParameter("orderDate")));
        draft.setExpectedDeliveryDate(parseDate(req.getParameter("expectedDeliveryDate")));
        draft.setNote(trim(req.getParameter("note")));
        req.setAttribute("order", draft);
        if (draft.getSupplierId() != null && draft.getSupplierId() > 0) {
            req.setAttribute("supplierProducts",
                    new PurchaseOrderDAO().findProductsBySupplier(draft.getSupplierId()));
        }
        req.getRequestDispatcher("/WEB-INF/views/admin/purchase-order-form.jsp").forward(req, resp);
    }

    private void handleEditForm(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long id = parseId(req.getParameter("id"));
        PurchaseOrderDAO dao = new PurchaseOrderDAO();
        PurchaseOrder po = null;
        if (id > 0) {
            po = dao.findById(id);
        }
        if (po == null) {
            resp.sendRedirect(req.getContextPath() + "/admin/purchase-orders?err=notfound");
            return;
        }
        if (!po.isEditable()) {
            resp.sendRedirect(req.getContextPath() + "/admin/purchase-orders?action=detail&id=" + id
                    + "&err=noteditable");
            return;
        }

        req.setAttribute("mode", "edit");
        req.setAttribute("suppliers", findActiveSuppliers());

        // Supplier dropdown changed → reload picker products for the NEW
        // supplier and drop saved items (they belong to the old supplier).
        long pickedSupplier = parseId(req.getParameter("supplier"));
        if (pickedSupplier > 0 && pickedSupplier != po.getSupplierId()) {
            po.setSupplierId(pickedSupplier);
            po.setOrderDate(parseDate(req.getParameter("orderDate")));
            po.setExpectedDeliveryDate(parseDate(req.getParameter("expectedDeliveryDate")));
            po.setNote(trim(req.getParameter("note")));
            req.setAttribute("order", po);
            req.setAttribute("supplierProducts", dao.findProductsBySupplier(pickedSupplier));
        } else {
            req.setAttribute("order", po);
            req.setAttribute("items", dao.findItems(id));
            req.setAttribute("supplierProducts", dao.findProductsBySupplier(po.getSupplierId()));
        }
        req.getRequestDispatcher("/WEB-INF/views/admin/purchase-order-form.jsp").forward(req, resp);
    }

    private void handleDetail(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long id = parseId(req.getParameter("id"));
        PurchaseOrderDAO dao = new PurchaseOrderDAO();
        PurchaseOrder po = null;
        if (id > 0) {
            po = dao.findById(id);
        }
        if (po == null) {
            resp.sendRedirect(req.getContextPath() + "/admin/purchase-orders?err=notfound");
            return;
        }
        req.setAttribute("order", po);
        req.setAttribute("items", dao.findItems(id));
        req.getRequestDispatcher("/WEB-INF/views/admin/purchase-order-detail.jsp").forward(req, resp);
    }

    /* ==================== POST handlers ==================== */
    /**
     * Save Draft or Place Order for a NEW purchase order. Which one happens is
     * decided by the submit button (name="submitAction").
     */
    private void handleCreate(HttpServletRequest req, HttpServletResponse resp, User user)
            throws ServletException, IOException {
        PurchaseOrder po = readOrderForm(req);
        List<PurchaseOrderItem> items = readItemsForm(req);
        String submitAction = trim(req.getParameter("submitAction"));

        Map<String, String> errors = validateOrder(po, items);
        if (!errors.isEmpty()) {
            repopulateForm(req, resp, "create", po, items, errors);
            return;
        }

        boolean placeOrder = "place".equals(submitAction);
        if (placeOrder) {
            po.setStatus("ORDERED");
        } else {
            po.setStatus("DRAFT");
        }
        po.setSourceType("MANUAL");
        po.setCreatedBy(user.getUserId());

        long poId = new PurchaseOrderDAO().create(po, items);
        if (poId <= 0) {
            errors.put("form", "Could not save the purchase order. Please try again.");
            repopulateForm(req, resp, "create", po, items, errors);
            return;
        }
        if (placeOrder) {
            resp.sendRedirect(req.getContextPath() + "/admin/purchase-orders?ok=placed");
        } else {
            resp.sendRedirect(req.getContextPath() + "/admin/purchase-orders?ok=created");
        }
    }

    /**
     * Save changes to a DRAFT PO. The draft guard lives in the DAO update's
     * WHERE clause too, so a stale form cannot overwrite an ordered PO.
     */
    private void handleUpdate(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long id = parseId(req.getParameter("purchaseOrderId"));
        PurchaseOrderDAO dao = new PurchaseOrderDAO();
        PurchaseOrder existing = null;
        if (id > 0) {
            existing = dao.findById(id);
        }
        if (existing == null) {
            resp.sendRedirect(req.getContextPath() + "/admin/purchase-orders?err=notfound");
            return;
        }
        if (!existing.isEditable()) {
            resp.sendRedirect(req.getContextPath() + "/admin/purchase-orders?action=detail&id=" + id
                    + "&err=noteditable");
            return;
        }

        PurchaseOrder po = readOrderForm(req);
        po.setPurchaseOrderId(id);
        // Order date + creator stay as originally saved — drafts keep history.
        po.setOrderDate(existing.getOrderDate());
        po.setCreatedBy(existing.getCreatedBy());
        List<PurchaseOrderItem> items = readItemsForm(req);

        Map<String, String> errors = validateOrder(po, items);
        if (!errors.isEmpty()) {
            repopulateForm(req, resp, "edit", po, items, errors);
            return;
        }

        boolean saved = dao.updateDraft(po, items);
        if (!saved) {
            resp.sendRedirect(req.getContextPath() + "/admin/purchase-orders?action=detail&id=" + id
                    + "&err=noteditable");
            return;
        }
        resp.sendRedirect(req.getContextPath() + "/admin/purchase-orders?ok=updated");
    }

    /**
     * Place Order on an existing DRAFT: DRAFT -> ORDERED. The status read comes
     * from the DB, never from request input.
     */
    private void handlePlace(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        long id = parseId(req.getParameter("purchaseOrderId"));
        PurchaseOrderDAO dao = new PurchaseOrderDAO();
        PurchaseOrder po = null;
        if (id > 0) {
            po = dao.findById(id);
        }
        if (po == null) {
            resp.sendRedirect(req.getContextPath() + "/admin/purchase-orders?err=notfound");
            return;
        }
        if (!"DRAFT".equals(po.getStatus())) {
            resp.sendRedirect(req.getContextPath() + "/admin/purchase-orders?action=detail&id=" + id
                    + "&err=noteditable");
            return;
        }

        List<PurchaseOrderItem> items = dao.findItems(id);
        boolean valid = revalidateForPlacing(po, items);
        if (!valid) {
            resp.sendRedirect(req.getContextPath() + "/admin/purchase-orders?action=edit&id=" + id
                    + "&err=invalid");
            return;
        }

        boolean placed = dao.changeStatus(id, "DRAFT", "ORDERED");
        if (placed) {
            resp.sendRedirect(req.getContextPath() + "/admin/purchase-orders?ok=placed");
        } else {
            resp.sendRedirect(req.getContextPath() + "/admin/purchase-orders?action=detail&id=" + id
                    + "&err=noteditable");
        }
    }

    /**
     * Cancel a PO. DRAFT cancels freely; ORDERED cancels only when every item
     * still has received_quantity = 0 (checked live in the DB).
     */
    private void handleCancel(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        long id = parseId(req.getParameter("purchaseOrderId"));
        PurchaseOrderDAO dao = new PurchaseOrderDAO();
        PurchaseOrder po = null;
        if (id > 0) {
            po = dao.findById(id);
        }
        if (po == null) {
            resp.sendRedirect(req.getContextPath() + "/admin/purchase-orders?err=notfound");
            return;
        }

        String current = po.getStatus();
        if ("DRAFT".equals(current)) {
            dao.changeStatus(id, "DRAFT", "CANCELLED");
            resp.sendRedirect(req.getContextPath() + "/admin/purchase-orders?ok=cancelled");
            return;
        }
        if ("ORDERED".equals(current)) {
            if (!dao.allItemsUnreceived(id)) {
                resp.sendRedirect(req.getContextPath() + "/admin/purchase-orders?action=detail&id=" + id
                        + "&err=notcancellable");
                return;
            }
            dao.changeStatus(id, "ORDERED", "CANCELLED");
            resp.sendRedirect(req.getContextPath() + "/admin/purchase-orders?ok=cancelled");
            return;
        }
        // PARTIALLY_RECEIVED / RECEIVED / CANCELLED — nothing to do
        resp.sendRedirect(req.getContextPath() + "/admin/purchase-orders?action=detail&id=" + id
                + "&err=notcancellable");
    }

    /* ==================== form reading ==================== */
    /**
     * Reads the PO header fields from the request into a PurchaseOrder entity.
     * Item lines are read separately by readItemsForm.
     */
    private PurchaseOrder readOrderForm(HttpServletRequest req) {
        PurchaseOrder po = new PurchaseOrder();
        po.setSupplierId(parseId(req.getParameter("supplierId")));
        po.setOrderDate(parseDate(req.getParameter("orderDate")));
        po.setExpectedDeliveryDate(parseDate(req.getParameter("expectedDeliveryDate")));
        po.setNote(trim(req.getParameter("note")));
        return po;
    }

    /**
     * Reads item rows: parallel arrays productId[] / quantity[] / unitCost[].
     * Rows with no product selected are skipped; everything else is kept raw —
     * validation happens in validateOrder.
     */
    private List<PurchaseOrderItem> readItemsForm(HttpServletRequest req) {
        String[] productIds = req.getParameterValues("productId");
        String[] quantities = req.getParameterValues("quantity");
        String[] unitCosts = req.getParameterValues("unitCost");

        List<PurchaseOrderItem> items = new ArrayList<>();
        if (productIds == null) {
            return items;
        }
        for (int i = 0; i < productIds.length; i++) {
            long productId = parseId(productIds[i]);
            if (productId <= 0) {
                continue;   // empty row — user added a line but picked nothing
            }
            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setProductId(productId);
            item.setOrderedQuantity((int) parseId(at(quantities, i)));
            String costRaw = trim(at(unitCosts, i));
            try {
                item.setUnitCost(new BigDecimal(costRaw));
            } catch (NumberFormatException e) {
                item.setUnitCost(new BigDecimal("-1"));   // invalid → caught by validation
            }
            items.add(item);
        }
        return items;
    }

    /* ==================== validation ==================== */
    /**
     * Backend-authoritative validation for save-draft AND place-order.
     * Totals are NOT checked here — they are recalculated inside the DAO.
     */
    private Map<String, String> validateOrder(PurchaseOrder po, List<PurchaseOrderItem> items) {
        Map<String, String> errors = new HashMap<>();
        PurchaseOrderDAO dao = new PurchaseOrderDAO();

        // --- supplier: required + exists + ACTIVE ---
        Supplier supplier = null;
        if (po.getSupplierId() == null || po.getSupplierId() <= 0) {
            errors.put("supplierId", "Supplier is required.");
        } else {
            supplier = new SupplierDAO().findById(po.getSupplierId());
            if (supplier == null) {
                errors.put("supplierId", "Supplier not found.");
            } else if (!"ACTIVE".equals(supplier.getStatus())) {
                errors.put("supplierId", "Supplier is not active.");
            }
        }

        // --- order date: required ---
        if (po.getOrderDate() == null) {
            errors.put("orderDate", "Order date is required.");
        }

        // --- expected delivery: optional, must not be before order date ---
        if (po.getExpectedDeliveryDate() != null && po.getOrderDate() != null
                && po.getExpectedDeliveryDate().before(po.getOrderDate())) {
            errors.put("expectedDeliveryDate",
                    "Expected delivery date cannot be earlier than order date.");
        }

        // --- items: at least one ---
        if (items.isEmpty()) {
            errors.put("items", "At least one medicine must be added.");
        }

        // --- per-item rules + duplicates + supplier membership ---
        Set<Long> seen = new HashSet<>();
        boolean dup = false;
        boolean badQty = false;
        boolean badCost = false;
        boolean notOfSupplier = false;
        for (PurchaseOrderItem item : items) {
            if (!seen.add(item.getProductId())) {
                dup = true;
            }
            if (item.getOrderedQuantity() == null || item.getOrderedQuantity() <= 0) {
                badQty = true;
            }
            if (item.getUnitCost() == null || item.getUnitCost().signum() < 0) {
                badCost = true;
            }
            if (po.getSupplierId() != null && po.getSupplierId() > 0
                    && !dao.isProductOfSupplier(po.getSupplierId(), item.getProductId())) {
                notOfSupplier = true;
            }
        }
        if (dup) {
            errors.put("items", "The same product cannot appear twice in one purchase order.");
        } else if (badQty) {
            errors.put("items", "Quantity must be greater than 0.");
        } else if (badCost) {
            errors.put("items", "Unit cost must be 0 or greater.");
        } else if (notOfSupplier) {
            errors.put("items", "This product is not supplied by the selected supplier.");
        }
        return errors;
    }

    /**
     * Re-check a stored DRAFT before placing it — the DB rows could have been
     * saved when rules were different, so we validate persisted state.
     */
    private boolean revalidateForPlacing(PurchaseOrder po, List<PurchaseOrderItem> items) {
        Supplier s = new SupplierDAO().findById(po.getSupplierId());
        if (s == null || !"ACTIVE".equals(s.getStatus())) {
            return false;
        }
        if (items.isEmpty()) {
            return false;
        }
        PurchaseOrderDAO dao = new PurchaseOrderDAO();
        for (PurchaseOrderItem item : items) {
            if (item.getOrderedQuantity() == null || item.getOrderedQuantity() <= 0) {
                return false;
            }
            if (item.getUnitCost() == null || item.getUnitCost().signum() < 0) {
                return false;
            }
            if (!dao.isProductOfSupplier(po.getSupplierId(), item.getProductId())) {
                return false;
            }
        }
        if (po.getExpectedDeliveryDate() != null && po.getOrderDate() != null
                && po.getExpectedDeliveryDate().before(po.getOrderDate())) {
            return false;
        }
        return true;
    }

    /* ==================== helpers ==================== */
    /**
     * Gate: must be logged in as OWNER_ADMIN. Returns null after redirect.
     */
    private User requireAdmin(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession session = req.getSession(false);
        Object u = null;
        if (session != null) {
            u = session.getAttribute("currentUser");
        }
        if (u instanceof User && "OWNER_ADMIN".equals(((User) u).getRoleName())) {
            return (User) u;
        }
        resp.sendRedirect(req.getContextPath() + "/authen?action=login");
        return null;
    }

    /**
     * Only ACTIVE suppliers can be picked on the PO form.
     */
    private List<Supplier> findActiveSuppliers() {
        List<Supplier> all = new SupplierDAO().findAll();
        List<Supplier> active = new ArrayList<>();
        for (Supplier s : all) {
            if ("ACTIVE".equals(s.getStatus())) {
                active.add(s);
            }
        }
        return active;
    }

    /**
     * Forward back to the form with errors + user input preserved.
     */
    private void repopulateForm(HttpServletRequest req, HttpServletResponse resp,
            String mode, PurchaseOrder po, List<PurchaseOrderItem> items,
            Map<String, String> errors)
            throws ServletException, IOException {
        req.setAttribute("errors", errors);
        req.setAttribute("mode", mode);
        req.setAttribute("order", po);
        req.setAttribute("items", items);
        req.setAttribute("suppliers", findActiveSuppliers());
        if (po.getSupplierId() != null && po.getSupplierId() > 0) {
            req.setAttribute("supplierProducts",
                    new PurchaseOrderDAO().findProductsBySupplier(po.getSupplierId()));
        }
        req.getRequestDispatcher("/WEB-INF/views/admin/purchase-order-form.jsp").forward(req, resp);
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
