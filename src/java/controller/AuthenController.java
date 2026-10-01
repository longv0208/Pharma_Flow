package controller;

import dao.UserDAO;
import model.User;
import util.PasswordUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Single controller for all auth concerns — per rule.md §22.
 *
 * URL: /authen
 *
 * GET  ?action=login     → render login form
 * GET  ?action=register  → render register form
 * GET  ?action=logout    → invalidate session, redirect /home
 * POST ?action=login     → authenticate, create session, role-based redirect
 * POST ?action=register  → validate + create CUSTOMER account, redirect login
 */
@WebServlet(name = "AuthenController", urlPatterns = {"/authen"})
public class AuthenController extends HttpServlet {

    private static final String SESSION_USER = "currentUser";
    private static final String SESSION_ROLE = "currentUserRole";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String action = req.getParameter("action");
        if (action == null) action = "login";
        switch (action) {
            case "login":    showLogin(req, resp);    break;
            case "register": showRegister(req, resp); break;
            case "logout":   doLogout(req, resp);     break;
            default:         showLogin(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String action = req.getParameter("action");
        if (action == null) action = "";
        switch (action) {
            case "login":    handleLogin(req, resp);    break;
            case "register": handleRegister(req, resp); break;
            default:
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown action");
        }
    }

    /* ============ GET handlers ============ */

    private void showLogin(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // If already logged in, route to the right area instead of showing the form.
        User u = currentUser(req);
        if (u != null) {
            resp.sendRedirect(req.getContextPath() + targetFor(u));
            return;
        }
        req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req, resp);
    }

    private void showRegister(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/views/auth/register.jsp").forward(req, resp);
    }

    private void doLogout(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) session.invalidate();
        resp.sendRedirect(req.getContextPath() + "/home");
    }

    /* ============ POST handlers ============ */

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String identifier = trim(req.getParameter("identifier"));
        String password   = req.getParameter("password");

        if (identifier.isEmpty() || password == null || password.isEmpty()) {
            req.setAttribute("error", "Identifier and password are required.");
            req.setAttribute("identifierValue", identifier);
            req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req, resp);
            return;
        }

        UserDAO dao = new UserDAO();
        User user = dao.findByIdentifier(identifier);
        if (user == null || !PasswordUtil.verify(password, user.getPasswordHash())) {
            req.setAttribute("error", "Incorrect username/email or password.");
            req.setAttribute("identifierValue", identifier);
            req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req, resp);
            return;
        }
        if (!user.isActive()) {
            req.setAttribute("error", "This account is inactive. Please contact support.");
            req.setAttribute("identifierValue", identifier);
            req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req, resp);
            return;
        }

        HttpSession session = req.getSession(true);
        session.setAttribute(SESSION_USER, user);
        session.setAttribute(SESSION_ROLE, user.getRoleName());
        resp.sendRedirect(req.getContextPath() + targetFor(user));
    }

    private void handleRegister(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String fullName = trim(req.getParameter("fullName"));
        String email    = trim(req.getParameter("email"));
        String phone    = trim(req.getParameter("phone"));
        String username = trim(req.getParameter("username"));
        String password = req.getParameter("password");
        String confirm  = req.getParameter("confirmPassword");

        // Field-level errors keyed by input name for JSP rendering.
        java.util.Map<String, String> errors = new java.util.HashMap<>();
        if (fullName.isEmpty())  errors.put("fullName", "This field is required.");
        if (email.isEmpty())     errors.put("email", "This field is required.");
        else if (!email.contains("@")) errors.put("email", "Invalid email format.");
        if (username.isEmpty())  errors.put("username", "This field is required.");
        if (password == null || password.isEmpty())
                                 errors.put("password", "This field is required.");
        else if (password.length() < 6)
                                 errors.put("password", "Password must be at least 6 characters.");
        if (confirm == null || !confirm.equals(password))
                                 errors.put("confirmPassword", "Passwords do not match.");
        if (phone.isEmpty())     errors.put("phone", "This field is required.");

        UserDAO dao = new UserDAO();
        if (errors.isEmpty()) {
            if (dao.existsByEmail(email))    errors.put("email", "This email is already registered.");
            if (dao.existsByUsername(username)) errors.put("username", "This username is already taken.");
        }

        if (!errors.isEmpty()) {
            req.setAttribute("errors", errors);
            req.setAttribute("fullNameValue", fullName);
            req.setAttribute("emailValue", email);
            req.setAttribute("phoneValue", phone);
            req.setAttribute("usernameValue", username);
            req.getRequestDispatcher("/WEB-INF/views/auth/register.jsp").forward(req, resp);
            return;
        }

        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setUsername(username);
        user.setPhone(phone);
        user.setPasswordHash(PasswordUtil.hash(password));
        // roleId set to CUSTOMER inside DAO — never from request

        long newId = dao.registerCustomer(user);
        if (newId <= 0) {
            req.setAttribute("error", "Registration failed. Please try again.");
            req.setAttribute("fullNameValue", fullName);
            req.setAttribute("emailValue", email);
            req.setAttribute("phoneValue", phone);
            req.setAttribute("usernameValue", username);
            req.getRequestDispatcher("/WEB-INF/views/auth/register.jsp").forward(req, resp);
            return;
        }

        req.setAttribute("success", "Account created. Please sign in.");
        req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req, resp);
    }

    /* ============ helpers ============ */

    private User currentUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return null;
        Object u = session.getAttribute(SESSION_USER);
        return (u instanceof User) ? (User) u : null;
    }

    /** Role → landing path per SRS UC02. */
    private String targetFor(User u) {
        String role = u.getRoleName() == null ? "CUSTOMER" : u.getRoleName();
        switch (role) {
            case "OWNER_ADMIN":  return "/admin";
            case "PHARMACIST":   return "/staff";
            case "SALES_STAFF":  return "/pos";
            case "CUSTOMER":
            default:             return "/home";
        }
    }

    private static String trim(String s) { return s == null ? "" : s.trim(); }
}
