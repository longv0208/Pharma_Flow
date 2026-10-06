package controller;

import dao.StocktakeDAO;
import dao.StocktakeDAO.StocktakeResult;
import model.Stocktake;
import model.StocktakeItem;
import model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * /inventory/stocktakes — physical inventory counting and reconciliation.
 * Dispatch via ?action= param (rule.md §22).
 *
 * GET:  (default) stocktake list, ?action=detail&id=N count/review screen.
 * POST: ?action=create | start | save | complete — every write runs in one
 * JDBC transaction inside the DAO, then redirect (PRG).
 *
 * Roles: OWNER_ADMIN and STAFF — everything else bounced to login.
 * The browser only supplies stocktake id + stocktakeItemId + actualQuantity;
 * system quantity, differences and batch status are always recomputed from
 * locked DB rows inside the transaction.
 */
@WebServlet(name = "StocktakeServlet", urlPatterns = {"/inventory/stocktakes"})
public class StocktakeServlet extends HttpServlet {

    private static final String LIST_JSP = "/WEB-INF/views/admin/stocktake-list.jsp";
    private static final String DETAIL_JSP = "/WEB-INF/views/admin/stocktake-detail.jsp";
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
            case "create":
                handleCreate(req, resp, user);
                break;
            case "start":
                handleStart(req, resp);
                break;
            case "save":
                handleSave(req, resp);
                break;
            case "complete":
                handleComplete(req, resp, user);
                break;
            default:
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                break;
        }
    }

    /* ==================== GET handlers ==================== */
    /**
     * Stocktake List — newest first, status filter + pagination.
     */
    private void handleList(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String status = trim(req.getParameter("status"));

        StocktakeDAO dao = new StocktakeDAO();
        int total = dao.countAll(status);
        int totalPages = (int) Math.ceil((double) total / PAGE_SIZE);
        if (totalPages < 1) {
            totalPages = 1;
        }
        int page = parsePage(req.getParameter("page"));
        if (page > totalPages) {
            page = totalPages;
        }
        int offset = (page - 1) * PAGE_SIZE;

        List<Stocktake> stocktakes = dao.findAll(status, offset, PAGE_SIZE);

        req.setAttribute("stocktakes", stocktakes);
        req.setAttribute("page", page);
        req.setAttribute("totalPages", totalPages);
        req.setAttribute("total", total);
        req.getRequestDispatcher(LIST_JSP).forward(req, resp);
    }

    /**
     * Stocktake Detail — same page renders Start view (DRAFT), counting view
     * (IN_PROGRESS) or read-only reconciliation result (COMPLETED).
     */
    private void handleDetail(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long stocktakeId = parseId(req.getParameter("id"));
        if (stocktakeId <= 0) {
            resp.sendRedirect(req.getContextPath() + "/inventory/stocktakes?err=notfound");
            return;
        }

        StocktakeDAO dao = new StocktakeDAO();
        Stocktake stocktake = dao.findById(stocktakeId);
        if (stocktake == null) {
            resp.sendRedirect(req.getContextPath() + "/inventory/stocktakes?err=notfound");
            return;
        }

        String keyword = trim(req.getParameter("q"));
        List<StocktakeItem> items = dao.findItems(stocktakeId, keyword);

        // Summary numbers for the completed screen.
        int matched = 0;
        int netDifference = 0;
        for (StocktakeItem item : items) {
            Integer diff = item.getDifferenceQuantity();
            if (diff != null && diff != 0) {
                netDifference += diff;
            } else if (diff != null) {
                matched++;
            }
        }

        req.setAttribute("stocktake", stocktake);
        req.setAttribute("items", items);
        req.setAttribute("matchedCount", matched);
        req.setAttribute("netDifference", netDifference);
        req.getRequestDispatcher(DETAIL_JSP).forward(req, resp);
    }

    /* ==================== POST handlers ==================== */
    /** Create a DRAFT stocktake owned by the current user, then open it. */
    private void handleCreate(HttpServletRequest req, HttpServletResponse resp, User user)
            throws IOException {
        StocktakeDAO dao = new StocktakeDAO();
        long stocktakeId = dao.createStocktake(user.getUserId());
        if (stocktakeId <= 0) {
            resp.sendRedirect(req.getContextPath() + "/inventory/stocktakes?err=db");
            return;
        }
        resp.sendRedirect(req.getContextPath()
                + "/inventory/stocktakes?action=detail&id=" + stocktakeId + "&ok=created");
    }

    /** DRAFT -> IN_PROGRESS: snapshot every current batch into items. */
    private void handleStart(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        long stocktakeId = parseId(req.getParameter("id"));
        if (stocktakeId <= 0) {
            resp.sendRedirect(req.getContextPath() + "/inventory/stocktakes?err=notfound");
            return;
        }

        StocktakeDAO dao = new StocktakeDAO();
        StocktakeResult result = dao.startStocktake(stocktakeId);
        if (result.ok) {
            resp.sendRedirect(req.getContextPath()
                    + "/inventory/stocktakes?action=detail&id=" + stocktakeId + "&ok=started");
        } else {
            redirectDetailError(req, resp, stocktakeId, result);
        }
    }

    /**
     * Save counting progress. Every qty_<stocktakeItemId> parameter is
     * applied: a number stores the count, a BLANK clears it back to NULL
     * ("not counted") so the backend never disagrees with what the form
     * shows. Item ownership is re-verified in the DAO.
     */
    private void handleSave(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        long stocktakeId = parseId(req.getParameter("id"));
        if (stocktakeId <= 0) {
            resp.sendRedirect(req.getContextPath() + "/inventory/stocktakes?err=notfound");
            return;
        }

        Map<Long, Integer> counts = parseCounts(req);
        if (counts == null) {
            resp.sendRedirect(req.getContextPath()
                    + "/inventory/stocktakes?action=detail&id=" + stocktakeId
                    + "&err=badquantity");
            return;
        }

        StocktakeDAO dao = new StocktakeDAO();
        StocktakeResult result = dao.saveCounts(stocktakeId, counts);
        if (result.ok) {
            resp.sendRedirect(req.getContextPath()
                    + "/inventory/stocktakes?action=detail&id=" + stocktakeId + "&ok=saved");
        } else {
            redirectDetailError(req, resp, stocktakeId, result);
        }
    }

    /**
     * Reconcile every counted item against the live on_hand and complete.
     * Any qty_* inputs still sitting in the form are saved first so a user
     * who typed counts and clicked Complete in one motion doesn't lose them.
     */
    private void handleComplete(HttpServletRequest req, HttpServletResponse resp, User user)
            throws IOException {
        long stocktakeId = parseId(req.getParameter("id"));
        if (stocktakeId <= 0) {
            resp.sendRedirect(req.getContextPath() + "/inventory/stocktakes?err=notfound");
            return;
        }

        StocktakeDAO dao = new StocktakeDAO();

        Map<Long, Integer> counts = parseCounts(req);
        if (counts == null) {
            resp.sendRedirect(req.getContextPath()
                    + "/inventory/stocktakes?action=detail&id=" + stocktakeId
                    + "&err=badquantity");
            return;
        }
        if (!counts.isEmpty()) {
            StocktakeResult save = dao.saveCounts(stocktakeId, counts);
            if (!save.ok) {
                redirectDetailError(req, resp, stocktakeId, save);
                return;
            }
        }

        StocktakeResult result = dao.completeStocktake(stocktakeId, user.getUserId());
        if (result.ok) {
            resp.sendRedirect(req.getContextPath()
                    + "/inventory/stocktakes?action=detail&id=" + stocktakeId + "&ok=completed");
        } else {
            redirectDetailError(req, resp, stocktakeId, result);
        }
    }

    /* ==================== helpers ==================== */
    /**
     * Collect qty_<stocktakeItemId> parameters into an itemId -> count map.
     * A BLANK input maps to null — it means "cleared, not counted" and the
     * DAO must NULL the stored count, never silently keep the old value.
     * An unparseable or negative value returns null so the caller can fail.
     */
    private static Map<Long, Integer> parseCounts(HttpServletRequest req) {
        Map<Long, Integer> counts = new HashMap<>();
        for (String name : req.getParameterMap().keySet()) {
            if (!name.startsWith("qty_")) {
                continue;
            }
            String raw = trim(req.getParameter(name));
            long itemId = parseId(name.substring(4));
            if (itemId <= 0) {
                return null;
            }
            if (raw.isEmpty()) {
                // Cleared input → mark the batch as not counted again.
                counts.put(itemId, null);
                continue;
            }
            int actual;
            try {
                actual = Integer.parseInt(raw);
            } catch (NumberFormatException e) {
                return null;
            }
            if (actual < 0) {
                return null;
            }
            counts.put(itemId, actual);
        }
        return counts;
    }

    /** Redirect back to the detail page carrying err= and optional detail. */
    private void redirectDetailError(HttpServletRequest req, HttpServletResponse resp,
            long stocktakeId, StocktakeResult result) throws IOException {
        String url = req.getContextPath()
                + "/inventory/stocktakes?action=detail&id=" + stocktakeId
                + "&err=" + result.error;
        if (result.detail != null && !result.detail.isEmpty()) {
            url += "&msg=" + URLEncoder.encode(result.detail, StandardCharsets.UTF_8);
        }
        resp.sendRedirect(url);
    }

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

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
