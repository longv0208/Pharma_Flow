package controller;

import dao.UserDAO;
import dao.VerificationTokenDAO;
import model.User;
import util.EmailSender;
import util.PasswordUtil;
import util.TokenUtil;
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
 * Single controller for all auth concerns — per rule.md §22.
 *
 * URL: /authen
 *
 * GET ?action=login → render login form GET ?action=register → render register
 * form GET ?action=verify-email → render OTP form (email from session/params)
 * GET ?action=forgot-password → render email request form GET
 * ?action=reset-password → render new-password form (needs valid OTP in
 * session) GET ?action=logout → invalidate session, redirect /home
 *
 * POST ?action=login → authenticate, create session, role-based redirect POST
 * ?action=register → validate, create INACTIVE user, send OTP POST
 * ?action=verify-email → check OTP, activate account POST ?action=resend-code →
 * issue a fresh OTP for the pending email POST ?action=forgot-password → email
 * exists → send OTP, go to OTP step POST ?action=reset-password → verify OTP,
 * update password
 */
@WebServlet(name = "AuthenController", urlPatterns = {"/authen"})
public class AuthenController extends HttpServlet {

    private static final String SESSION_USER = "currentUser";
    private static final String SESSION_ROLE = "currentUserRole";

    /**
     * Session keys for pending flows — cleared on success.
     */
    private static final String S_VERIFY_USER = "pendingVerifyUserId";
    private static final String S_VERIFY_EMAIL = "pendingVerifyEmail";
    private static final String S_RESET_USER = "pendingResetUserId";
    private static final String S_RESET_EMAIL = "pendingResetEmail";
    private static final String S_RESET_OK = "resetOtpVerified";
    private static final String S_RESET_HASH = "resetOtpHash";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String action = req.getParameter("action");
        if (action == null) {
            action = "login";
        }
        switch (action) {
            case "login":
                showLogin(req, resp);
                break;
            case "register":
                showRegister(req, resp);
                break;
            case "verify-email":
                showVerifyEmail(req, resp);
                break;
            case "forgot-password":
                showForgotPassword(req, resp);
                break;
            case "reset-password":
                showResetPassword(req, resp);
                break;
            case "logout":
                doLogout(req, resp);
                break;
            default:
                showLogin(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String action = req.getParameter("action");
        if (action == null) {
            action = "";
        }
        switch (action) {
            case "login":
                handleLogin(req, resp);
                break;
            case "register":
                handleRegister(req, resp);
                break;
            case "verify-email":
                handleVerifyEmail(req, resp);
                break;
            case "resend-code":
                handleResendCode(req, resp);
                break;
            case "forgot-password":
                handleForgotPassword(req, resp);
                break;
            case "reset-password":
                handleResetPassword(req, resp);
                break;
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

    private void showVerifyEmail(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession s = req.getSession(false);
        if (s == null || s.getAttribute(S_VERIFY_USER) == null) {
            resp.sendRedirect(req.getContextPath() + "/authen?action=login");
            return;
        }
        req.getRequestDispatcher("/WEB-INF/views/auth/verify-email.jsp").forward(req, resp);
    }

    private void showForgotPassword(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/views/auth/forgot-password.jsp").forward(req, resp);
    }

    private void showResetPassword(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession s = req.getSession(false);
        boolean verified = false;
        if (s != null && Boolean.TRUE.equals(s.getAttribute(S_RESET_OK))) {
            verified = true;
        }
        if (!verified) {
            resp.sendRedirect(req.getContextPath() + "/authen?action=forgot-password");
            return;
        }
        req.getRequestDispatcher("/WEB-INF/views/auth/reset-password.jsp").forward(req, resp);
    }

    private void doLogout(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        resp.sendRedirect(req.getContextPath() + "/home");
    }

    /* ============ POST handlers ============ */
    private void handleLogin(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String identifier = trim(req.getParameter("identifier"));
        String password = req.getParameter("password");

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
            // INACTIVE accounts may be pending email verification — send them there.
            HttpSession s = req.getSession(true);
            s.setAttribute(S_VERIFY_USER, user.getUserId());
            s.setAttribute(S_VERIFY_EMAIL, user.getEmail());
            resp.sendRedirect(req.getContextPath() + "/authen?action=verify-email&pending=1");
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
        String email = trim(req.getParameter("email"));
        String phone = trim(req.getParameter("phone"));
        String username = trim(req.getParameter("username"));
        String password = req.getParameter("password");
        String confirm = req.getParameter("confirmPassword");

        // Field-level errors keyed by input name for JSP rendering.
        Map<String, String> errors = new HashMap<>();
        if (fullName.isEmpty()) {
            errors.put("fullName", "This field is required.");
        } else if (fullName.length() > 150) {
            errors.put("fullName", "Must be at most 150 characters.");
        }
        if (email.isEmpty()) {
            errors.put("email", "This field is required.");
        } else if (email.length() > 150) {
            errors.put("email", "Must be at most 150 characters.");
        } else if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            errors.put("email", "Invalid email format.");
        }
        if (username.isEmpty()) {
            errors.put("username", "This field is required.");
        } else if (username.length() > 100) {
            errors.put("username", "Must be at most 100 characters.");
        }
        if (password == null || password.isEmpty()) {
            errors.put("password", "This field is required.");
        } else if (password.length() < 6) {
            errors.put("password", "Password must be at least 6 characters.");
        }
        if (confirm == null || !confirm.equals(password)) {
            errors.put("confirmPassword", "Passwords do not match.");
        }
        if (phone.isEmpty()) {
            errors.put("phone", "This field is required.");
        } else if (phone.length() > 30) {
            errors.put("phone", "Must be at most 30 characters.");
        }

        UserDAO dao = new UserDAO();
        if (errors.isEmpty()) {
            if (dao.existsByEmail(email)) {
                errors.put("email", "This email is already registered.");
            } else if (dao.existsByUsername(username)) {
                errors.put("username", "This username is already taken.");
            }
        }

        if (!errors.isEmpty()) {
            backToRegister(req, resp, errors, fullName, email, phone, username);
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
            backToRegister(req, resp, null, fullName, email, phone, username);
            return;
        }

        // Issue a 6-digit OTP and mail it — account stays INACTIVE until confirmed.
        String code = TokenUtil.generateCode();
        VerificationTokenDAO vt = new VerificationTokenDAO();
        vt.invalidatePrevious(newId, "VERIFY_EMAIL");
        vt.insert(newId, "VERIFY_EMAIL", TokenUtil.hash(code));
        EmailSender.sendVerificationEmail(email, code, fullName);

        HttpSession s = req.getSession(true);
        s.setAttribute(S_VERIFY_USER, newId);
        s.setAttribute(S_VERIFY_EMAIL, email);
        resp.sendRedirect(req.getContextPath() + "/authen?action=verify-email&sent=1");
    }

    private void handleVerifyEmail(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession s = req.getSession(false);
        if (s == null || s.getAttribute(S_VERIFY_USER) == null) {
            resp.sendRedirect(req.getContextPath() + "/authen?action=login");
            return;
        }
        long userId = (Long) s.getAttribute(S_VERIFY_USER);
        String code = trim(req.getParameter("code"));

        if (code.isEmpty()) {
            req.setAttribute("error", "Please enter the 6-digit code.");
            req.getRequestDispatcher("/WEB-INF/views/auth/verify-email.jsp").forward(req, resp);
            return;
        }

        VerificationTokenDAO vt = new VerificationTokenDAO();
        if (!vt.consume(userId, "VERIFY_EMAIL", TokenUtil.hash(code))) {
            req.setAttribute("error", "Invalid or expired code. Check the latest email or resend.");
            req.getRequestDispatcher("/WEB-INF/views/auth/verify-email.jsp").forward(req, resp);
            return;
        }

        new UserDAO().activateUser(userId);
        s.removeAttribute(S_VERIFY_USER);
        s.removeAttribute(S_VERIFY_EMAIL);

        req.setAttribute("success", "Email verified — you can sign in now.");
        req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req, resp);
    }

    /**
     * POST ?action=resend-code — shared by the verify-email and reset-otp
     * pages. Which flow we are in is decided by which session key exists.
     */
    private void handleResendCode(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession s = req.getSession(false);

        // Decide which pending flow is asking for a new code.
        boolean isVerifyFlow = false;
        boolean isResetFlow = false;
        if (s != null) {
            isVerifyFlow = s.getAttribute(S_VERIFY_USER) != null;
            isResetFlow = s.getAttribute(S_RESET_USER) != null;
        }
        if (!isVerifyFlow && !isResetFlow) {
            resp.sendRedirect(req.getContextPath() + "/authen?action=login");
            return;
        }

        long userId;
        String type;
        String jsp;
        if (isVerifyFlow) {
            userId = (Long) s.getAttribute(S_VERIFY_USER);
            type = "VERIFY_EMAIL";
            jsp = "verify-email.jsp";
        } else {
            userId = (Long) s.getAttribute(S_RESET_USER);
            type = "RESET_PASSWORD";
            jsp = "reset-otp.jsp";
        }

        // Enforce the 60s cooldown between two sends.
        VerificationTokenDAO vt = new VerificationTokenDAO();
        long cooldownLeft = vt.resendCooldownLeft(userId, type);
        if (cooldownLeft > 0) {
            req.setAttribute("error", "Please wait " + cooldownLeft + "s before requesting a new code.");
            req.setAttribute("resendCooldown", cooldownLeft);
            req.getRequestDispatcher("/WEB-INF/views/auth/" + jsp).forward(req, resp);
            return;
        }

        // Issue a fresh code: invalidate the old one, insert + mail the new one.
        User u = new UserDAO().findById(userId);
        if (u != null) {
            String code = TokenUtil.generateCode();
            vt.invalidatePrevious(userId, type);
            vt.insert(userId, type, TokenUtil.hash(code));
            if (isVerifyFlow) {
                EmailSender.sendVerificationEmail(u.getEmail(), code, u.getFullName());
            } else {
                EmailSender.sendResetPasswordEmail(u.getEmail(), code, u.getFullName());
            }
        }
        req.setAttribute("success", "A new code was sent to your email.");
        req.setAttribute("resendCooldown", 60L);
        req.getRequestDispatcher("/WEB-INF/views/auth/" + jsp).forward(req, resp);
    }

    private void handleForgotPassword(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String email = trim(req.getParameter("email"));

        if (email.isEmpty()) {
            req.setAttribute("error", "Please enter your account email.");
            req.getRequestDispatcher("/WEB-INF/views/auth/forgot-password.jsp").forward(req, resp);
            return;
        }

        UserDAO dao = new UserDAO();
        User user = dao.findByEmail(email);
        if (user == null) {
            // Deliberately vague — don't reveal whether the email exists.
            req.setAttribute("error", "If this email is registered, a reset code has been sent.");
            req.getRequestDispatcher("/WEB-INF/views/auth/forgot-password.jsp").forward(req, resp);
            return;
        }

        String code = TokenUtil.generateCode();
        VerificationTokenDAO vt = new VerificationTokenDAO();
        vt.invalidatePrevious(user.getUserId(), "RESET_PASSWORD");
        vt.insert(user.getUserId(), "RESET_PASSWORD", TokenUtil.hash(code));
        EmailSender.sendResetPasswordEmail(user.getEmail(), code, user.getFullName());

        HttpSession s = req.getSession(true);
        s.setAttribute(S_RESET_USER, user.getUserId());
        s.setAttribute(S_RESET_EMAIL, user.getEmail());
        s.removeAttribute(S_RESET_OK);
        req.setAttribute("sent", true);
        req.getRequestDispatcher("/WEB-INF/views/auth/reset-otp.jsp").forward(req, resp);
    }

    private void handleResetPassword(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession s = req.getSession(false);
        if (s == null || s.getAttribute(S_RESET_USER) == null) {
            resp.sendRedirect(req.getContextPath() + "/authen?action=forgot-password");
            return;
        }
        long userId = (Long) s.getAttribute(S_RESET_USER);
        String step = trim(req.getParameter("step"));

        if ("otp".equals(step)) {
            // Step 1: check OTP only — don't consume yet (it must survive to step 2).
            String code = trim(req.getParameter("code"));
            String hash = TokenUtil.hash(code);
            VerificationTokenDAO vt = new VerificationTokenDAO();
            if (code.isEmpty() || !vt.existsLive(userId, "RESET_PASSWORD", hash)) {
                req.setAttribute("error", "Invalid or expired code. Check the latest email or resend.");
                req.getRequestDispatcher("/WEB-INF/views/auth/reset-otp.jsp").forward(req, resp);
                return;
            }
            s.setAttribute(S_RESET_OK, true);
            s.setAttribute(S_RESET_HASH, hash);
            resp.sendRedirect(req.getContextPath() + "/authen?action=reset-password");
            return;
        }

        // Step 2: new password — requires the OTP flag from step 1.
        if (!Boolean.TRUE.equals(s.getAttribute(S_RESET_OK))) {
            resp.sendRedirect(req.getContextPath() + "/authen?action=forgot-password");
            return;
        }
        String password = req.getParameter("password");
        String confirm = req.getParameter("confirmPassword");

        Map<String, String> errors = new HashMap<>();
        if (password == null || password.isEmpty()) {
            errors.put("password", "This field is required.");
        } else if (password.length() < 6) {
            errors.put("password", "Password must be at least 6 characters.");
        }
        if (confirm == null || !confirm.equals(password)) {
            errors.put("confirmPassword", "Passwords do not match.");
        }

        if (!errors.isEmpty()) {
            req.setAttribute("errors", errors);
            req.getRequestDispatcher("/WEB-INF/views/auth/reset-password.jsp").forward(req, resp);
            return;
        }

        // Consume the OTP (hash kept from step 1) + update password in one go.
        String hash = (String) s.getAttribute(S_RESET_HASH);
        VerificationTokenDAO vt = new VerificationTokenDAO();
        if (hash == null || !vt.consume(userId, "RESET_PASSWORD", hash)) {
            // OTP already spent or lost — restart the flow.
            s.removeAttribute(S_RESET_OK);
            s.removeAttribute(S_RESET_HASH);
            req.setAttribute("error", "Session expired. Please request a new code.");
            req.getRequestDispatcher("/WEB-INF/views/auth/forgot-password.jsp").forward(req, resp);
            return;
        }

        new UserDAO().updatePassword(userId, PasswordUtil.hash(password));
        s.removeAttribute(S_RESET_USER);
        s.removeAttribute(S_RESET_EMAIL);
        s.removeAttribute(S_RESET_OK);
        s.removeAttribute(S_RESET_HASH);

        req.setAttribute("success", "Password updated. Please sign in with your new password.");
        req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req, resp);
    }

    /* ============ helpers ============ */
    private void backToRegister(HttpServletRequest req, HttpServletResponse resp,
            Map<String, String> errors,
            String fullName, String email, String phone, String username)
            throws ServletException, IOException {
        if (errors != null) {
            req.setAttribute("errors", errors);
        }
        req.setAttribute("fullNameValue", fullName);
        req.setAttribute("emailValue", email);
        req.setAttribute("phoneValue", phone);
        req.setAttribute("usernameValue", username);
        req.getRequestDispatcher("/WEB-INF/views/auth/register.jsp").forward(req, resp);
    }

    private User currentUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return null;
        }
        Object u = session.getAttribute(SESSION_USER);
        if (u instanceof User) {
            return (User) u;
        }
        return null;
    }

    /**
     * Role → landing path per SRS UC02.
     */
    private String targetFor(User u) {
        String role = u.getRoleName();
        if (role == null) {
            role = "CUSTOMER";
        }
        switch (role) {
            case "OWNER_ADMIN":
                return "/admin";
            case "PHARMACIST":
                return "/staff";
            case "SALES_STAFF":
                return "/pos";
            case "CUSTOMER":
            default:
                return "/home";
        }
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
