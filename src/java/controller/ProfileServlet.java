package controller;

import dao.CustomerProfileDAO;
import model.CustomerProfile;
import model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

/**
 * GET/POST /profile — customer self-service profile.
 *
 * Session-derived userId (never from request) — rule: no IDOR.
 * Only CUSTOMER role allowed; staff/admin redirected to their own areas.
 */
@WebServlet(name = "ProfileServlet", urlPatterns = {"/profile"})
public class ProfileServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = currentUser(req, resp);
        if (user == null) return;
        if (!"CUSTOMER".equals(user.getRoleName())) {
            resp.sendRedirect(req.getContextPath() + targetFor(user));
            return;
        }

        CustomerProfileDAO dao = new CustomerProfileDAO();
        CustomerProfile profile = dao.findByUserId(user.getUserId());
        req.setAttribute("profile", profile);
        req.getRequestDispatcher("/WEB-INF/views/customer/profile.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = currentUser(req, resp);
        if (user == null) return;
        if (!"CUSTOMER".equals(user.getRoleName())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String fullName        = trim(req.getParameter("fullName"));
        String phone           = trim(req.getParameter("phone"));
        String provinceCity    = trim(req.getParameter("provinceCity"));
        String district        = trim(req.getParameter("district"));
        String ward            = trim(req.getParameter("ward"));
        String detailedAddress = trim(req.getParameter("detailedAddress"));

        java.util.Map<String, String> errors = new java.util.HashMap<>();
        if (fullName.isEmpty()) errors.put("fullName", "This field is required.");
        if (phone.isEmpty())    errors.put("phone", "This field is required.");

        if (!errors.isEmpty()) {
            req.setAttribute("errors", errors);
            CustomerProfileDAO dao = new CustomerProfileDAO();
            req.setAttribute("profile", dao.findByUserId(user.getUserId()));
            req.getRequestDispatcher("/WEB-INF/views/customer/profile.jsp").forward(req, resp);
            return;
        }

        CustomerProfileDAO dao = new CustomerProfileDAO();
        dao.updateUserContactByUserId(user.getUserId(), fullName, phone);
        dao.updateAddressByUserId(user.getUserId(), provinceCity, district, ward, detailedAddress);

        // Refresh session user so header shows new name.
        user.setFullName(fullName);
        user.setPhone(phone);
        HttpSession session = req.getSession(false);
        if (session != null) session.setAttribute("currentUser", user);

        req.setAttribute("success", "Profile saved successfully.");
        req.setAttribute("profile", dao.findByUserId(user.getUserId()));
        req.getRequestDispatcher("/WEB-INF/views/customer/profile.jsp").forward(req, resp);
    }

    /* ============ helpers ============ */

    private User currentUser(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        Object u = session == null ? null : session.getAttribute("currentUser");
        if (!(u instanceof User)) {
            resp.sendRedirect(req.getContextPath() + "/authen?action=login");
            return null;
        }
        return (User) u;
    }

    private String targetFor(User u) {
        String role = u.getRoleName() == null ? "CUSTOMER" : u.getRoleName();
        switch (role) {
            case "OWNER_ADMIN":  return "/admin";
            case "PHARMACIST":   return "/staff";
            case "SALES_STAFF":  return "/pos";
            default:             return "/home";
        }
    }

    private static String trim(String s) { return s == null ? "" : s.trim(); }
}
