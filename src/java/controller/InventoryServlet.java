package controller;

import dao.CategoryDAO;
import dao.InventoryDAO;
import dao.InventoryDAO.ProductInventoryRow;
import dao.InventoryDAO.StatusResult;
import model.InventoryBatch;
import model.InventoryMovement;
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
 * /inventory — Inventory visibility, batch detail, block/unblock, history.
 * Dispatch via ?action= param (rule.md §22).
 *
 * GET actions: list (default), product, batch, history POST actions:
 * block-batch, unblock-batch
 *
 * Roles: CHU_QUAN_QUAN_TRI and NHAN_VIEN — everything else is bounced to login. Stock
 * quantities are NEVER edited here; only batch status changes and they always
 * write an audit movement inside one JDBC transaction.
 */
@WebServlet(name = "InventoryServlet", urlPatterns = {"/inventory"})
public class InventoryServlet extends HttpServlet {

    private static final String LIST_JSP = "/WEB-INF/views/admin/inventory-list.jsp";
    private static final String PRODUCT_JSP = "/WEB-INF/views/admin/inventory-product.jsp";
    private static final String BATCH_JSP = "/WEB-INF/views/admin/inventory-batch.jsp";
    private static final String HISTORY_JSP = "/WEB-INF/views/admin/inventory-history.jsp";
    private static final int PAGE_SIZE = 20;
    private static final int HISTORY_PAGE_SIZE = 20;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = requireInventoryAccess(req, resp);
        if (user == null) {
            return;
        }
        String action = req.getParameter("action");
        if (action == null) {
            action = "list";
        }

