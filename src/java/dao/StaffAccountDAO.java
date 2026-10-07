package dao;

import db.DBContext;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.StaffProfile;
import model.User;

/**
 * Owner-side management of internal employee accounts (NHAN_VIEN +
 * NHAN_VIEN_GIAO_HANG). Extends DBContext per rule.md §20.
 *
 * Every read/write here is scoped to MANAGED_ROLES — admin and customer
 * accounts are invisible to this DAO even when a crafted user_id arrives.
 * Writes that touch users + staff_profiles run inside one transaction.
 */
public class StaffAccountDAO extends DBContext {

    private static final Logger LOG = Logger.getLogger(StaffAccountDAO.class.getName());

    /** Roles this module may create/list/edit — never CHU_QUAN_QUAN_TRI or KHACH_HANG. */
    private static final String MANAGED_ROLES = "('NHAN_VIEN','NHAN_VIEN_GIAO_HANG')";

    /** Result of a write command — mapped to Vietnamese messages in the servlet. */
    public enum StaffAccountResult {
        SUCCESS,
        INVALID_ROLE,
        EMAIL_EXISTS,
        USERNAME_EXISTS,
        EMPLOYEE_CODE_EXISTS,
        USER_NOT_FOUND,
        NOT_MANAGED_EMPLOYEE,
        DB_ERROR
    }

    /** One row of the staff list — users + roles + LEFT JOIN staff_profiles. */
    public static class StaffAccountRow {
        public long userId;
        public String fullName;
        public String email;
        public String username;
        public String phone;
        public String status;
        public String roleName;
        public Long staffId;
        public String employeeCode;
        public java.sql.Timestamp createdAt;

        public long getUserId() {
            return userId;
        }

        public String getFullName() {
            return fullName;
        }

        public String getEmail() {
            return email;
        }

        public String getUsername() {
            return username;
        }

        public String getPhone() {
            return phone;
        }

        public String getStatus() {
            return status;
        }

        public String getRoleName() {
            return roleName;
        }

        public Long getStaffId() {
            return staffId;
        }

        public String getEmployeeCode() {
            return employeeCode;
        }

        public java.sql.Timestamp getCreatedAt() {
            return createdAt;
        }

        /** Vietnamese role label — raw codes never reach the JSP. */
        public String getRoleLabel() {
            if ("NHAN_VIEN".equals(roleName)) {
                return "Nhân viên";
            }
            if ("NHAN_VIEN_GIAO_HANG".equals(roleName)) {
                return "Nhân viên giao hàng";
            }
            return roleName;
        }

        /** Vietnamese status label. */
        public String getStatusLabel() {
            if ("HOAT_DONG".equals(status)) {
                return "Hoạt động";
            }
            return "Ngừng hoạt động";
        }
    }

    /**
     * Maps one joined row to a User entity + its StaffProfile for the
     * edit form. Returns StaffAccountRow instead for list pages.
     */
    private StaffAccountRow rowFromResultSet(ResultSet rs) throws SQLException {
        StaffAccountRow r = new StaffAccountRow();
        r.userId = rs.getLong("user_id");
        r.fullName = rs.getString("full_name");
        r.email = rs.getString("email");
        r.username = rs.getString("username");
        r.phone = rs.getString("phone");
        r.status = rs.getString("status");
        r.roleName = rs.getString("role_name");
        long staffId = rs.getLong("staff_id");
        if (rs.wasNull()) {
            r.staffId = null;
        } else {
            r.staffId = staffId;
        }
        r.employeeCode = rs.getString("employee_code");
        r.createdAt = rs.getTimestamp("created_at");
        return r;
    }

    /* ==================== list + filters ==================== */

