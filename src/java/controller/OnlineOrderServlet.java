package controller;

import dao.CartDAO;
import dao.CustomerProfileDAO;
import dao.OnlineOrderDAO;
import dao.OnlineOrderDAO.OrderResult;
import model.Cart;
import model.CartItem;
import model.CustomerProfile;
import model.OnlineOrder;
import model.OnlineOrderItem;
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
import java.util.List;
import java.util.UUID;

/**
 * /orders — the KHACH_HANG online-order resource: checkout, order history,
 * own order detail, and customer cancellation.
 *
 * GET  actions: (default) history list — checkout, detail (?id=).
 * POST actions: place, cancel.
 *
 * Identity always comes from sessionScope.currentUser resolved to
 * customer_profiles.customer_id — no customer_id is ever taken from the
 * request, and every read/cancel is scoped by that id so one customer can
 * never see or touch another's order (IDOR guard in the DAO + servlet).
 *
 * A rotating `onlineCheckoutToken` (same idea as the POS token) makes every
 * checkout form single-use: refresh/back-button resubmits and stale tabs fail
 * before the DAO transaction ever runs.
 */
@WebServlet(name = "OnlineOrderServlet", urlPatterns = {"/orders"})
public class OnlineOrderServlet extends HttpServlet {

    private static final String CHECKOUT_JSP = "/WEB-INF/views/customer/checkout.jsp";
    private static final String LIST_JSP = "/WEB-INF/views/customer/order-list.jsp";
    private static final String DETAIL_JSP = "/WEB-INF/views/customer/order-detail.jsp";
    private static final int PAGE_SIZE = 10;

    private static final String SESSION_TOKEN = "onlineCheckoutToken";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = requireCustomer(req, resp);
        if (user == null) {
            return;
        }
        Long customerId = new OnlineOrderDAO().resolveCustomerId(user.getUserId());
        if (customerId == null) {
            resp.sendRedirect(req.getContextPath() + "/home?err=profile");
            return;
        }

