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
 * GET /staff — PHARMACIST landing page after login.
 * Minimal stub: requires authenticated PHARMACIST role.
 */
@WebServlet(name = "StaffServlet", urlPatterns = {"/staff"})
public class StaffServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        Object u = session == null ? null : session.getAttribute("currentUser");
        if (!(u instanceof User) || !"PHARMACIST".equals(((User) u).getRoleName())) {
            resp.sendRedirect(req.getContextPath() + "/authen?action=login");
            return;
        }
        req.getRequestDispatcher("/WEB-INF/views/staff/dashboard.jsp").forward(req, resp);
    }
}
