package controller;

import dao.CartDAO;
import dao.OnlineOrderDAO;
import model.Cart;
import model.CartItem;
import model.Product;
import model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

/**
 * /cart — the KHACH_HANG shopping cart backed by `carts` + `cart_items`.
 *
 * GET  (default): render the current DANG_HOAT_DONG cart.
 * POST actions:   add, update, remove, clear — all PRG back to /cart.
 *
 * Identity always comes from sessionScope.currentUser + the resolved
 * customer_profiles.customer_id — customer_id is never read from the request,
 * and every item write is additionally scoped by cart_id so a customer can
 * only ever touch their own lines.
 */
@WebServlet(name = "CartServlet", urlPatterns = {"/cart"})
public class CartServlet extends HttpServlet {

    private static final String CART_JSP = "/WEB-INF/views/customer/cart.jsp";

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

        CartDAO dao = new CartDAO();
        Cart cart = dao.findActiveCart(customerId);
        List<CartItem> items = new java.util.ArrayList<>();
        java.math.BigDecimal cartTotal = java.math.BigDecimal.ZERO;
        if (cart != null) {
            items = dao.findItems(cart.getCartId());
            for (CartItem item : items) {
                cartTotal = cartTotal.add(item.getSubtotal());
            }
        }

        req.setAttribute("items", items);
        req.setAttribute("cartTotal", cartTotal);
        req.setAttribute("categories", new dao.CategoryDAO().findAllActive());
        req.getRequestDispatcher(CART_JSP).forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = requireCustomer(req, resp);
        if (user == null) {
            return;
        }
        String action = req.getParameter("action");
        if (action == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        Long customerId = new OnlineOrderDAO().resolveCustomerId(user.getUserId());
        if (customerId == null) {
            resp.sendRedirect(req.getContextPath() + "/home?err=profile");
            return;
        }

        switch (action) {
            case "add":
                handleAdd(req, resp, customerId);
                break;
            case "update":
                handleUpdate(req, resp, customerId);
                break;
            case "remove":
                handleRemove(req, resp, customerId);
                break;
            case "clear":
                handleClear(req, resp, customerId);
                break;
            default:
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                break;
        }
    }

    /* ==================== POST handlers ==================== */
    /**
     * Add `quantity` of one product — re-validates the online-sale rule against
     * the live DB (HOAT_DONG + KHONG_KE_DON + online_sale_allowed) and caps the
     * resulting cart quantity at live saleable stock. `back` carries the page
     * the button was pressed on so the redirect returns there.
     */
    private void handleAdd(HttpServletRequest req, HttpServletResponse resp, long customerId)
            throws IOException {
        String back = trim(req.getParameter("back"));
        long productId = parseId(req.getParameter("productId"));
        int qty = parseQty(req.getParameter("quantity"), 1);
        if (productId <= 0 || qty <= 0) {
            redirectBack(req, resp, back, "err", "badrequest");
            return;
        }

        CartDAO dao = new CartDAO();
        Product product = dao.findProductForCart(productId);
        if (product == null) {
            redirectBack(req, resp, back, "err", "notfound");
            return;
        }
        if (!"HOAT_DONG".equals(product.getStatus())) {
            redirectBack(req, resp, back, "err", "inactive");
            return;
        }
        if (product.getProductType() != null
                && "KE_DON".equals(product.getProductType().name())) {
            redirectBack(req, resp, back, "err", "rx");
            return;
        }
        if (product.getProductType() != null
                && "HAN_CHE".equals(product.getProductType().name())) {
            redirectBack(req, resp, back, "err", "restricted");
            return;
        }
        if (!Boolean.TRUE.equals(product.getOnlineSaleAllowed())) {
            redirectBack(req, resp, back, "err", "notonline");
            return;
        }

        Cart cart = dao.getOrCreateActiveCart(customerId);
        if (cart == null) {
            redirectBack(req, resp, back, "err", "dberror");
            return;
        }

        // Existing + requested must stay within what is sellable right now.
        int currentQty = 0;
        List<CartItem> items = dao.findItems(cart.getCartId());
        for (CartItem item : items) {
            if (item.getProductId() == productId) {
                currentQty = item.getQuantity();
            }
        }
        long saleable = 0;
        if (product.getAvailableQuantity() != null) {
            saleable = product.getAvailableQuantity();
        }
        if (currentQty + qty > saleable) {
            redirectBack(req, resp, back, "err", "stock");
            return;
        }

        dao.upsertItem(cart.getCartId(), productId, qty);
        redirectBack(req, resp, back, "ok", "added");
    }

