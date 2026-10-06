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
 * GET /pos — STAFF landing stub for the POS area. POS itself is a later phase;
 * for now any STAFF session may open the placeholder page.
 */
@WebServlet(name = "PosServlet", urlPatterns = {"/pos"})
public class PosServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        Object u = null;
        if (session != null) {
            u = session.getAttribute("currentUser");
        }
        if (!(u instanceof User) || !"STAFF".equals(((User) u).getRoleName())) {
            resp.sendRedirect(req.getContextPath() + "/authen?action=login");
            return;
        }
        req.getRequestDispatcher("/WEB-INF/views/pos/dashboard.jsp").forward(req, resp);
    }
}
