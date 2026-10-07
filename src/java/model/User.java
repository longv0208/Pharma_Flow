package model;

/**
 * Mirror of `users` table. Wrapper types per rule.md §17. passwordHash stored
 * but never exposed to JSP.
 */
public class User {

    private Long userId;
    private Long roleId;
    private String roleName;
    private String fullName;
    private String email;
    private String username;
    private String passwordHash;
    private String phone;
    private String status;   // "HOAT_DONG" | "NGUNG_HOAT_DONG"

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Convenience for JSP / session consumers.
     */
    public boolean isActive() {
        return "HOAT_DONG".equals(status);
    }
}
