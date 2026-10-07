package controller;

import dao.InventoryAlertDAO;
import dao.ReportDAO;
import dao.ReportDAO.DailySalesRow;
import dao.ReportDAO.InventorySummary;
import dao.ReportDAO.OrderStatusSummary;
import dao.ReportDAO.SalesSummary;
import dao.ReportDAO.TopProductRow;
import model.InventoryAlertSetting;
import model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Date;
import java.util.Calendar;
import java.util.List;

/**
 * /reports — CHU_QUAN_QUAN_TRI operational reporting. GET only.
 *
 * One page split into five sections:
 *   1. Sales summary cards + POS/online share for the selected period
 *   2. Daily revenue table (POS + online side by side)
 *   3. Top 10 best-selling products for the period
 *   4. Current online-order workflow counters (all-time, no date filter)
 *   5. Current inventory snapshot + alert summary (all-time, no date filter)
 *
 * Optional ?from=yyyy-MM-dd&to=yyyy-MM-dd scope sections 1-3 only — workflow
 * counters, inventory and alerts are current-state by definition. Invalid or
 * inverted dates silently fall back to the current month.
 *
 * Revenue definitions (spec §7-9):
 *   POS    = sale_transactions HOAN_TAT by sale_datetime
 *   Online = online_orders HOAN_TAT by updated_at (schema has no
 *            completed_at and HOAN_TAT is terminal, so updated_at is the
 *            completion timestamp)
 * Never double-counts inventory_movements — those are audit rows, not sales.
 *
 * Read-only: no POST handler, no writes anywhere. Authorization is the same
 * session check AdminServlet uses — anything but CHU_QUAN_QUAN_TRI is
 * redirected away before a single query runs.
 */
@WebServlet(name = "ReportServlet", urlPatterns = {"/reports"})
public class ReportServlet extends HttpServlet {

    private static final String JSP = "/WEB-INF/views/admin/reports.jsp";
    private static final int TOP_LIMIT = 10;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!requireAdmin(req, resp)) {
            return;
        }

        Date[] range = resolveRange(req.getParameter("from"), req.getParameter("to"));
        Date from = range[0];
        Date to = range[1];

        ReportDAO reportDao = new ReportDAO();
        SalesSummary sales = reportDao.findSalesSummary(from, to);
        List<DailySalesRow> daily = reportDao.findDailySales(from, to);
        List<TopProductRow> topProducts = reportDao.findTopProducts(from, to, TOP_LIMIT);
        OrderStatusSummary orderStatus = reportDao.findOrderStatusSummary();

        InventoryAlertDAO alertDao = new InventoryAlertDAO();
        InventoryAlertSetting settings = alertDao.getEffectiveSettings();
        InventorySummary inventory = reportDao.findInventorySummary(
                settings.getNearExpiryWarningDays());
        int[] alertCounts = alertDao.getAlertSummary(
                settings.getMinimumStockLevel(), settings.getNearExpiryWarningDays());

        req.setAttribute("from", from);
        req.setAttribute("to", to);
        req.setAttribute("sales", sales);
        req.setAttribute("daily", daily);
        req.setAttribute("topProducts", topProducts);
        req.setAttribute("orderStatus", orderStatus);
        req.setAttribute("inventory", inventory);
        req.setAttribute("alertCounts", alertCounts);
        req.getRequestDispatcher(JSP).forward(req, resp);
    }

    /* ==================== helpers ==================== */
    /**
     * Parse ?from/?to as SQL dates. Defaults: first day of the current month
     * through today. Any parse failure or from > to resets the pair to that
     * default — the report always renders a usable period.
     */
    private Date[] resolveRange(String fromParam, String toParam) {
        Date today = new Date(System.currentTimeMillis());
        Calendar cal = Calendar.getInstance();
        cal.setTime(today);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        Date firstOfMonth = new Date(cal.getTimeInMillis());

        Date from = parseDate(fromParam);
        Date to = parseDate(toParam);
        if (from == null) {
            from = firstOfMonth;
        }
        if (to == null) {
            to = today;
        }
        if (from.after(to)) {
            from = firstOfMonth;
            to = today;
        }
        return new Date[]{from, to};
    }

    private static Date parseDate(String s) {
        if (s == null) {
            return null;
        }
        try {
            return Date.valueOf(s.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** CHU_QUAN_QUAN_TRI-only gate. Returns false after redirect. */
    private boolean requireAdmin(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession session = req.getSession(false);
        Object u = null;
        if (session != null) {
            u = session.getAttribute("currentUser");
        }
        if (u instanceof User && "CHU_QUAN_QUAN_TRI".equals(((User) u).getRoleName())) {
            return true;
        }
        resp.sendRedirect(req.getContextPath() + "/authen?action=login");
        return false;
    }
}
