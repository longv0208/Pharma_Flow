package controller;

import dao.InventoryAlertDAO;
import model.InventoryAlert;
import model.InventoryAlertSetting;
import model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * /inventory/alerts — dynamic inventory alerts + global threshold settings.
 * Dispatch via ?action= param (rule.md §22).
 *
 * GET:  (default) alert list, ?action=settings threshold form (CHU_QUAN_QUAN_TRI).
 * POST: ?action=save-settings — CHU_QUAN_QUAN_TRI only.
 *
 * The alerts page is READ-ONLY: it recalculates from live batches on every
 * request — nothing is written to inventory_batches, no movements, no alert
 * rows are stored. Only save-settings writes, and only to
 * inventory_alert_settings (the global product_id IS NULL row).
 *
 * Roles: CHU_QUAN_QUAN_TRI and NHAN_VIEN can view alerts. Only
 * CHU_QUAN_QUAN_TRI may open or save the settings page — NHAN_VIEN is bounced
 * back to the list.
 */
@WebServlet(name = "InventoryAlertServlet", urlPatterns = {"/inventory/alerts"})
public class InventoryAlertServlet extends HttpServlet {

    private static final String LIST_JSP = "/WEB-INF/views/admin/inventory-alerts.jsp";
    private static final String SETTINGS_JSP = "/WEB-INF/views/admin/inventory-alert-settings.jsp";
    private static final int PAGE_SIZE = 20;

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
            case "settings":
                handleSettings(req, resp, user);
                break;
            default:
                handleList(req, resp, user);
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
        if (action == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        switch (action) {
            case "save-settings":
                handleSaveSettings(req, resp, user);
                break;
            default:
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                break;
        }
    }

    /* ==================== GET handlers ==================== */
    /**
     * Alert list — recalculated on every request (nothing persisted).
     * Filters: ?type= one of the four alert types, ?q= product/sku/batch.
     */
    private void handleList(HttpServletRequest req, HttpServletResponse resp, User user)
            throws ServletException, IOException {
        String type = trim(req.getParameter("type"));
        if (!InventoryAlertDAO.isKnownAlertType(type)) {
            type = null;
        }
        String keyword = trim(req.getParameter("q"));

        InventoryAlertDAO dao = new InventoryAlertDAO();
        InventoryAlertSetting settings = dao.getEffectiveSettings();
        int minimum = settings.getMinimumStockLevel();
        int warningDays = settings.getNearExpiryWarningDays();

        int[] summary = dao.getAlertSummary(minimum, warningDays);
        List<InventoryAlert> all = dao.findAlerts(type, keyword, minimum, warningDays);

        int total = all.size();
        int totalPages = (int) Math.ceil((double) total / PAGE_SIZE);
        if (totalPages < 1) {
            totalPages = 1;
        }
        int page = parsePage(req.getParameter("page"));
        if (page > totalPages) {
            page = totalPages;
        }
        int from = (page - 1) * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, total);
        List<InventoryAlert> alerts = new ArrayList<>(all.subList(from, to));

        req.setAttribute("alerts", alerts);
        req.setAttribute("summary", summary);
        req.setAttribute("page", page);
        req.setAttribute("totalPages", totalPages);
        req.setAttribute("total", total);
        req.setAttribute("settings", settings);
        req.setAttribute("ownerAdmin", "CHU_QUAN_QUAN_TRI".equals(user.getRoleName()));
        req.getRequestDispatcher(LIST_JSP).forward(req, resp);
    }

    /**
     * Global settings form — CHU_QUAN_QUAN_TRI only. NHAN_VIEN is bounced to
     * the list.
     */
    private void handleSettings(HttpServletRequest req, HttpServletResponse resp, User user)
            throws ServletException, IOException {
        if (!"CHU_QUAN_QUAN_TRI".equals(user.getRoleName())) {
            resp.sendRedirect(req.getContextPath() + "/inventory/alerts");
            return;
        }
        InventoryAlertDAO dao = new InventoryAlertDAO();
        req.setAttribute("settings", dao.getEffectiveSettings());
        req.getRequestDispatcher(SETTINGS_JSP).forward(req, resp);
    }

    /* ==================== POST handlers ==================== */
    /**
     * Save the single global threshold row (product_id IS NULL).
     * Validation at the controller edge: both values must be integers >= 0.
     * PRG back to the settings form on success.
     */
    private void handleSaveSettings(HttpServletRequest req, HttpServletResponse resp, User user)
            throws IOException {
        if (!"CHU_QUAN_QUAN_TRI".equals(user.getRoleName())) {
            resp.sendRedirect(req.getContextPath() + "/inventory/alerts");
            return;
        }

        Integer minimum = parseNonNegative(req.getParameter("minimumStockLevel"));
        Integer warningDays = parseNonNegative(req.getParameter("nearExpiryWarningDays"));
        if (minimum == null || warningDays == null) {
            resp.sendRedirect(req.getContextPath()
                    + "/inventory/alerts?action=settings&err=invalid");
            return;
        }

        InventoryAlertDAO dao = new InventoryAlertDAO();
        InventoryAlertSetting saved = dao.saveGlobalSettings(minimum, warningDays,
                user.getUserId());
        if (saved == null) {
            resp.sendRedirect(req.getContextPath()
                    + "/inventory/alerts?action=settings&err=db");
            return;
        }
        resp.sendRedirect(req.getContextPath()
                + "/inventory/alerts?action=settings&ok=saved");
    }

    /* ==================== helpers ==================== */
    /**
     * Gate: must be logged in as CHU_QUAN_QUAN_TRI or NHAN_VIEN. Returns null
     * after redirect — callers return immediately.
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
            if ("CHU_QUAN_QUAN_TRI".equals(role) || "NHAN_VIEN".equals(role)) {
                return user;
            }
        }
        resp.sendRedirect(req.getContextPath() + "/authen?action=login");
        return null;
    }

    /** Parse a non-negative integer; null when blank/negative/unparseable. */
    private static Integer parseNonNegative(String s) {
        String t = trim(s);
        if (t.isEmpty()) {
            return null;
        }
        int v;
        try {
            v = Integer.parseInt(t);
        } catch (NumberFormatException e) {
            return null;
        }
        if (v < 0) {
            return null;
        }
        return v;
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

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
