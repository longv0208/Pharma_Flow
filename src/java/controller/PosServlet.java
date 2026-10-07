package controller;

import dao.PosDAO;
import dao.PosDAO.CheckoutResult;
import model.PosCartItem;
import model.Prescription;
import model.Product;
import model.SaleItem;
import model.SaleItemBatchAllocation;
import model.SaleTransaction;
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
import java.sql.Date;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * /pos — the STAFF point-of-sale counter.
 *
 * GET  actions: (default) main screen — search box (?q=) + cart + checkout
 *               form; history — past sales list; detail — one receipt (?id=).
 * POST actions: add, update-qty, remove, clear, checkout.
 *
 * Roles: STAFF only — the counter is a staff tool (do NOT widen to
 * OWNER_ADMIN). Cart lives in the session as a LinkedHashMap
 * productId -> PosCartItem; a rotating `posCheckoutToken` blocks
 * double-submit and stale-tab checkouts.
 */
@WebServlet(name = "PosServlet", urlPatterns = {"/pos"})
public class PosServlet extends HttpServlet {

    private static final String MAIN_JSP = "/WEB-INF/views/pos/dashboard.jsp";
    private static final String DETAIL_JSP = "/WEB-INF/views/pos/pos-detail.jsp";
    private static final String HISTORY_JSP = "/WEB-INF/views/pos/pos-history.jsp";
    private static final int HISTORY_PAGE_SIZE = 20;

    private static final String SESSION_CART = "posCart";
    private static final String SESSION_TOKEN = "posCheckoutToken";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = requireStaff(req, resp);
        if (user == null) {
            return;
        }
        String action = req.getParameter("action");
        if (action == null) {
            action = "main";
        }

