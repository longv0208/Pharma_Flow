package model;

/**
 * Mirror of `users` table.
 * Wrapper types per rule.md §17. passwordHash stored but never exposed to JSP.
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
    private String status;   // "ACTIVE" | "INACTIVE"

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }

    public Long getRoleId() { return roleId; }
    public void setRoleId(Long v) { this.roleId = v; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String v) { this.roleName = v; }

    public String getFullName() { return fullName; }
    public void setFullName(String v) { this.fullName = v; }

    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }

    public String getUsername() { return username; }
    public void setUsername(String v) { this.username = v; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String v) { this.passwordHash = v; }

    public String getPhone() { return phone; }
    public void setPhone(String v) { this.phone = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    /** Convenience for JSP / session consumers. */
    public boolean isActive() { return "ACTIVE".equals(status); }
}
