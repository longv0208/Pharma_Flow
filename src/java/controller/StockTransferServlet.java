package controller;

import dao.LocationDAO;
import dao.StockTransferDAO;
import dao.StockTransferDAO.TransferResult;
import model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet(name = "StockTransferServlet", urlPatterns = {"/inventory/transfers"})
public class StockTransferServlet extends HttpServlet {

    private static final String FORM_JSP = "/WEB-INF/views/admin/stock-transfer-form.jsp";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (requireAccess(req, resp) == null) {
            return;
        }
        req.setAttribute("zones", new LocationDAO().findZones(null, "HOAT_DONG"));
        req.getRequestDispatcher(FORM_JSP).forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        User user = requireAccess(req, resp);
        if (user == null) {
            return;
        }
        long batchId = parseId(req.getParameter("batchId"));
        long fromBinId = parseId(req.getParameter("fromBinId"));
        long toBinId = parseId(req.getParameter("toBinId"));
        int qty = (int) parseId(req.getParameter("quantity"));
        String reason = trim(req.getParameter("reason"));

        TransferResult result = new StockTransferDAO().transfer(
                batchId, fromBinId, toBinId, qty, user.getUserId(), reason);
        if (result.ok) {
            resp.sendRedirect(req.getContextPath() + "/inventory/transfers?ok=transferred");
        } else {
            resp.sendRedirect(req.getContextPath() + "/inventory/transfers?err=" + result.error);
        }
    }

    private User requireAccess(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession session = req.getSession(false);
        Object u = session != null ? session.getAttribute("currentUser") : null;
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