        switch (action) {
            case "history":
                handleHistory(req, resp);
                break;
            case "detail":
                handleDetail(req, resp);
                break;
            default:
                handleMain(req, resp);
                break;
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = requireStaff(req, resp);
        if (user == null) {
            return;
        }
        String action = req.getParameter("action");
        if (action == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        switch (action) {
            case "add":
                handleAdd(req, resp);
                break;
            case "update-qty":
                handleUpdateQty(req, resp);
                break;
            case "remove":
                handleRemove(req, resp);
                break;
            case "clear":
                handleClear(req, resp);
                break;
            case "checkout":
                handleCheckout(req, resp, user);
                break;
            default:
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                break;
        }
    }

    /* ==================== GET handlers ==================== */
    /**
     * Main POS screen — product search results for ?q=, the session cart, and
     * a fresh checkout token.
     */
    private void handleMain(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession();
        PosDAO dao = new PosDAO();

        String keyword = trim(req.getParameter("q"));
        List<Product> results = new ArrayList<>();
        if (!keyword.isEmpty()) {
            results = dao.findProductsForPos(keyword);
        }

        Map<Long, PosCartItem> cart = getCart(session);
        List<PosCartItem> cartLines = new ArrayList<>(cart.values());
        java.math.BigDecimal cartTotal = java.math.BigDecimal.ZERO;
        boolean cartHasRx = false;
        for (PosCartItem line : cartLines) {
            cartTotal = cartTotal.add(line.getSubtotal());
            if (line.isRx()) {
                cartHasRx = true;
            }
            // refresh the display stock so the badge is not stale
            line.setSaleableQuantity(dao.getSaleableQuantity(line.getProductId()));
        }

        // rotate the token on every render — every checkout form is single-use
        session.setAttribute(SESSION_TOKEN, UUID.randomUUID().toString());

        req.setAttribute("q", keyword);
        req.setAttribute("results", results);
        req.setAttribute("cartLines", cartLines);
        req.setAttribute("cartTotal", cartTotal);
        req.setAttribute("cartHasRx", cartHasRx);
        req.setAttribute("paymentMethods", PosDAO.PAYMENT_METHODS);
        req.setAttribute("checkoutToken", session.getAttribute(SESSION_TOKEN));
        req.getRequestDispatcher(MAIN_JSP).forward(req, resp);
    }

    /** Sale history — paginated, filterable by date range + payment method. */
    private void handleHistory(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String payment = trim(req.getParameter("payment"));
        Date from = parseDate(req.getParameter("from"));
        Date to = parseDate(req.getParameter("to"));

        PosDAO dao = new PosDAO();
        int total = dao.countSales(payment, from, to);
        int totalPages = (int) Math.ceil((double) total / HISTORY_PAGE_SIZE);
        if (totalPages < 1) {
            totalPages = 1;
        }
        int page = parsePage(req.getParameter("page"));
        if (page > totalPages) {
            page = totalPages;
        }
        int offset = (page - 1) * HISTORY_PAGE_SIZE;

        List<SaleTransaction> sales = dao.findSales(payment, from, to,
                offset, HISTORY_PAGE_SIZE);

        req.setAttribute("sales", sales);
        req.setAttribute("payment", payment);
        req.setAttribute("from", trim(req.getParameter("from")));
        req.setAttribute("to", trim(req.getParameter("to")));
        req.setAttribute("paymentMethods", PosDAO.PAYMENT_METHODS);
        req.setAttribute("page", page);
        req.setAttribute("totalPages", totalPages);
        req.setAttribute("total", total);
        req.getRequestDispatcher(HISTORY_JSP).forward(req, resp);
    }

    /** Sale detail / receipt — header + lines + batch traceability, read-only. */
    private void handleDetail(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long saleId = parseId(req.getParameter("id"));
        if (saleId <= 0) {
            resp.sendRedirect(req.getContextPath() + "/pos?action=history&err=notfound");
            return;
        }

        PosDAO dao = new PosDAO();
        SaleTransaction sale = dao.findSaleById(saleId);
        if (sale == null) {
            resp.sendRedirect(req.getContextPath() + "/pos?action=history&err=notfound");
            return;
        }
        List<SaleItem> items = dao.findSaleItems(saleId);
        List<SaleItemBatchAllocation> allocations = dao.findSaleAllocations(saleId);
        Prescription prescription = null;
        if (sale.getPrescriptionId() != null) {
            prescription = dao.findSalePrescription(sale.getPrescriptionId());
        }

        req.setAttribute("sale", sale);
        req.setAttribute("items", items);
        req.setAttribute("allocations", allocations);
        req.setAttribute("prescription", prescription);
        req.getRequestDispatcher(DETAIL_JSP).forward(req, resp);
    }

    /* ==================== POST handlers ==================== */
    /**
     * Add one product to the cart (or bump its quantity). Rejects RESTRICTED
     * items at add time too — they can be shown in search but never sold.
     */
    private void handleAdd(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession session = req.getSession();
        long productId = parseId(req.getParameter("productId"));
        String back = trim(req.getParameter("q"));
        if (productId <= 0) {
            redirectMain(resp, req, back, "notfound", null);
            return;
        }
        int qty = parseQty(req.getParameter("qty"), 1);

        PosDAO dao = new PosDAO();
        Product product = dao.findPosProduct(productId);
        if (product == null) {
            redirectMain(resp, req, back, "PRODUCT_NOT_FOUND", null);
            return;
        }
        if (!"ACTIVE".equals(product.getStatus())) {
            redirectMain(resp, req, back, "PRODUCT_INACTIVE", product.getProductName());
            return;
        }
        if (product.getProductType() != null
                && "RESTRICTED".equals(product.getProductType().name())) {
            redirectMain(resp, req, back, "RESTRICTED_NOT_ALLOWED", product.getProductName());
            return;
        }

        Map<Long, PosCartItem> cart = getCart(session);
        PosCartItem line = cart.get(productId);
        if (line == null) {
            line = new PosCartItem();
            line.setProductId(productId);
            line.setProductName(product.getProductName());
            line.setSku(product.getSku());
            line.setProductType(product.getProductType().name());
            line.setSellingUnit(product.getSellingUnit());
            line.setUnitPrice(product.getSellingPrice());
            line.setQuantity(0);
            cart.put(productId, line);
        }
        int newQty = line.getQuantity() + qty;
        long saleable = dao.getSaleableQuantity(productId);
        if (newQty > saleable) {
            redirectMain(resp, req, back, "INSUFFICIENT_STOCK",
                    product.getProductName() + " (max " + saleable + ")");
            return;
        }
        line.setQuantity(newQty);
        line.setSaleableQuantity(saleable);
        redirectMain(resp, req, back, null, "added");
    }

    /** Set the exact quantity of one cart line; 0 or less removes the line. */
    private void handleUpdateQty(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession session = req.getSession();
        long productId = parseId(req.getParameter("productId"));
        int qty = parseQty(req.getParameter("qty"), 0);

        Map<Long, PosCartItem> cart = getCart(session);
        PosCartItem line = cart.get(productId);
        if (line == null) {
            redirectMain(resp, req, "", "notfound", null);
            return;
        }
        if (qty <= 0) {
            cart.remove(productId);
            redirectMain(resp, req, "", null, "removed");
            return;
        }
        long saleable = new PosDAO().getSaleableQuantity(productId);
        if (qty > saleable) {
            redirectMain(resp, req, "", "INSUFFICIENT_STOCK",
                    line.getProductName() + " (max " + saleable + ")");
            return;
        }
        line.setQuantity(qty);
        line.setSaleableQuantity(saleable);
        redirectMain(resp, req, "", null, "updated");
    }

    /** Remove one line from the cart. */
    private void handleRemove(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession session = req.getSession();
        long productId = parseId(req.getParameter("productId"));
        getCart(session).remove(productId);
        redirectMain(resp, req, "", null, "removed");
    }

    /** Clear the whole cart. */
    private void handleClear(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession session = req.getSession();
        session.removeAttribute(SESSION_CART);
        redirectMain(resp, req, "", null, "cleared");
    }

    /**
     * Checkout — validates the single-use token, then hands the whole thing to
     * PosDAO.completeSale which re-validates everything under row locks. The
     * cart survives a failed checkout; a success clears it.
     */
    private void handleCheckout(HttpServletRequest req, HttpServletResponse resp, User user)
            throws IOException {
        HttpSession session = req.getSession();

        String expected = (String) session.getAttribute(SESSION_TOKEN);
        String sent = trim(req.getParameter("checkoutToken"));
        if (expected == null || !expected.equals(sent)) {
            redirectMain(resp, req, "", "INVALID_TOKEN", null);
            return;
        }
        // burn the token immediately — refresh/back-button resubmits fail
        session.removeAttribute(SESSION_TOKEN);

        Map<Long, PosCartItem> cart = getCart(session);
        if (cart.isEmpty()) {
            redirectMain(resp, req, "", "EMPTY_CART", null);
            return;
        }

        String paymentMethod = trim(req.getParameter("paymentMethod"));
        // Manual Rx check — staff confirmed a valid external paper
        // prescription; the names go into the audit row, the checkbox flag
        // proves the check actually happened.
        String prescriber = trim(req.getParameter("prescriber"));
        String healthcareFacility = trim(req.getParameter("healthcareFacility"));
        boolean prescriptionChecked = req.getParameter("prescriptionChecked") != null;

        PosDAO dao = new PosDAO();
        CheckoutResult result = dao.completeSale(
                user.getUserId(), new ArrayList<>(cart.values()),
                paymentMethod, prescriber, healthcareFacility, prescriptionChecked);

        if (result.ok) {
            session.removeAttribute(SESSION_CART);
            resp.sendRedirect(req.getContextPath()
                    + "/pos?action=detail&id=" + result.saleId + "&ok=completed");
            return;
        }
        // failure: keep the cart so staff can fix quantities and retry
        String err = result.error;
        if (result.detail != null && !result.detail.isEmpty()) {
            err = err + "&msg=" + URLEncoder.encode(result.detail,
                    StandardCharsets.UTF_8);
        }
        redirectMain(resp, req, "", err, null);
    }

    /* ==================== helpers ==================== */
    /** STAFF-only gate. Returns null after redirect — callers return at once. */
    private User requireStaff(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession session = req.getSession(false);
        Object u = null;
        if (session != null) {
            u = session.getAttribute("currentUser");
        }
        if (u instanceof User) {
            User user = (User) u;
            if ("STAFF".equals(user.getRoleName())) {
                return user;
            }
        }
        resp.sendRedirect(req.getContextPath() + "/authen?action=login");
        return null;
    }

    /** The session cart — created on first use, never null. */
    @SuppressWarnings("unchecked")
    private Map<Long, PosCartItem> getCart(HttpSession session) {
        Object c = session.getAttribute(SESSION_CART);
        if (c instanceof Map) {
            return (Map<Long, PosCartItem>) c;
        }
        Map<Long, PosCartItem> cart = new LinkedHashMap<>();
        session.setAttribute(SESSION_CART, cart);
        return cart;
    }

    /**
     * PRG back to the main screen, carrying ?q= so the search box keeps its
     * keyword, plus an error/success code (with optional detail message).
     */
    private void redirectMain(HttpServletResponse resp, HttpServletRequest req,
            String keyword, String err, String ok) throws IOException {
        StringBuilder url = new StringBuilder(req.getContextPath() + "/pos");
        String sep = "?";
        if (keyword != null && !keyword.isEmpty()) {
            url.append(sep).append("q=")
                    .append(URLEncoder.encode(keyword, StandardCharsets.UTF_8));
            sep = "&";
        }
        if (err != null) {
            url.append(sep).append("err=").append(err);
            sep = "&";
        }
        if (ok != null) {
            url.append(sep).append("ok=").append(ok);
            sep = "&";
        }
        resp.sendRedirect(url.toString());
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

    /** Quantity parse with fallback — bad input becomes the fallback, never a 500. */
    private static int parseQty(String s, int fallback) {
        if (s == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
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