    /**
     * Shared FROM/WHERE for list + count. Filters are appended as
     * PreparedStatement parameters by the caller — values never concatenate
     * into SQL.
     */
    private String listFromWhere(String q, String role, String status, List<Object> params) {
        StringBuilder sql = new StringBuilder();
        sql.append("FROM users u ");
        sql.append("JOIN roles r ON r.role_id = u.role_id ");
        sql.append("LEFT JOIN staff_profiles sp ON sp.user_id = u.user_id ");
        sql.append("WHERE r.role_name IN ").append(MANAGED_ROLES).append(' ');

        if (q != null && !q.isEmpty()) {
            sql.append("AND (u.full_name LIKE ? OR u.email LIKE ? ");
            sql.append("OR u.username LIKE ? OR sp.employee_code LIKE ?) ");
            String like = "%" + q + "%";
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if ("NHAN_VIEN".equals(role) || "NHAN_VIEN_GIAO_HANG".equals(role)) {
            sql.append("AND r.role_name = ? ");
            params.add(role);
        }
        if ("HOAT_DONG".equals(status) || "NGUNG_HOAT_DONG".equals(status)) {
            sql.append("AND u.status = ? ");
            params.add(status);
        }
        return sql.toString();
    }

    /** Paged staff list — newest first, deterministic tie-break on user_id. */
    public List<StaffAccountRow> findPage(String q, String role, String status,
            int limit, int offset) {
        List<StaffAccountRow> rows = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        String sql = "SELECT u.user_id, u.full_name, u.email, u.username, u.phone, "
                + "u.status, r.role_name, sp.staff_id, sp.employee_code, u.created_at "
                + listFromWhere(q, role, status, params)
                + "ORDER BY u.created_at DESC, u.user_id DESC LIMIT ? OFFSET ?";
        try {
            connection = getConnection();
            if (connection == null) {
                return rows;
            }
            statement = connection.prepareStatement(sql);
            int i = 1;
            for (Object p : params) {
                statement.setObject(i, p);
                i++;
            }
            statement.setInt(i, limit);
            statement.setInt(i + 1, offset);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                rows.add(rowFromResultSet(resultSet));
            }
            return rows;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findPage failed", ex);
            return rows;
        } finally {
            closeResources();
        }
    }

