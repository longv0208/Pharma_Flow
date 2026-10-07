package controller;

import dao.DeliveryDAO;
import dao.DeliveryDAO.DeliveryResult;
import model.InventoryReservation;
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
import java.util.List;

/**
 * /delivery — the NHAN_VIEN_GIAO_HANG shared delivery queue.
 *
 * GET  actions: (default) queue list — only the three delivery-relevant
 *               statuses (SAN_SANG / DANG_GIAO / HOAN_TAT), paginated +
 *               filtered by status and one search box (order id / recipient
 *               name / phone); detail — one order's delivery view (?id=).
 * POST actions: start (SAN_SANG -> DANG_GIAO with stock-out), complete
 *               (DANG_GIAO -> HOAN_TAT).
 *
 * NHAN_VIEN_GIAO_HANG only — KHACH_HANG, NHAN_VIEN and CHU_QUAN_QUAN_TRI are
 * redirected away; guests go to login. Actor identity is always
 * sessionScope.currentUser.getUserId() — never a request parameter — so the
 * BAN_ONLINE movements written at dispatch carry the real shipper id.
 *
 * There is deliberately no assignment/ownership: the queue is shared and the
 * first transaction to lock the order row wins. The transition set is closed
 * and status-validated in the DAO under that lock — a second shipper's click
 * reads the new state and fails INVALID_STATUS. PRG after every POST so a
 * refresh never replays a transition.
 */
@WebServlet(name = "DeliveryServlet", urlPatterns = {"/delivery"})
public class DeliveryServlet extends HttpServlet {

    private static final String LIST_JSP = "/WEB-INF/views/shipper/delivery-list.jsp";
    private static final String DETAIL_JSP = "/WEB-INF/views/shipper/delivery-detail.jsp";
    private static final int PAGE_SIZE = 15;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = requireShipper(req, resp);
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
        User user = requireShipper(req, resp);
        if (user == null) {
            return;
        }
        String action = req.getParameter("action");
        if (action == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        switch (action) {
            case "start":
            case "complete":
                handleTransition(req, resp, user, action);
                break;
            default:
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                break;
        }
    }

    /* ==================== GET handlers ==================== */
    /**
     * The shared delivery queue — only SAN_SANG / DANG_GIAO / HOAN_TAT are
     * ever shown or filterable; anything else in ?status= is ignored. Work
     * order inside the DAO: on the road first, then ready, then history.
     */
    private void handleList(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String status = whitelistStatus(req.getParameter("status"));
        String search = trim(req.getParameter("q"));

        DeliveryDAO dao = new DeliveryDAO();
        int total = dao.countDeliveryOrders(status, search);
        int totalPages = (int) Math.ceil((double) total / PAGE_SIZE);
        if (totalPages < 1) {
            totalPages = 1;
        }
        int page = parsePage(req.getParameter("page"));
        if (page > totalPages) {
            page = totalPages;
        }
        int offset = (page - 1) * PAGE_SIZE;

        List<OnlineOrder> orders = dao.findDeliveryOrders(status, search, offset, PAGE_SIZE);
        req.setAttribute("orders", orders);
        req.setAttribute("status", status);
        req.setAttribute("q", search);
        req.setAttribute("page", page);
        req.setAttribute("totalPages", totalPages);
        req.setAttribute("total", total);
        req.getRequestDispatcher(LIST_JSP).forward(req, resp);
    }

    /**
     * Delivery detail — order header, recipient, items and the reserved
     * batches (read-only). Orders outside the delivery states are not the
     * shipper's business and bounce back to the queue.
     */
    private void handleDetail(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long orderId = parseId(req.getParameter("id"));
        if (orderId <= 0) {
            resp.sendRedirect(req.getContextPath() + "/delivery?err=ORDER_NOT_FOUND");
            return;
        }

        DeliveryDAO dao = new DeliveryDAO();
        OnlineOrder order = dao.findOrderById(orderId);
        if (order == null || !isDeliveryStatus(order.getOrderStatus())) {
            resp.sendRedirect(req.getContextPath() + "/delivery?err=ORDER_NOT_FOUND");
            return;
        }
        List<OnlineOrderItem> items = dao.findOrderItems(orderId);
        List<InventoryReservation> reservations = dao.findOrderReservations(orderId);

        req.setAttribute("order", order);
        req.setAttribute("items", items);
        req.setAttribute("reservations", reservations);
        req.getRequestDispatcher(DETAIL_JSP).forward(req, resp);
    }

    /* ==================== POST handlers ==================== */
    /**
     * Shared handler for the two transitions — the action name selects which
     * DAO method runs; the DAO enforces the from-state under a row lock. The
     * only request input is orderId; actor + states come from the
     * session/DB. PRG back to the detail page with ok=/err=.
     */
    private void handleTransition(HttpServletRequest req, HttpServletResponse resp,
            User user, String action) throws IOException {
        long orderId = parseId(req.getParameter("orderId"));
        if (orderId <= 0) {
            resp.sendRedirect(req.getContextPath() + "/delivery?err=ORDER_NOT_FOUND");
            return;
        }

        DeliveryDAO dao = new DeliveryDAO();
        DeliveryResult result;
        String ok;
        if ("start".equals(action)) {
            result = dao.startDelivery(orderId, user.getUserId());
            ok = "started";
        } else {
            result = dao.completeDelivery(orderId);
            ok = "completed";
        }

        if (result.ok) {
            resp.sendRedirect(req.getContextPath()
                    + "/delivery?action=detail&id=" + orderId + "&ok=" + ok);
            return;
        }
        resp.sendRedirect(req.getContextPath()
                + "/delivery?action=detail&id=" + orderId + "&err=" + result.error);
    }

    /* ==================== helpers ==================== */
    /** NHAN_VIEN_GIAO_HANG-only gate. Returns null after redirect — callers return at once. */
    private User requireShipper(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession session = req.getSession(false);
        Object u = null;
        if (session != null) {
            u = session.getAttribute("currentUser");
        }
        if (u instanceof User) {
            User user = (User) u;
            if ("NHAN_VIEN_GIAO_HANG".equals(user.getRoleName())) {
                return user;
            }
            resp.sendRedirect(req.getContextPath() + "/home?err=role");
            return null;
        }
        resp.sendRedirect(req.getContextPath() + "/authen?action=login");
        return null;
    }

    /** Only the three delivery states may be filtered — anything else -> no filter. */
    private static String whitelistStatus(String s) {
        String v = trim(s);
        if (isDeliveryStatus(v)) {
            return v;
        }
        return "";
    }

    private static boolean isDeliveryStatus(String v) {
        return "SAN_SANG".equals(v) || "DANG_GIAO".equals(v) || "HOAN_TAT".equals(v);
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
