package controller;

import dao.InventoryAdjustmentDAO;
import dao.InventoryAdjustmentDAO.AdjustmentResult;
import dao.InventoryDAO;
import model.InventoryAdjustment;
import model.InventoryBatch;
import model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Date;
import java.util.List;

/**
 * /inventory/adjustments — manual on_hand corrections per batch.
 * Dispatch via ?action= param (rule.md §22).
 *
 * GET:  (default) adjustment history list, ?action=new&batchId=N form.
 * POST: ?action=create — one JDBC transaction, then redirect (PRG).
 *
 * Roles: OWNER_ADMIN and STAFF — everything else bounced to login.
 * Only batchId / quantityChange / reason / note are read from the request;
 * before/after/status are always recomputed from the locked DB row.
 */
@WebServlet(name = "InventoryAdjustmentServlet", urlPatterns = {"/inventory/adjustments"})
public class InventoryAdjustmentServlet extends HttpServlet {

    private static final String LIST_JSP = "/WEB-INF/views/admin/inventory-adjustments.jsp";
    private static final String FORM_JSP = "/WEB-INF/views/admin/inventory-adjustment-form.jsp";
    private static final int PAGE_SIZE = 20;

    /** The reason allowlist handed to the form — matches the DB enum. */
    private static final String[] REASONS = {
        "DAMAGED", "LOST", "EXPIRED", "COUNT_CORRECTION", "DATA_CORRECTION", "OTHER"};

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = requireAccess(req, resp);
        if (user == null) {
            return;
        }
        String action = req.getParameter("action");
        if (action == null) {
            action = "list";
        }

        switch (action) {
            case "new":
                handleNew(req, resp);
                break;
            default:
                handleList(req, resp);
                break;
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = requireAccess(req, resp);
        if (user == null) {
            return;
        }
        String action = req.getParameter("action");
        if ("create".equals(action)) {
            handleCreate(req, resp, user);
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    /* ==================== GET handlers ==================== */
    /**
     * Adjustment History — newest first, optional filters + pagination.
     */
    private void handleList(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String productKeyword = trim(req.getParameter("q"));
        String batchNumber = trim(req.getParameter("batch"));
        String reason = trim(req.getParameter("reason"));
        Date fromDate = parseDate(req.getParameter("from"));
        Date toDate = parseDate(req.getParameter("to"));

        InventoryAdjustmentDAO dao = new InventoryAdjustmentDAO();
        int total = dao.countAll(productKeyword, batchNumber, reason, fromDate, toDate);
        int totalPages = (int) Math.ceil((double) total / PAGE_SIZE);
        if (totalPages < 1) {
            totalPages = 1;
        }
        int page = parsePage(req.getParameter("page"));
        if (page > totalPages) {
            page = totalPages;
        }
        int offset = (page - 1) * PAGE_SIZE;

        List<InventoryAdjustment> adjustments = dao.findAll(
                productKeyword, batchNumber, reason, fromDate, toDate, offset, PAGE_SIZE);

        req.setAttribute("adjustments", adjustments);
        req.setAttribute("reasons", REASONS);
        req.setAttribute("page", page);
        req.setAttribute("totalPages", totalPages);
        req.setAttribute("total", total);
        req.getRequestDispatcher(LIST_JSP).forward(req, resp);
    }

    /**
     * New adjustment form — loads the batch for context; quantities shown are
     * display-only and re-read server-side on submit.
     */
    private void handleNew(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long batchId = parseId(req.getParameter("batchId"));
        if (batchId <= 0) {
            resp.sendRedirect(req.getContextPath() + "/inventory/adjustments?err=notfound");
            return;
        }

        InventoryDAO inventoryDao = new InventoryDAO();
        InventoryBatch batch = inventoryDao.findBatchById(batchId);
        if (batch == null) {
            resp.sendRedirect(req.getContextPath() + "/inventory/adjustments?err=notfound");
            return;
        }

        req.setAttribute("batch", batch);
        req.setAttribute("reasons", REASONS);
        req.getRequestDispatcher(FORM_JSP).forward(req, resp);
    }

    /* ==================== POST handlers ==================== */
    /**
     * Create the adjustment in one transaction; redirect back to the list (or
     * the form with ?err=) — never forward after POST.
     */
    private void handleCreate(HttpServletRequest req, HttpServletResponse resp, User user)
            throws IOException {
        long batchId = parseId(req.getParameter("batchId"));
        String qtyRaw = trim(req.getParameter("quantityChange"));
        String reason = trim(req.getParameter("reason"));
        String note = trim(req.getParameter("note"));

        if (batchId <= 0) {
            resp.sendRedirect(req.getContextPath() + "/inventory/adjustments?err=notfound");
            return;
        }
        // quantityChange must parse as a non-zero integer.
        int quantityChange;
        try {
            quantityChange = Integer.parseInt(qtyRaw);
        } catch (NumberFormatException e) {
            resp.sendRedirect(req.getContextPath()
                    + "/inventory/adjustments?action=new&batchId=" + batchId + "&err=badquantity");
            return;
        }

        InventoryAdjustmentDAO dao = new InventoryAdjustmentDAO();
        AdjustmentResult result = dao.createAdjustment(
                batchId, user.getUserId(), quantityChange, reason, note);

        if (result.ok) {
            resp.sendRedirect(req.getContextPath() + "/inventory/adjustments?ok=created");
        } else {
            resp.sendRedirect(req.getContextPath()
                    + "/inventory/adjustments?action=new&batchId=" + batchId
                    + "&err=" + result.error);
        }
    }

    /* ==================== helpers ==================== */
    /**
     * Gate: must be logged in as OWNER_ADMIN or STAFF. Returns null after
     * redirect — callers return immediately.
     */
    private User requireAccess(HttpServletRequest req, HttpServletResponse resp)
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

    private static int parsePage(String s) {
        if (s == null || s.isEmpty()) {
            return 1;
        }
        try {
            int p = Integer.parseInt(s.trim());
            if (p < 1) {
                return 1;
            }
            return p;
        } catch (NumberFormatException e) {
            return 1;
        }
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

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
