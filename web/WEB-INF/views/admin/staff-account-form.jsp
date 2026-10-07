<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="staff-accounts"/>
<c:set var="isEdit" value="${mode == 'edit'}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>${isEdit ? "Sửa" : "Thêm"} nhân viên — Quản trị — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <div class="section-head">
            <div>
                <h2>${isEdit ? 'Sửa tài khoản nhân viên' : 'Thêm nhân viên'}</h2>
                <p class="section-sub">
                    ${isEdit ? 'Cập nhật thông tin liên hệ. Vai trò và trạng thái thay đổi qua thao tác riêng.'
                              : 'Tài khoản được tạo ở trạng thái Hoạt động — không qua bước xác thực email.'}
                </p>
            </div>
        </div>

        <div class="profile-card">
            <form class="profile-form" method="post" data-disable-on-submit
                  action="${ctx}/admin/staff-accounts?action=${isEdit ? 'update' : 'create'}">

                <c:if test="${isEdit}">
                    <input type="hidden" name="userId" value="${row.userId}">
                </c:if>

                <c:if test="${not empty errors.form}">
                    <div class="alert alert-error" role="alert"><c:out value="${errors.form}"/></div>
                </c:if>

                <fieldset class="profile-group">
                    <legend>Thông tin cá nhân</legend>

                    <div class="profile-grid">
                        <div class="form-field">
                            <label for="fullName">Họ tên <span class="req">*</span></label>
                            <input type="text" id="fullName" name="fullName" required maxlength="150"
                                   value="<c:out value='${isEdit ? row.fullName : fullNameValue}'/>">
                            <c:if test="${not empty errors.fullName}">
                                <span class="field-error"><c:out value="${errors.fullName}"/></span>
                            </c:if>
                        </div>

                        <div class="form-field">
                            <label for="phone">Số điện thoại</label>
                            <input type="tel" id="phone" name="phone" maxlength="30"
                                   value="<c:out value='${isEdit ? row.phone : phoneValue}'/>">
                            <c:if test="${not empty errors.phone}">
                                <span class="field-error"><c:out value="${errors.phone}"/></span>
                            </c:if>
                        </div>
                    </div>

                    <div class="profile-grid">
                        <div class="form-field">
                            <label for="email">Email <span class="req">*</span></label>
                            <input type="email" id="email" name="email" required maxlength="150"
                                   value="<c:out value='${isEdit ? row.email : emailValue}'/>">
                            <c:if test="${not empty errors.email}">
                                <span class="field-error"><c:out value="${errors.email}"/></span>
                            </c:if>
                        </div>

                        <div class="form-field">
                            <label for="username">Tên đăng nhập <span class="req">*</span></label>
                            <input type="text" id="username" name="username" required maxlength="100"
                                   value="<c:out value='${isEdit ? row.username : usernameValue}'/>">
                            <c:if test="${not empty errors.username}">
                                <span class="field-error"><c:out value="${errors.username}"/></span>
                            </c:if>
                        </div>
                    </div>

                    <div class="profile-grid">
                        <div class="form-field">
                            <label for="employeeCode">Mã nhân viên</label>
                            <input type="text" id="employeeCode" name="employeeCode" maxlength="50"
                                   value="<c:out value='${isEdit ? row.employeeCode : employeeCodeValue}'/>"
                                   placeholder="VD: NV001 — có thể để trống">
                            <c:if test="${not empty errors.employeeCode}">
                                <span class="field-error"><c:out value="${errors.employeeCode}"/></span>
                            </c:if>
                        </div>

                        <div class="form-field">
                            <label>Vai trò <span class="req">*</span></label>
                            <c:choose>
                                <c:when test="${isEdit}">
                                    <%-- Role is immutable after creation — text only, no input. --%>
                                    <div class="field-static"><c:out value="${row.roleLabel}"/></div>
                                </c:when>
                                <c:otherwise>
                                    <select id="roleName" name="roleName" required>
                                        <option value="NHAN_VIEN"
                                                ${roleNameValue == 'NHAN_VIEN' ? 'selected' : ''}>Nhân viên</option>
                                        <option value="NHAN_VIEN_GIAO_HANG"
                                                ${roleNameValue == 'NHAN_VIEN_GIAO_HANG' ? 'selected' : ''}>Nhân viên giao hàng</option>
                                    </select>
                                    <c:if test="${not empty errors.roleName}">
                                        <span class="field-error"><c:out value="${errors.roleName}"/></span>
                                    </c:if>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                </fieldset>

                <c:if test="${!isEdit}">
                    <fieldset class="profile-group">
                        <legend>Mật khẩu ban đầu</legend>
                        <p class="field-hint">Admin đặt mật khẩu cho nhân viên — không gửi email, không OTP.</p>

                        <div class="profile-grid">
                            <div class="form-field">
                                <label for="password">Mật khẩu <span class="req">*</span></label>
                                <input type="password" id="password" name="password" required minlength="6"
                                       autocomplete="new-password">
                                <c:if test="${not empty errors.password}">
                                    <span class="field-error"><c:out value="${errors.password}"/></span>
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
                </c:if>

                <div class="profile-actions">
                    <button type="submit" class="btn btn-primary">${isEdit ? 'Lưu thay đổi' : 'Tạo tài khoản'}</button>
                    <a class="btn btn-ghost" href="${ctx}/admin/staff-accounts">Hủy</a>
                </div>
            </form>
        </div>
    </main>
</div>

<%@ include file="/WEB-INF/jspf/admin-footer.jspf" %>

<script>
    // Prevent duplicate submits (same pattern as other forms)
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
