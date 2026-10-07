<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="staff-accounts"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Đặt lại mật khẩu — Quản trị — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <div class="section-head">
            <div>
                <h2>Đặt lại mật khẩu</h2>
                <p class="section-sub">
                    Tài khoản: <strong><c:out value="${row.fullName}"/></strong>
                    (<c:out value="${row.username}"/>) — <c:out value="${row.roleLabel}"/>
                </p>
            </div>
        </div>

        <div class="profile-card">
            <form class="profile-form" method="post" data-disable-on-submit
                  action="${ctx}/admin/staff-accounts?action=reset-password">
                <input type="hidden" name="userId" value="${row.userId}">

                <fieldset class="profile-group">
                    <legend>Mật khẩu mới</legend>
                    <p class="field-hint">
                        Admin đặt mật khẩu mới trực tiếp — không cần mật khẩu cũ, không gửi OTP hay email.
                        Tài khoản đang ngừng hoạt động sẽ giữ nguyên trạng thái.
                    </p>

                    <div class="profile-grid">
                        <div class="form-field">
                            <label for="newPassword">Mật khẩu mới <span class="req">*</span></label>
                            <input type="password" id="newPassword" name="newPassword" required minlength="6"
                                   autocomplete="new-password">
                            <c:if test="${not empty errors.newPassword}">
                                <span class="field-error"><c:out value="${errors.newPassword}"/></span>
                            </c:if>
                        </div>

                        <div class="form-field">
                            <label for="confirmPassword">Xác nhận mật khẩu <span class="req">*</span></label>
                            <input type="password" id="confirmPassword" name="confirmPassword" required minlength="6"
                                   autocomplete="new-password">
                            <c:if test="${not empty errors.confirmPassword}">
                                <span class="field-error"><c:out value="${errors.confirmPassword}"/></span>
                            </c:if>
                        </div>
                    </div>
                </fieldset>

                <div class="profile-actions">
                    <button type="submit" class="btn btn-primary">Đặt lại mật khẩu</button>
                    <a class="btn btn-ghost" href="${ctx}/admin/staff-accounts">Hủy</a>
                </div>
            </form>
        </div>
    </main>
</div>

<%@ include file="/WEB-INF/jspf/admin-footer.jspf" %>

<script>
    (function () {
        var form = document.querySelector('form[data-disable-on-submit]');
        if (!form) return;
        form.addEventListener('submit', function () {
            var btn = form.querySelector('button[type="submit"]');
            if (btn) { btn.disabled = true; btn.textContent = 'Đang lưu…'; }
        });
    })();
</script>
</body>
</html>