    /** Total matching the same filters — drives the pager. */
    public int countPage(String q, String role, String status) {
        List<Object> params = new ArrayList<>();
        String sql = "SELECT COUNT(*) " + listFromWhere(q, role, status, params);
        try {
            connection = getConnection();
            if (connection == null) {
                return 0;
            }
            statement = connection.prepareStatement(sql);
            int i = 1;
            for (Object p : params) {
                statement.setObject(i, p);
                i++;
            }
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
            return 0;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "countPage failed", ex);
            return 0;
        } finally {
            closeResources();
        }
    }

    /* ==================== find managed target ==================== */

    /**
     * Load one employee by user_id — returns null for customers, admins and
     * unknown ids so a crafted id can never reach an unmanaged account.
     */
    public StaffAccountRow findManagedById(long userId) {
        String sql = "SELECT u.user_id, u.full_name, u.email, u.username, u.phone, "
                + "u.status, r.role_name, sp.staff_id, sp.employee_code, u.created_at "
                + "FROM users u "
                + "JOIN roles r ON r.role_id = u.role_id "
                + "LEFT JOIN staff_profiles sp ON sp.user_id = u.user_id "
                + "WHERE u.user_id = ? AND r.role_name IN " + MANAGED_ROLES + " LIMIT 1";
        try {
            connection = getConnection();
            if (connection == null) {
                return null;
            }
            statement = connection.prepareStatement(sql);
            statement.setLong(1, userId);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return rowFromResultSet(resultSet);
            }
            return null;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "findManagedById failed", ex);
            return null;
        } finally {
            closeResources();
        }
    }

    /* ==================== uniqueness checks ==================== */

    public boolean existsEmailOtherThan(String email, long userId) {
        return existsUserFieldOtherThan("email", email, userId);
    }

    public boolean existsUsernameOtherThan(String username, long userId) {
        return existsUserFieldOtherThan("username", username, userId);
    }

    private boolean existsUserFieldOtherThan(String column, String value, long userId) {
        String sql = "SELECT 1 FROM users WHERE " + column + " = ? AND user_id <> ? LIMIT 1";
        try {
            connection = getConnection();
            if (connection == null) {
                return false;
            }
            statement = connection.prepareStatement(sql);
            statement.setString(1, value);
            statement.setLong(2, userId);
            resultSet = statement.executeQuery();
            return resultSet.next();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "existsUserFieldOtherThan " + column + " failed", ex);
            return false;
        } finally {
            closeResources();
        }
    }

    /** employee_code unique across staff_profiles, excluding this user's own row. */
    public boolean existsEmployeeCodeOtherThan(String employeeCode, long userId) {
        String sql = "SELECT 1 FROM staff_profiles WHERE employee_code = ? AND user_id <> ? LIMIT 1";
        try {
            connection = getConnection();
            if (connection == null) {
                return false;
            }
            statement = connection.prepareStatement(sql);
            statement.setString(1, employeeCode);
            statement.setLong(2, userId);
            resultSet = statement.executeQuery();
            return resultSet.next();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "existsEmployeeCodeOtherThan failed", ex);
            return false;
        } finally {
            closeResources();
        }
    }

    /* ==================== create ==================== */

    /**
     * Create an employee account + its staff_profiles row in one transaction.
     * roleName must be exactly NHAN_VIEN or NHAN_VIEN_GIAO_HANG — resolved to
     * role_id inside the DB, never trusted from the request. The new account
     * starts HOAT_DONG and never enters the customer email-verification flow.
     */
    public StaffAccountResult createEmployee(User user, String roleName, String employeeCode) {
        if (!"NHAN_VIEN".equals(roleName) && !"NHAN_VIEN_GIAO_HANG".equals(roleName)) {
            return StaffAccountResult.INVALID_ROLE;
        }
        String insertUser = "INSERT INTO users (role_id, full_name, email, username, "
                + "password_hash, phone, status) "
                + "SELECT role_id, ?, ?, ?, ?, ?, 'HOAT_DONG' FROM roles WHERE role_name = ?";
        String insertProfile = "INSERT INTO staff_profiles (user_id, employee_code) VALUES (?, ?)";
        try {
            connection = getConnection();
            if (connection == null) {
                return StaffAccountResult.DB_ERROR;
            }
            connection.setAutoCommit(false);
            try {
                // Re-check uniqueness inside the tx — the list-page pre-check
                // is UX only; the DB unique keys are the real guard.
                if (existsOn(connection, "SELECT 1 FROM users WHERE email = ?", user.getEmail())) {
                    connection.rollback();
                    return StaffAccountResult.EMAIL_EXISTS;
                }
                if (existsOn(connection, "SELECT 1 FROM users WHERE username = ?", user.getUsername())) {
                    connection.rollback();
                    return StaffAccountResult.USERNAME_EXISTS;
                }
                if (employeeCode != null
                        && existsOn(connection,
                                "SELECT 1 FROM staff_profiles WHERE employee_code = ?", employeeCode)) {
                    connection.rollback();
                    return StaffAccountResult.EMPLOYEE_CODE_EXISTS;
                }

                statement = connection.prepareStatement(insertUser, Statement.RETURN_GENERATED_KEYS);
                statement.setString(1, user.getFullName());
                statement.setString(2, user.getEmail());
                statement.setString(3, user.getUsername());
                statement.setString(4, user.getPasswordHash());
                if (user.getPhone() == null || user.getPhone().isEmpty()) {
                    statement.setNull(5, java.sql.Types.VARCHAR);
                } else {
                    statement.setString(5, user.getPhone());
                }
                statement.setString(6, roleName);
                int rows = statement.executeUpdate();
                if (rows == 0) {
                    connection.rollback();
                    // role_name not found in roles table — treat as invalid role
                    return StaffAccountResult.INVALID_ROLE;
                }
                resultSet = statement.getGeneratedKeys();
                if (!resultSet.next()) {
                    connection.rollback();
                    return StaffAccountResult.DB_ERROR;
                }
                long newUserId = resultSet.getLong(1);
                resultSet.close();
                statement.close();

                statement = connection.prepareStatement(insertProfile);
                statement.setLong(1, newUserId);
                if (employeeCode == null) {
                    statement.setNull(2, java.sql.Types.VARCHAR);
                } else {
                    statement.setString(2, employeeCode);
                }
                statement.executeUpdate();

                connection.commit();
                return StaffAccountResult.SUCCESS;
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "createEmployee failed", ex);
            return duplicateToResult(ex);
        } finally {
            closeResources();
        }
    }

    /* ==================== update ==================== */

    /**
     * Update identity fields on a managed employee. One transaction because it
     * touches users + staff_profiles; a legacy row missing its profile gets
     * one created instead of failing. Role and status are NOT touched here.
     */
    public StaffAccountResult updateEmployee(long userId, String fullName, String email,
            String username, String phone, String employeeCode) {
        String updateUser = "UPDATE users SET full_name = ?, email = ?, username = ?, phone = ? "
                + "WHERE user_id = ?";
        try {
            connection = getConnection();
            if (connection == null) {
                return StaffAccountResult.DB_ERROR;
            }
            connection.setAutoCommit(false);
            try {
                // Verify the target is a managed employee before any write.
                String roleName = managedRoleOf(connection, userId);
                if (roleName == null) {
                    connection.rollback();
                    return StaffAccountResult.NOT_MANAGED_EMPLOYEE;
                }
                if (existsOtherThanOn(connection,
                        "SELECT 1 FROM users WHERE email = ? AND user_id <> ?", email, userId)) {
                    connection.rollback();
                    return StaffAccountResult.EMAIL_EXISTS;
                }
                if (existsOtherThanOn(connection,
                        "SELECT 1 FROM users WHERE username = ? AND user_id <> ?", username, userId)) {
                    connection.rollback();
                    return StaffAccountResult.USERNAME_EXISTS;
                }
                if (employeeCode != null && existsOtherThanOn(connection,
                        "SELECT 1 FROM staff_profiles WHERE employee_code = ? AND user_id <> ?",
                        employeeCode, userId)) {
                    connection.rollback();
                    return StaffAccountResult.EMPLOYEE_CODE_EXISTS;
                }

                statement = connection.prepareStatement(updateUser);
                statement.setString(1, fullName);
                statement.setString(2, email);
                statement.setString(3, username);
                if (phone == null || phone.isEmpty()) {
                    statement.setNull(4, java.sql.Types.VARCHAR);
                } else {
                    statement.setString(4, phone);
                }
                statement.setLong(5, userId);
                statement.executeUpdate();
                statement.close();

                // Ensure staff_profiles exists — create for legacy rows that lack one.
                boolean hasProfile = existsOn(connection,
                        "SELECT 1 FROM staff_profiles WHERE user_id = ?", userId);
                if (hasProfile) {
                    statement = connection.prepareStatement(
                            "UPDATE staff_profiles SET employee_code = ? WHERE user_id = ?");
                    if (employeeCode == null) {
                        statement.setNull(1, java.sql.Types.VARCHAR);
                    } else {
                        statement.setString(1, employeeCode);
                    }
                    statement.setLong(2, userId);
                    statement.executeUpdate();
                } else {
                    statement = connection.prepareStatement(
                            "INSERT INTO staff_profiles (user_id, employee_code) VALUES (?, ?)");
                    statement.setLong(1, userId);
                    if (employeeCode == null) {
                        statement.setNull(2, java.sql.Types.VARCHAR);
                    } else {
                        statement.setString(2, employeeCode);
                    }
                    statement.executeUpdate();
                }

                connection.commit();
                return StaffAccountResult.SUCCESS;
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "updateEmployee failed", ex);
            return duplicateToResult(ex);
        } finally {
            closeResources();
        }
    }

    /* ==================== activate / deactivate ==================== */

    /**
     * HOAT_DONG → NGUNG_HOAT_DONG. Only touches rows whose current status is
     * HOAT_DONG so a repeated command is a harmless no-op. Returns
     * NOT_MANAGED_EMPLOYEE when the target is a customer/admin/unknown id.
     */
    public StaffAccountResult deactivate(long userId) {
        return setStatus(userId, "HOAT_DONG", "NGUNG_HOAT_DONG");
    }

    /** NGUNG_HOAT_DONG → HOAT_DONG, same guard as deactivate. */
    public StaffAccountResult activate(long userId) {
        return setStatus(userId, "NGUNG_HOAT_DONG", "HOAT_DONG");
    }

    private StaffAccountResult setStatus(long userId, String from, String to) {
        String sql = "UPDATE users u JOIN roles r ON r.role_id = u.role_id "
                + "SET u.status = ? "
                + "WHERE u.user_id = ? AND u.status = ? AND r.role_name IN " + MANAGED_ROLES;
        try {
            connection = getConnection();
            if (connection == null) {
                return StaffAccountResult.DB_ERROR;
            }
            statement = connection.prepareStatement(sql);
            statement.setString(1, to);
            statement.setLong(2, userId);
            statement.setString(3, from);
            int rows = statement.executeUpdate();
            if (rows == 1) {
                return StaffAccountResult.SUCCESS;
            }
            // 0 rows: either already in the target state or not a managed employee.
            String role = managedRoleOf(connection, userId);
            if (role == null) {
                return StaffAccountResult.NOT_MANAGED_EMPLOYEE;
            }
            // Managed employee already in the target state — treat as success (idempotent).
            return StaffAccountResult.SUCCESS;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "setStatus " + to + " failed", ex);
            return StaffAccountResult.DB_ERROR;
        } finally {
            closeResources();
        }
    }

    /* ==================== admin password reset ==================== */

    /**
     * Replace password_hash for a managed employee — admin-chosen password,
     * no OTP, no email. Never touches users.status, so a deactivated employee
     * stays deactivated.
     */
    public StaffAccountResult resetPassword(long userId, String passwordHash) {
        String sql = "UPDATE users u JOIN roles r ON r.role_id = u.role_id "
                + "SET u.password_hash = ? "
                + "WHERE u.user_id = ? AND r.role_name IN " + MANAGED_ROLES;
        try {
            connection = getConnection();
            if (connection == null) {
                return StaffAccountResult.DB_ERROR;
            }
            statement = connection.prepareStatement(sql);
            statement.setString(1, passwordHash);
            statement.setLong(2, userId);
            int rows = statement.executeUpdate();
            if (rows == 1) {
                return StaffAccountResult.SUCCESS;
            }
            return StaffAccountResult.NOT_MANAGED_EMPLOYEE;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "resetPassword failed", ex);
            return StaffAccountResult.DB_ERROR;
        } finally {
            closeResources();
        }
    }

    /* ==================== tx-scoped helpers ==================== */

    /** SELECT 1 existence check on an open tx connection. */
    private boolean existsOn(java.sql.Connection conn, String sql, String value) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean existsOn(java.sql.Connection conn, String sql, long value) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean existsOtherThanOn(java.sql.Connection conn, String sql,
            String value, long userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, value);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Role name of a managed employee on an open tx connection, or null when
     * the user is missing / unmanaged.
     */
    private String managedRoleOf(java.sql.Connection conn, long userId) throws SQLException {
        String sql = "SELECT r.role_name FROM users u JOIN roles r ON r.role_id = u.role_id "
                + "WHERE u.user_id = ? AND r.role_name IN " + MANAGED_ROLES;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
                return null;
            }
        }
    }

    /**
     * Map a duplicate-key race (two admins submitting at once) to the matching
     * business result instead of leaking the SQL error.
     */
    private StaffAccountResult duplicateToResult(SQLException ex) {
        String msg = ex.getMessage();
        if (msg != null && msg.contains("uq_user_email")) {
            return StaffAccountResult.EMAIL_EXISTS;
        }
        if (msg != null && msg.contains("uq_user_username")) {
            return StaffAccountResult.USERNAME_EXISTS;
        }
        if (msg != null && msg.contains("uq_staff_employee_code")) {
            return StaffAccountResult.EMPLOYEE_CODE_EXISTS;
        }
        return StaffAccountResult.DB_ERROR;
    }

    /* ==================== unused per-DAO contract ==================== */

    /** Present per rule.md §20 — row mapping reused via rowFromResultSet. */
    public StaffProfile getFromResultSet(ResultSet rs) throws SQLException {
        StaffProfile p = new StaffProfile();
        p.setStaffId(rs.getLong("staff_id"));
        p.setUserId(rs.getLong("user_id"));
        p.setEmployeeCode(rs.getString("employee_code"));
        return p;
    }
}
