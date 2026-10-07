package controller;

import dao.StaffAccountDAO;
import dao.StaffAccountDAO.StaffAccountResult;
import dao.StaffAccountDAO.StaffAccountRow;
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
import java.util.List;
import java.util.Map;

/**
 * /admin/staff-accounts — CHU_QUAN_QUAN_TRI only. Manages internal employee
 * accounts (NHAN_VIEN + NHAN_VIEN_GIAO_HANG): list/search/filter/paginate,
 * create, edit, activate/deactivate, admin password reset.
 *
 * Never touches KHACH_HANG or CHU_QUAN_QUAN_TRI — the DAO scopes every query
 * to the two managed role names, so a crafted user_id can't reach another
 * account class. Dispatch via ?action= param (rule.md §22).
 */
@WebServlet(name = "StaffAccountServlet", urlPatterns = {"/admin/staff-accounts"})
public class StaffAccountServlet extends HttpServlet {

    private static final int PAGE_SIZE = 15;
    private static final String LIST_JSP = "/WEB-INF/views/admin/staff-account-list.jsp";
    private static final String FORM_JSP = "/WEB-INF/views/admin/staff-account-form.jsp";
    private static final String PASSWORD_JSP = "/WEB-INF/views/admin/staff-account-password.jsp";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!requireAdmin(req, resp)) {
            return;
        }
        String action = req.getParameter("action");
        if (action == null) {
            action = "list";
        }
        switch (action) {
            case "list":
                handleList(req, resp);
                break;
            case "new":
                handleNewForm(req, resp);
                break;
            case "edit":
                handleEditForm(req, resp);
                break;
            case "password":
                handlePasswordForm(req, resp);
                break;
            default:
                handleList(req, resp);
                break;
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!requireAdmin(req, resp)) {
            return;
        }
        String action = req.getParameter("action");
        if (action == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        switch (action) {
            case "create":
                handleCreate(req, resp);
                break;
            case "update":
                handleUpdate(req, resp);
                break;
            case "activate":
                handleActivate(req, resp);
                break;
            case "deactivate":
                handleDeactivate(req, resp);
                break;
            case "reset-password":
                handleResetPassword(req, resp);
                break;
            default:
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                break;
        }
    }

    /* ==================== GET handlers ==================== */

    private void handleList(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String q = trim(req.getParameter("q"));
        String role = allowlistedRole(req.getParameter("role"));
        String status = allowlistedStatus(req.getParameter("status"));

        int page = (int) parseId(req.getParameter("page"));
        if (page < 1) {
            page = 1;
        }

        StaffAccountDAO dao = new StaffAccountDAO();
        int total = dao.countPage(q, role, status);
        int pages = (total + PAGE_SIZE - 1) / PAGE_SIZE;
        if (pages < 1) {
            pages = 1;
        }
        if (page > pages) {
            page = pages;
        }

        List<StaffAccountRow> rows = dao.findPage(q, role, status,
                PAGE_SIZE, (page - 1) * PAGE_SIZE);

        req.setAttribute("rows", rows);
        req.setAttribute("total", total);
        req.setAttribute("page", page);
        req.setAttribute("pages", pages);
        req.getRequestDispatcher(LIST_JSP).forward(req, resp);
    }

    private void handleNewForm(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setAttribute("mode", "create");
        req.getRequestDispatcher(FORM_JSP).forward(req, resp);
    }

    private void handleEditForm(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long id = parseId(req.getParameter("id"));
        StaffAccountRow row = null;
        if (id > 0) {
            row = new StaffAccountDAO().findManagedById(id);
        }
        if (row == null) {
            resp.sendRedirect(req.getContextPath() + "/admin/staff-accounts?err=notfound");
            return;
        }
        req.setAttribute("mode", "edit");
        req.setAttribute("row", row);
        req.getRequestDispatcher(FORM_JSP).forward(req, resp);
    }

    private void handlePasswordForm(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long id = parseId(req.getParameter("id"));
        StaffAccountRow row = null;
        if (id > 0) {
            row = new StaffAccountDAO().findManagedById(id);
        }
        if (row == null) {
            resp.sendRedirect(req.getContextPath() + "/admin/staff-accounts?err=notfound");
            return;
        }
        req.setAttribute("row", row);
        req.getRequestDispatcher(PASSWORD_JSP).forward(req, resp);
    }

    /* ==================== POST handlers ==================== */

    private void handleCreate(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String fullName = trim(req.getParameter("fullName"));
        String email = trim(req.getParameter("email"));
        String username = trim(req.getParameter("username"));
        String phone = trim(req.getParameter("phone"));
        String employeeCode = trim(req.getParameter("employeeCode"));
        String roleName = allowlistedRole(req.getParameter("roleName"));
        String password = req.getParameter("password");
        String confirm = req.getParameter("confirmPassword");

        Map<String, String> errors = validateIdentity(fullName, email, username,
                phone, employeeCode);
        if (roleName == null) {
            errors.put("roleName", "Vai trò nhân viên không hợp lệ.");
        }
        if (password == null || password.isEmpty()) {
            errors.put("password", "Vui lòng nhập mật khẩu.");
        } else if (password.length() < 6) {
            errors.put("password", "Mật khẩu phải có ít nhất 6 ký tự.");
        }
        if (confirm == null || !confirm.equals(password)) {
            errors.put("confirmPassword", "Mật khẩu xác nhận không khớp.");
        }

        StaffAccountDAO dao = new StaffAccountDAO();
        // UX pre-checks; the DAO re-verifies inside the transaction.
        if (errors.isEmpty()) {
            if (dao.existsEmailOtherThan(email, 0)) {
                errors.put("email", "Email đã được sử dụng.");
            }
            if (dao.existsUsernameOtherThan(username, 0)) {
                errors.put("username", "Tên đăng nhập đã được sử dụng.");
            }
            if (!employeeCode.isEmpty()
                    && dao.existsEmployeeCodeOtherThan(employeeCode, 0)) {
                errors.put("employeeCode", "Mã nhân viên đã được sử dụng.");
            }
        }

        if (!errors.isEmpty()) {
            backToForm(req, resp, errors, "create", null,
                    fullName, email, username, phone, employeeCode, roleName);
            return;
        }

        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setUsername(username);
        user.setPhone(phone.isEmpty() ? null : phone);
        user.setPasswordHash(PasswordUtil.hash(password));

        String codeForDb = employeeCode.isEmpty() ? null : employeeCode;
        StaffAccountResult result = dao.createEmployee(user, roleName, codeForDb);
        if (result != StaffAccountResult.SUCCESS) {
            errors = errorsForResult(result);
            backToForm(req, resp, errors, "create", null,
                    fullName, email, username, phone, employeeCode, roleName);
            return;
        }
        resp.sendRedirect(req.getContextPath() + "/admin/staff-accounts?ok=created");
    }

    private void handleUpdate(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long id = parseId(req.getParameter("userId"));
        String fullName = trim(req.getParameter("fullName"));
        String email = trim(req.getParameter("email"));
        String username = trim(req.getParameter("username"));
        String phone = trim(req.getParameter("phone"));
        String employeeCode = trim(req.getParameter("employeeCode"));

        StaffAccountDAO dao = new StaffAccountDAO();
        StaffAccountRow existing = null;
        if (id > 0) {
            existing = dao.findManagedById(id);
        }
        if (existing == null) {
            resp.sendRedirect(req.getContextPath() + "/admin/staff-accounts?err=notfound");
            return;
        }

        Map<String, String> errors = validateIdentity(fullName, email, username,
                phone, employeeCode);
        if (errors.isEmpty()) {
            if (dao.existsEmailOtherThan(email, id)) {
                errors.put("email", "Email đã được sử dụng.");
            }
            if (dao.existsUsernameOtherThan(username, id)) {
                errors.put("username", "Tên đăng nhập đã được sử dụng.");
            }
            if (!employeeCode.isEmpty()
                    && dao.existsEmployeeCodeOtherThan(employeeCode, id)) {
                errors.put("employeeCode", "Mã nhân viên đã được sử dụng.");
            }
        }

        if (!errors.isEmpty()) {
            backToForm(req, resp, errors, "edit", existing,
                    fullName, email, username, phone, employeeCode, null);
            return;
        }

        String codeForDb = employeeCode.isEmpty() ? null : employeeCode;
        StaffAccountResult result = dao.updateEmployee(id, fullName, email, username,
                phone.isEmpty() ? null : phone, codeForDb);
        if (result != StaffAccountResult.SUCCESS) {
            errors = errorsForResult(result);
            backToForm(req, resp, errors, "edit", existing,
                    fullName, email, username, phone, employeeCode, null);
            return;
        }
        resp.sendRedirect(req.getContextPath() + "/admin/staff-accounts?ok=updated");
    }

    private void handleActivate(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        long id = parseId(req.getParameter("userId"));
        if (id <= 0) {
            resp.sendRedirect(req.getContextPath() + "/admin/staff-accounts?err=notfound");
            return;
        }
        StaffAccountResult result = new StaffAccountDAO().activate(id);
        String ok = result == StaffAccountResult.SUCCESS ? "activated" : null;
        if (ok != null) {
            resp.sendRedirect(req.getContextPath() + "/admin/staff-accounts?ok=" + ok);
        } else {
            resp.sendRedirect(req.getContextPath() + "/admin/staff-accounts?err=notfound");
        }
    }

    private void handleDeactivate(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        long id = parseId(req.getParameter("userId"));
        if (id <= 0) {
            resp.sendRedirect(req.getContextPath() + "/admin/staff-accounts?err=notfound");
            return;
        }
        StaffAccountResult result = new StaffAccountDAO().deactivate(id);
        if (result == StaffAccountResult.SUCCESS) {
            resp.sendRedirect(req.getContextPath() + "/admin/staff-accounts?ok=deactivated");
        } else {
            resp.sendRedirect(req.getContextPath() + "/admin/staff-accounts?err=notfound");
        }
    }

    private void handleResetPassword(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long id = parseId(req.getParameter("userId"));
        String password = req.getParameter("newPassword");
        String confirm = req.getParameter("confirmPassword");

        StaffAccountDAO dao = new StaffAccountDAO();
        StaffAccountRow row = null;
        if (id > 0) {
            row = dao.findManagedById(id);
        }
        if (row == null) {
            resp.sendRedirect(req.getContextPath() + "/admin/staff-accounts?err=notfound");
            return;
        }

        Map<String, String> errors = new HashMap<>();
        if (password == null || password.isEmpty()) {
            errors.put("newPassword", "Vui lòng nhập mật khẩu mới.");
        } else if (password.length() < 6) {
            errors.put("newPassword", "Mật khẩu phải có ít nhất 6 ký tự.");
        }
        if (confirm == null || !confirm.equals(password)) {
            errors.put("confirmPassword", "Mật khẩu xác nhận không khớp.");
        }

        if (!errors.isEmpty()) {
            req.setAttribute("errors", errors);
            req.setAttribute("row", row);
            req.getRequestDispatcher(PASSWORD_JSP).forward(req, resp);
            return;
        }

        StaffAccountResult result = dao.resetPassword(id, PasswordUtil.hash(password));
        if (result == StaffAccountResult.SUCCESS) {
            resp.sendRedirect(req.getContextPath() + "/admin/staff-accounts?ok=password-reset");
        } else {
            resp.sendRedirect(req.getContextPath() + "/admin/staff-accounts?err=notfound");
        }
    }

    /* ==================== helpers ==================== */

    /** Allowlist: only the two managed role names pass through. */
    private static String allowlistedRole(String role) {
        String r = trim(role);
        if ("NHAN_VIEN".equals(r) || "NHAN_VIEN_GIAO_HANG".equals(r)) {
            return r;
        }
        return null;
    }

    /** Allowlist: only the two real status values pass through. */
    private static String allowlistedStatus(String status) {
        String s = trim(status);
        if ("HOAT_DONG".equals(s) || "NGUNG_HOAT_DONG".equals(s)) {
            return s;
        }
        return null;
    }

    /** Field validation shared by create + update — backend is authoritative. */
    private Map<String, String> validateIdentity(String fullName, String email,
            String username, String phone, String employeeCode) {
        Map<String, String> errors = new HashMap<>();
        if (fullName.isEmpty()) {
            errors.put("fullName", "Vui lòng nhập họ tên.");
        } else if (fullName.length() > 150) {
            errors.put("fullName", "Tối đa 150 ký tự.");
        }
        if (email.isEmpty()) {
            errors.put("email", "Vui lòng nhập email.");
        } else if (email.length() > 150) {
            errors.put("email", "Tối đa 150 ký tự.");
        } else if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            errors.put("email", "Email không hợp lệ.");
        }
        if (username.isEmpty()) {
            errors.put("username", "Vui lòng nhập tên đăng nhập.");
        } else if (username.length() > 100) {
            errors.put("username", "Tối đa 100 ký tự.");
        }
        if (phone.length() > 30) {
            errors.put("phone", "Tối đa 30 ký tự.");
        }
        if (employeeCode.length() > 50) {
            errors.put("employeeCode", "Tối đa 50 ký tự.");
        }
        return errors;
    }

    /** Map a DAO result to per-field errors for the form. */
    private Map<String, String> errorsForResult(StaffAccountResult result) {
        Map<String, String> errors = new HashMap<>();
        if (result == StaffAccountResult.EMAIL_EXISTS) {
            errors.put("email", "Email đã được sử dụng.");
        } else if (result == StaffAccountResult.USERNAME_EXISTS) {
            errors.put("username", "Tên đăng nhập đã được sử dụng.");
        } else if (result == StaffAccountResult.EMPLOYEE_CODE_EXISTS) {
            errors.put("employeeCode", "Mã nhân viên đã được sử dụng.");
        } else if (result == StaffAccountResult.INVALID_ROLE) {
            errors.put("roleName", "Vai trò nhân viên không hợp lệ.");
        } else {
            errors.put("form", "Không thể lưu tài khoản. Vui lòng thử lại.");
        }
        return errors;
    }

    /**
     * Re-render the form with the submitted values — same repopulate pattern
     * as AdminServlet's category/supplier forms.
     */
    private void backToForm(HttpServletRequest req, HttpServletResponse resp,
            Map<String, String> errors, String mode, StaffAccountRow existing,
            String fullName, String email, String username,
            String phone, String employeeCode, String roleName)
            throws ServletException, IOException {
        req.setAttribute("errors", errors);
        req.setAttribute("mode", mode);
        // In edit mode the row's own id must survive; in create mode there is no row.
        if (existing != null) {
            req.setAttribute("row", existing);
        }
        req.setAttribute("fullNameValue", fullName);
        req.setAttribute("emailValue", email);
        req.setAttribute("usernameValue", username);
        req.setAttribute("phoneValue", phone);
        req.setAttribute("employeeCodeValue", employeeCode);
        req.setAttribute("roleNameValue", roleName);
        req.getRequestDispatcher(FORM_JSP).forward(req, resp);
    }

    /** Gate: must be logged in as CHU_QUAN_QUAN_TRI. Returns false after redirect. */
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
