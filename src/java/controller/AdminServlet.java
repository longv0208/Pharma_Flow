package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.User;
import java.io.IOException;

/**
 * GET /admin — OWNER_ADMIN landing page after login.
 * Minimal stub: redirects to /home when not authenticated as admin.
 * Real admin screens will be built in later phases.
 */
@WebServlet(name = "AdminServlet", urlPatterns = {"/admin"})
public class AdminServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        Object u = session == null ? null : session.getAttribute("currentUser");
        if (!(u instanceof User) || !"OWNER_ADMIN".equals(((User) u).getRoleName())) {
            resp.sendRedirect(req.getContextPath() + "/authen?action=login");
            return;
        }
        req.getRequestDispatcher("/WEB-INF/views/admin/dashboard.jsp").forward(req, resp);
    }
}