    /** Set the exact quantity of one cart line; re-checked against live stock. */
    private void handleUpdate(HttpServletRequest req, HttpServletResponse resp, long customerId)
            throws IOException {
        long productId = parseId(req.getParameter("productId"));
        int qty = parseQty(req.getParameter("quantity"), 0);
        if (productId <= 0) {
            resp.sendRedirect(req.getContextPath() + "/cart?err=badrequest");
            return;
        }

        CartDAO dao = new CartDAO();
        Cart cart = dao.findActiveCart(customerId);
        if (cart == null) {
            resp.sendRedirect(req.getContextPath() + "/cart?err=notfound");
            return;
        }

        if (qty <= 0) {
            dao.removeItem(cart.getCartId(), productId);
            resp.sendRedirect(req.getContextPath() + "/cart?ok=removed");
            return;
        }

        long saleable = 0;
        Product product = dao.findProductForCart(productId);
        if (product != null && product.getAvailableQuantity() != null) {
            saleable = product.getAvailableQuantity();
        }
        if (qty > saleable) {
            resp.sendRedirect(req.getContextPath() + "/cart?err=stock");
            return;
        }

        dao.updateItemQuantity(cart.getCartId(), productId, qty);
        resp.sendRedirect(req.getContextPath() + "/cart?ok=updated");
    }

    /** Remove one line — scoped to the customer's own active cart. */
    private void handleRemove(HttpServletRequest req, HttpServletResponse resp, long customerId)
            throws IOException {
        long productId = parseId(req.getParameter("productId"));
        CartDAO dao = new CartDAO();
        Cart cart = dao.findActiveCart(customerId);
        if (cart != null && productId > 0) {
            dao.removeItem(cart.getCartId(), productId);
        }
        resp.sendRedirect(req.getContextPath() + "/cart?ok=removed");
    }

    /** Retire the whole cart — DANG_HOAT_DONG -> DA_XOA. */
    private void handleClear(HttpServletRequest req, HttpServletResponse resp, long customerId)
            throws IOException {
        CartDAO dao = new CartDAO();
        Cart cart = dao.findActiveCart(customerId);
        if (cart != null) {
            dao.clearCart(cart.getCartId());
        }
        resp.sendRedirect(req.getContextPath() + "/cart?ok=cleared");
    }

    /* ==================== helpers ==================== */
    /** KHACH_HANG-only gate. Returns null after redirect — callers return at once. */
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
            // Logged in but the wrong role — cart is a customer-only feature.
            resp.sendRedirect(req.getContextPath() + "/home?err=role");
            return null;
        }
        resp.sendRedirect(req.getContextPath() + "/authen?action=login");
        return null;
    }

    /**
     * PRG back to where the Add button was pressed — the card grid, the product
     * detail page, the home page, or the cart itself. Only known-relative
     * targets are allowed so `back` can never become an open redirect.
     */
    private void redirectBack(HttpServletRequest req, HttpServletResponse resp,
            String back, String kind, String code) throws IOException {
        String target = req.getContextPath() + "/cart";
        if ("detail".equals(back)) {
            long id = parseId(req.getParameter("productId"));
            if (id > 0) {
                target = req.getContextPath() + "/products/" + id;
            }
        } else if ("products".equals(back)) {
            target = req.getContextPath() + "/products";
        } else if ("home".equals(back)) {
            target = req.getContextPath() + "/home";
        }
        String sep = target.contains("?") ? "&" : "?";
        resp.sendRedirect(target + sep + kind + "=" + code);
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

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