        String action = req.getParameter("action");
        if (action == null) {
            action = "list";
        }
        switch (action) {
            case "checkout":
                handleCheckoutForm(req, resp, user, customerId);
                break;
            case "detail":
                handleDetail(req, resp, customerId);
                break;
            default:
                handleList(req, resp, customerId);
                break;
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = requireCustomer(req, resp);
        if (user == null) {
            return;
        }
        Long customerId = new OnlineOrderDAO().resolveCustomerId(user.getUserId());
        if (customerId == null) {
            resp.sendRedirect(req.getContextPath() + "/home?err=profile");
            return;
        }

        String action = req.getParameter("action");
        if (action == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        switch (action) {
            case "place":
                handlePlace(req, resp, user, customerId);
                break;
            case "cancel":
                handleCancel(req, resp, user, customerId);
                break;
            default:
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                break;
        }
    }

    /* ==================== GET handlers ==================== */
    /**
     * Checkout form — current cart lines, delivery fields pre-filled from the
     * profile (editable per order), and a fresh single-use token.
     */
    private void handleCheckoutForm(HttpServletRequest req, HttpServletResponse resp,
            User user, long customerId) throws ServletException, IOException {
        HttpSession session = req.getSession();
        CartDAO cartDao = new CartDAO();
        Cart cart = cartDao.findActiveCart(customerId);
        List<CartItem> items = new java.util.ArrayList<>();
        java.math.BigDecimal cartTotal = java.math.BigDecimal.ZERO;
        if (cart != null) {
            items = cartDao.findItems(cart.getCartId());
            for (CartItem item : items) {
                cartTotal = cartTotal.add(item.getSubtotal());
            }
        }
        if (items.isEmpty()) {
            resp.sendRedirect(req.getContextPath() + "/cart?err=empty");
            return;
        }

        CustomerProfile profile = new CustomerProfileDAO().findByUserId(user.getUserId());

        // rotate the token on every render — every checkout form is single-use
        session.setAttribute(SESSION_TOKEN, UUID.randomUUID().toString());

        req.setAttribute("items", items);
        req.setAttribute("cartTotal", cartTotal);
        req.setAttribute("profile", profile);
        req.setAttribute("checkoutToken", session.getAttribute(SESSION_TOKEN));
        req.setAttribute("categories", new dao.CategoryDAO().findAllActive());
        req.getRequestDispatcher(CHECKOUT_JSP).forward(req, resp);
    }

    /** Order history — paginated, newest first, own orders only. */
    private void handleList(HttpServletRequest req, HttpServletResponse resp, long customerId)
            throws ServletException, IOException {
        OnlineOrderDAO dao = new OnlineOrderDAO();
        int total = dao.countOrders(customerId);
        int totalPages = (int) Math.ceil((double) total / PAGE_SIZE);
        if (totalPages < 1) {
            totalPages = 1;
        }
        int page = parsePage(req.getParameter("page"));
        if (page > totalPages) {
            page = totalPages;
        }
        int offset = (page - 1) * PAGE_SIZE;

        List<OnlineOrder> orders = dao.findOrders(customerId, offset, PAGE_SIZE);
        req.setAttribute("orders", orders);
        req.setAttribute("page", page);
        req.setAttribute("totalPages", totalPages);
        req.setAttribute("total", total);
        req.setAttribute("categories", new dao.CategoryDAO().findAllActive());
        req.getRequestDispatcher(LIST_JSP).forward(req, resp);
    }

    /** Own order detail — the DAO's customer-scoped lookup is the IDOR guard. */
    private void handleDetail(HttpServletRequest req, HttpServletResponse resp, long customerId)
            throws ServletException, IOException {
        long orderId = parseId(req.getParameter("id"));
        if (orderId <= 0) {
            resp.sendRedirect(req.getContextPath() + "/orders?err=notfound");
            return;
        }

        OnlineOrderDAO dao = new OnlineOrderDAO();
        OnlineOrder order = dao.findOrderForCustomer(orderId, customerId);
        if (order == null) {
            resp.sendRedirect(req.getContextPath() + "/orders?err=notfound");
            return;
        }
        List<OnlineOrderItem> items = dao.findOrderItems(orderId);
        req.setAttribute("order", order);
        req.setAttribute("items", items);
        req.setAttribute("categories", new dao.CategoryDAO().findAllActive());
        req.getRequestDispatcher(DETAIL_JSP).forward(req, resp);
    }

    /* ==================== POST handlers ==================== */
    /**
     * Place the order — single-use token check first, then the delivery fields
     * are trimmed + length-validated per the DB limits, then OnlineOrderDAO
     * re-validates everything (stock, sellability, price) under row locks.
     * The cart survives a failed checkout so the customer can fix and retry.
     */
    private void handlePlace(HttpServletRequest req, HttpServletResponse resp,
            User user, long customerId) throws IOException {
        HttpSession session = req.getSession();

        String expected = (String) session.getAttribute(SESSION_TOKEN);
        String sent = trim(req.getParameter("checkoutToken"));
        if (expected == null || !expected.equals(sent)) {
            resp.sendRedirect(req.getContextPath() + "/cart?err=token");
            return;
        }
        // burn the token immediately — refresh/back-button resubmits fail
        session.removeAttribute(SESSION_TOKEN);

        // Delivery snapshot — backend limits are authoritative, trim everything.
        String customerName = trim(req.getParameter("customerName"));
        String customerPhone = trim(req.getParameter("customerPhone"));
        String provinceCity = trim(req.getParameter("provinceCity"));
        String district = trim(req.getParameter("district"));
        String ward = trim(req.getParameter("ward"));
        String detailedAddress = trim(req.getParameter("detailedAddress"));

        String err = validateDelivery(customerName, customerPhone, provinceCity,
                district, ward, detailedAddress);
        if (err != null) {
            resp.sendRedirect(req.getContextPath()
                    + "/orders?action=checkout&err=" + err);
            return;
        }

        OnlineOrderDAO dao = new OnlineOrderDAO();
        OrderResult result = dao.placeOrder(user.getUserId(), customerId,
                customerName, customerPhone, provinceCity, district, ward,
                detailedAddress);

        if (result.ok) {
            resp.sendRedirect(req.getContextPath()
                    + "/orders?action=detail&id=" + result.orderId + "&ok=placed");
            return;
        }
        String url = req.getContextPath() + "/cart?err=" + result.error;
        if (result.detail != null && !result.detail.isEmpty()) {
            url = url + "&msg=" + URLEncoder.encode(result.detail, StandardCharsets.UTF_8);
        }
        resp.sendRedirect(url);
    }

    /**
     * Customer cancel — the DAO re-checks ownership + CHO_XU_LY under a row
     * lock and releases every DANG_GIU reservation in the same transaction.
     */
    private void handleCancel(HttpServletRequest req, HttpServletResponse resp,
            User user, long customerId) throws IOException {
        long orderId = parseId(req.getParameter("orderId"));
        if (orderId <= 0) {
            resp.sendRedirect(req.getContextPath() + "/orders?err=notfound");
            return;
        }

        OnlineOrderDAO dao = new OnlineOrderDAO();
        OrderResult result = dao.cancelOrder(user.getUserId(), customerId, orderId);
        if (result.ok) {
            resp.sendRedirect(req.getContextPath()
                    + "/orders?action=detail&id=" + orderId + "&ok=cancelled");
            return;
        }
        resp.sendRedirect(req.getContextPath()
                + "/orders?action=detail&id=" + orderId + "&err=" + result.error);
    }

    /* ==================== helpers ==================== */
    /** KHACH_HANG-only gate — orders belong to the customer role. */
    private User requireCustomer(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession session = req.getSession(false);
        Object u = null;
        if (session != null) {
            u = session.getAttribute("currentUser");
        }
        if (u instanceof User) {
            User user = (User) u;
            if ("KHACH_HANG".equals(user.getRoleName())) {
                return user;
            }
            resp.sendRedirect(req.getContextPath() + "/home?err=role");
            return null;
        }
        resp.sendRedirect(req.getContextPath() + "/authen?action=login");
        return null;
    }

    /**
     * Delivery-field validation — required + exact DB varchar limits. Returns
     * the error code, or null when everything passes.
     */
    private static String validateDelivery(String customerName, String customerPhone,
            String provinceCity, String district, String ward, String detailedAddress) {
        if (customerName.isEmpty() || customerName.length() > 150) {
            return "name";
        }
        if (customerPhone.isEmpty() || customerPhone.length() > 30) {
            return "phone";
        }
        if (provinceCity.isEmpty() || provinceCity.length() > 100) {
            return "province";
        }
        if (district.isEmpty() || district.length() > 100) {
            return "district";
        }
        if (ward.isEmpty() || ward.length() > 100) {
            return "ward";
        }
        if (detailedAddress.isEmpty() || detailedAddress.length() > 255) {
            return "address";
        }
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
