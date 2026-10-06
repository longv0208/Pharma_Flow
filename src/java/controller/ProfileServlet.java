package controller;

import dao.CustomerProfileDAO;
import dao.UserDAO;
import model.CustomerProfile;
import model.User;
import util.PasswordUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * GET/POST /profile — customer self-service profile.
 *
 * Session-derived userId (never from request) — rule: no IDOR. Only CUSTOMER
 * role allowed; staff/admin redirected to their own areas.
 */
@WebServlet(name = "ProfileServlet", urlPatterns = {"/profile"})
public class ProfileServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = currentUser(req, resp);
        if (user == null) {
            return;
        }
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
        if (user == null) {
            return;
        }
        if (!"CUSTOMER".equals(user.getRoleName())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String action = trim(req.getParameter("action"));
        if ("change-password".equals(action)) {
            handleChangePassword(req, resp, user);
            return;
        }
        handleProfileUpdate(req, resp, user);
    }

    /* ============ POST handlers ============ */
    private void handleProfileUpdate(HttpServletRequest req, HttpServletResponse resp, User user)
            throws ServletException, IOException {
        String fullName = trim(req.getParameter("fullName"));
        String phone = trim(req.getParameter("phone"));
        String provinceCity = trim(req.getParameter("provinceCity"));
        String district = trim(req.getParameter("district"));
        String ward = trim(req.getParameter("ward"));
        String detailedAddress = trim(req.getParameter("detailedAddress"));

        Map<String, String> errors = new HashMap<>();
        if (fullName.isEmpty()) {
            errors.put("fullName", "This field is required.");
        } else if (fullName.length() > 150) {
            errors.put("fullName", "Must be at most 150 characters.");
        }
        if (phone.isEmpty()) {
            errors.put("phone", "This field is required.");
        } else if (phone.length() > 30) {
            errors.put("phone", "Must be at most 30 characters.");
        }

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
        if (session != null) {
            session.setAttribute("currentUser", user);
        }

        req.setAttribute("success", "Profile saved successfully.");
        req.setAttribute("profile", dao.findByUserId(user.getUserId()));
        req.getRequestDispatcher("/WEB-INF/views/customer/profile.jsp").forward(req, resp);
    }

    /**
     * POST /profile?action=change-password Verifies current password, validates
     * new one, updates hash.
     */
    private void handleChangePassword(HttpServletRequest req, HttpServletResponse resp, User user)
            throws ServletException, IOException {
        String current = req.getParameter("currentPassword");
        String password = req.getParameter("newPassword");
        String confirm = req.getParameter("confirmNewPassword");

        Map<String, String> errors = new HashMap<>();
        UserDAO dao = new UserDAO();
        User fresh = dao.findById(user.getUserId());

        if (current == null || fresh == null
                || !PasswordUtil.verify(current, fresh.getPasswordHash())) {
            errors.put("currentPassword", "Current password is incorrect.");
        }
        if (password == null || password.isEmpty()) {
            errors.put("newPassword", "This field is required.");
        } else if (password.length() < 6) {
            errors.put("newPassword", "Password must be at least 6 characters.");
        } else if (password.equals(current)) {
            errors.put("newPassword", "New password must differ from the current one.");
        }
        if (confirm == null || !confirm.equals(password)) {
            errors.put("confirmNewPassword", "Passwords do not match.");
        }

        CustomerProfileDAO profileDao = new CustomerProfileDAO();
        req.setAttribute("profile", profileDao.findByUserId(user.getUserId()));

        if (!errors.isEmpty()) {
            req.setAttribute("pwErrors", errors);
            req.setAttribute("showPwForm", true);
            req.getRequestDispatcher("/WEB-INF/views/customer/profile.jsp").forward(req, resp);
            return;
        }

        dao.updatePassword(user.getUserId(), PasswordUtil.hash(password));
        req.setAttribute("success", "Password changed successfully.");
        req.getRequestDispatcher("/WEB-INF/views/customer/profile.jsp").forward(req, resp);
    }

    /* ============ helpers ============ */
    private User currentUser(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        Object u = null;
        if (session != null) {
            u = session.getAttribute("currentUser");
        }
        if (!(u instanceof User)) {
            resp.sendRedirect(req.getContextPath() + "/authen?action=login");
            return null;
        }
        return (User) u;
    }

    private String targetFor(User u) {
        String role = u.getRoleName();
        if (role == null) {
            role = "CUSTOMER";
        }
        switch (role) {
            case "OWNER_ADMIN":
                return "/admin";
            case "STAFF":
                return "/inventory";
            case "SHIPPER":
                return "/home";
            default:
                return "/home";
        }
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