        switch (action) {
            case "product":
                handleProductDetail(req, resp);
                break;
            case "batch":
                handleBatchDetail(req, resp);
                break;
            case "history":
                handleHistory(req, resp);
                break;
            default:
                handleList(req, resp);
                break;
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = requireInventoryAccess(req, resp);
        if (user == null) {
            return;
        }
        String action = req.getParameter("action");
        if (action == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        switch (action) {
            case "block-batch":
                handleBlockBatch(req, resp, user);
                break;
            case "unblock-batch":
                handleUnblockBatch(req, resp, user);
                break;
            default:
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                break;
        }
    }

    /* ==================== GET handlers ==================== */
    /**
     * Inventory List — products with aggregated batch totals, paginated.
     */
    private void handleList(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String keyword = trim(req.getParameter("q"));
        Long categoryId = parseIdOrNull(req.getParameter("categoryId"));
        String status = trim(req.getParameter("status"));

        InventoryDAO dao = new InventoryDAO();
        int total = dao.countInventoryProducts(keyword, categoryId, status);
        int totalPages = (int) Math.ceil((double) total / PAGE_SIZE);
        if (totalPages < 1) {
            totalPages = 1;
        }
        int page = parsePage(req.getParameter("page"));
        if (page > totalPages) {
            page = totalPages;
        }
        int offset = (page - 1) * PAGE_SIZE;

        List<ProductInventoryRow> products = dao.findInventoryProducts(
                keyword, categoryId, status, offset, PAGE_SIZE);

        req.setAttribute("products", products);
        req.setAttribute("categories", new CategoryDAO().findAllActive());
        req.setAttribute("page", page);
        req.setAttribute("totalPages", totalPages);
        req.setAttribute("total", total);
        req.getRequestDispatcher(LIST_JSP).forward(req, resp);
    }

    /**
     * Product Inventory Detail — one product + all its batches (FEFO order).
     */
    private void handleProductDetail(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long productId = parseId(req.getParameter("id"));
        if (productId <= 0) {
            resp.sendRedirect(req.getContextPath() + "/inventory?err=notfound");
            return;
        }

        InventoryDAO dao = new InventoryDAO();
        ProductInventoryRow product = dao.findProductInventorySummary(productId);
        if (product == null) {
            resp.sendRedirect(req.getContextPath() + "/inventory?err=notfound");
            return;
        }

        List<InventoryBatch> batches = dao.findBatchesByProduct(productId);
        req.setAttribute("product", product);
        req.setAttribute("batches", batches);
        req.getRequestDispatcher(PRODUCT_JSP).forward(req, resp);
    }

    /**
     * Batch Detail — one batch with product + supplier + receipt context.
     */
    private void handleBatchDetail(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long batchId = parseId(req.getParameter("id"));
        if (batchId <= 0) {
            resp.sendRedirect(req.getContextPath() + "/inventory?err=notfound");
            return;
        }

        InventoryDAO dao = new InventoryDAO();
        InventoryBatch batch = dao.findBatchById(batchId);
        if (batch == null) {
            resp.sendRedirect(req.getContextPath() + "/inventory?err=notfound");
            return;
        }

        // Expiry warning — display only, never writes to DB.
        String expiryWarning = null;
        if (batch.getExpiryDate() != null) {
            Date today = new Date(System.currentTimeMillis());
            if (!batch.getExpiryDate().after(today)) {
                expiryWarning = "EXPIRED";
            } else {
                long diffMs = batch.getExpiryDate().getTime() - today.getTime();
                long diffDays = diffMs / (1000L * 60 * 60 * 24);
                if (diffDays <= InventoryDAO.NEAR_EXPIRY_DAYS) {
                    expiryWarning = "NEAR_EXPIRY";
                }
            }
        }
        req.setAttribute("batch", batch);
        req.setAttribute("expiryWarning", expiryWarning);
        req.getRequestDispatcher(BATCH_JSP).forward(req, resp);
    }

    /**
     * Inventory History — paginated movement audit trail.
     */
    private void handleHistory(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String productKeyword = trim(req.getParameter("q"));
        String batchNumber = trim(req.getParameter("batch"));
        String movementType = trim(req.getParameter("type"));
        Long productId = parseIdOrNull(req.getParameter("productId"));
        Long batchId = parseIdOrNull(req.getParameter("batchId"));
        Date fromDate = parseDate(req.getParameter("from"));
        Date toDate = parseDate(req.getParameter("to"));

        InventoryDAO dao = new InventoryDAO();
        int total = dao.countMovements(
                productKeyword, batchNumber, movementType, productId, batchId,
                fromDate, toDate);
        int totalPages = (int) Math.ceil((double) total / HISTORY_PAGE_SIZE);
        if (totalPages < 1) {
            totalPages = 1;
        }
        int page = parsePage(req.getParameter("page"));
        if (page > totalPages) {
            page = totalPages;
        }
        int offset = (page - 1) * HISTORY_PAGE_SIZE;

        List<InventoryMovement> movements = dao.findMovements(
                productKeyword, batchNumber, movementType, productId, batchId,
                fromDate, toDate, offset, HISTORY_PAGE_SIZE);

        req.setAttribute("movements", movements);
        req.setAttribute("page", page);
        req.setAttribute("totalPages", totalPages);
        req.setAttribute("total", total);
        req.getRequestDispatcher(HISTORY_JSP).forward(req, resp);
    }

    /* ==================== POST handlers ==================== */
    /**
     * Block a batch — status → BI_KHOA, movement type KHOA, one transaction.
     */
    private void handleBlockBatch(HttpServletRequest req, HttpServletResponse resp, User user)
            throws IOException {
        long batchId = parseId(req.getParameter("batchId"));
        if (batchId <= 0) {
            resp.sendRedirect(req.getContextPath() + "/inventory?err=notfound");
            return;
        }

        String reason = trim(req.getParameter("reason"));
        if (reason.isEmpty()) {
            resp.sendRedirect(req.getContextPath()
                    + "/inventory?action=batch&id=" + batchId + "&err=reasonrequired");
            return;
        }

        InventoryDAO dao = new InventoryDAO();
        StatusResult result = dao.blockBatch(batchId, user.getUserId(), reason);
        if (result.ok) {
            resp.sendRedirect(req.getContextPath()
                    + "/inventory?action=batch&id=" + batchId + "&ok=blocked");
        } else {
            resp.sendRedirect(req.getContextPath()
                    + "/inventory?action=batch&id=" + batchId + "&err=" + result.error);
        }
    }

    /**
     * Unblock a batch — backend picks the safe post-unblock status.
     */
    private void handleUnblockBatch(HttpServletRequest req, HttpServletResponse resp, User user)
            throws IOException {
        long batchId = parseId(req.getParameter("batchId"));
        if (batchId <= 0) {
            resp.sendRedirect(req.getContextPath() + "/inventory?err=notfound");
            return;
        }

        String reason = trim(req.getParameter("reason"));
        if (reason.isEmpty()) {
            resp.sendRedirect(req.getContextPath()
                    + "/inventory?action=batch&id=" + batchId + "&err=reasonrequired");
            return;
        }

        InventoryDAO dao = new InventoryDAO();
        StatusResult result = dao.unblockBatch(batchId, user.getUserId(), reason);
        if (result.ok) {
            resp.sendRedirect(req.getContextPath()
                    + "/inventory?action=batch&id=" + batchId + "&ok=unblocked");
        } else {
            resp.sendRedirect(req.getContextPath()
                    + "/inventory?action=batch&id=" + batchId + "&err=" + result.error);
        }
    }

    /* ==================== helpers ==================== */
    /**
     * Gate: must be logged in as CHU_QUAN_QUAN_TRI or NHAN_VIEN. Returns null
     * after redirect — callers return immediately.
     */
    private User requireInventoryAccess(HttpServletRequest req, HttpServletResponse resp)
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

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
