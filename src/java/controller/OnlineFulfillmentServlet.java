package controller;

import dao.OnlineFulfillmentDAO;
import dao.OnlineFulfillmentDAO.FulfillmentResult;
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
 * /fulfillment — the NHAN_VIEN online-order preparation desk.
 *
 * GET  actions: (default) order list — all statuses, paginated + filtered by
 *               status and one search box (order id / customer name / phone);
 *               detail — one order's picking view (?id=).
 * POST actions: confirm, prepare, ready, reject.
 *
 * NHAN_VIEN only — this is a staff tool; KHACH_HANG, NHAN_VIEN_GIAO_HANG and
 * CHU_QUAN_QUAN_TRI are redirected away. Actor identity is always
 * sessionScope.currentUser.getUserId() — never a request parameter — so the
 * GIAI_PHONG_GIU_HANG movements on reject carry the real staff id.
 *
 * The transition set is closed and status-validated in the DAO under a row
 * lock: confirm CHO_XU_LY->DA_XAC_NHAN, prepare DA_XAC_NHAN->DANG_CHUAN_BI,
 * ready DANG_CHUAN_BI->SAN_SANG, reject CHO_XU_LY->TU_CHOI. PRG after every
 * POST so a refresh never replays a transition.
 */
@WebServlet(name = "OnlineFulfillmentServlet", urlPatterns = {"/fulfillment"})
public class OnlineFulfillmentServlet extends HttpServlet {

    private static final String LIST_JSP = "/WEB-INF/views/staff/online-order-list.jsp";
    private static final String DETAIL_JSP = "/WEB-INF/views/staff/online-order-detail.jsp";
    private static final int PAGE_SIZE = 15;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = requireStaff(req, resp);
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
            case "confirm":
            case "prepare":
            case "ready":
            case "reject":
                handleTransition(req, resp, user, action);
                break;
            default:
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                break;
        }
    }

    /* ==================== GET handlers ==================== */
    /**
     * Staff order list — all statuses for visibility, paginated, filterable by
     * status dropdown and one search box (order id / name / phone).
     */
    private void handleList(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String status = trim(req.getParameter("status"));
        String search = trim(req.getParameter("q"));

        OnlineFulfillmentDAO dao = new OnlineFulfillmentDAO();
        int total = dao.countOrders(status, search);
        int totalPages = (int) Math.ceil((double) total / PAGE_SIZE);
        if (totalPages < 1) {
            totalPages = 1;
        }
        int page = parsePage(req.getParameter("page"));
        if (page > totalPages) {
            page = totalPages;
        }
        int offset = (page - 1) * PAGE_SIZE;

        List<OnlineOrder> orders = dao.findOrders(status, search, offset, PAGE_SIZE);
        req.setAttribute("orders", orders);
        req.setAttribute("status", status);
        req.setAttribute("q", search);
        req.setAttribute("page", page);
        req.setAttribute("totalPages", totalPages);
        req.setAttribute("total", total);
        req.getRequestDispatcher(LIST_JSP).forward(req, resp);
    }

    /**
     * Picking detail — order header, items, and the DANG_GIU batch
     * reservations staff physically picks from. Reservations are the source
     * of truth (the customer's FEFO allocation), never re-run.
     */
    private void handleDetail(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long orderId = parseId(req.getParameter("id"));
        if (orderId <= 0) {
            resp.sendRedirect(req.getContextPath() + "/fulfillment?err=ORDER_NOT_FOUND");
            return;
        }

        OnlineFulfillmentDAO dao = new OnlineFulfillmentDAO();
        OnlineOrder order = dao.findOrderById(orderId);
        if (order == null) {
            resp.sendRedirect(req.getContextPath() + "/fulfillment?err=ORDER_NOT_FOUND");
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
     * Shared handler for the four transitions — the action name selects which
     * DAO method runs; the DAO enforces the from-state under a row lock. The
     * only request input is orderId; actor + states come from the DB/session.
     * PRG back to the detail page with ok=/err=.
     */
    private void handleTransition(HttpServletRequest req, HttpServletResponse resp,
            User user, String action) throws IOException {
        long orderId = parseId(req.getParameter("orderId"));
        if (orderId <= 0) {
            resp.sendRedirect(req.getContextPath() + "/fulfillment?err=ORDER_NOT_FOUND");
            return;
        }

        OnlineFulfillmentDAO dao = new OnlineFulfillmentDAO();
        FulfillmentResult result;
        String ok;
        if ("confirm".equals(action)) {
            result = dao.confirmOrder(orderId);
            ok = "confirmed";
        } else if ("prepare".equals(action)) {
            result = dao.prepareOrder(orderId);
            ok = "prepared";
        } else if ("ready".equals(action)) {
            result = dao.readyOrder(orderId);
            ok = "ready";
        } else {
            // reject — actor id goes in so movements record the staff user
            result = dao.rejectOrder(orderId, user.getUserId());
            ok = "rejected";
        }

        if (result.ok) {
            resp.sendRedirect(req.getContextPath()
                    + "/fulfillment?action=detail&id=" + orderId + "&ok=" + ok);
            return;
        }
        resp.sendRedirect(req.getContextPath()
                + "/fulfillment?action=detail&id=" + orderId + "&err=" + result.error);
    }

    /* ==================== helpers ==================== */
    /** NHAN_VIEN-only gate. Returns null after redirect — callers return at once. */
    private User requireStaff(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession session = req.getSession(false);
        Object u = null;
        if (session != null) {
            u = session.getAttribute("currentUser");
        }
        if (u instanceof User) {
            User user = (User) u;
            if ("NHAN_VIEN".equals(user.getRoleName())) {
                return user;
            }
            resp.sendRedirect(req.getContextPath() + "/home?err=role");
            return null;
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
