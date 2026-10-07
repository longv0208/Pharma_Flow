<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Hồ sơ của tôi — PharmaFlow</title>
    <link rel="stylesheet" href="${ctx}/css/main.css">
</head>
<body>

<%@ include file="/WEB-INF/jspf/header.jspf" %>

<section class="section">
    <div class="container profile-wrap">
        <div class="section-head">
            <div>
                <h2>Hồ sơ của tôi</h2>
                <p class="section-sub">Quản lý thông tin cá nhân và địa chỉ giao hàng mặc định.</p>
            </div>
        </div>

        <c:if test="${not empty error}">
            <div class="alert alert-error" role="alert"><c:out value="${error}"/></div>
        </c:if>
        <c:if test="${not empty success}">
            <div class="alert alert-success" role="status"><c:out value="${success}"/></div>
        </c:if>

        <div class="profile-card">
            <form class="profile-form" action="${ctx}/profile" method="post" data-disable-on-submit>

                <fieldset class="profile-group">
                    <legend>Tài khoản</legend>

                    <div class="form-field">
                        <label for="email">Email</label>
                        <input type="email" id="email" name="email"
                               value="<c:out value='${sessionScope.currentUser.email}'/>"
                               readonly disabled aria-readonly="true">
                        <span class="field-hint">Email là tài khoản đăng nhập của bạn và không thể thay đổi tại đây.</span>
                    </div>

                    <div class="form-field">
                        <label for="fullName">Họ tên</label>
                        <input type="text" id="fullName" name="fullName"
                               value="<c:out value='${sessionScope.currentUser.fullName}'/>"
                               autocomplete="name" required>
                        <c:if test="${not empty errors.fullName}">
                            <span class="field-error"><c:out value="${errors.fullName}"/></span>
                        </c:if>
                    </div>

                    <div class="form-field">
                        <label for="phone">Số điện thoại</label>
                        <input type="tel" id="phone" name="phone"
                               value="<c:out value='${sessionScope.currentUser.phone}'/>"
                               autocomplete="tel" required>
                        <c:if test="${not empty errors.phone}">
                            <span class="field-error"><c:out value="${errors.phone}"/></span>
                        </c:if>
                    </div>
                </fieldset>

                <fieldset class="profile-group">
                    <legend>Địa chỉ giao hàng</legend>

                    <div class="profile-grid">
                        <div class="form-field">
                            <label for="provinceCity">Tỉnh / Thành phố</label>
                            <input type="text" id="provinceCity" name="provinceCity"
                                   value="<c:out value='${profile.provinceCity}'/>" autocomplete="address-level1">
                        </div>

                        <div class="form-field">
                            <label for="district">Quận / Huyện</label>
                            <input type="text" id="district" name="district"
                                   value="<c:out value='${profile.district}'/>" autocomplete="address-level2">
                        </div>

                        <div class="form-field">
                            <label for="ward">Phường / Xã</label>
                            <input type="text" id="ward" name="ward"
                                   value="<c:out value='${profile.ward}'/>" autocomplete="address-level3">
                        </div>
                    </div>

                    <div class="form-field">
                        <label for="detailedAddress">Địa chỉ chi tiết</label>
                        <input type="text" id="detailedAddress" name="detailedAddress"
                               value="<c:out value='${profile.detailedAddress}'/>"
                               autocomplete="street-address"
                               placeholder="Số nhà, đường…">
                    </div>
                </fieldset>

                <div class="profile-actions">
                    <button type="submit" class="btn btn-primary">Lưu thay đổi</button>
                    <a class="btn btn-ghost" href="${ctx}/home">Hủy</a>
                </div>
            </form>
        </div>

        <%-- Change password — separate POST action on the same servlet --%>
        <div class="profile-card" style="margin-top:24px">
            <form class="profile-form" action="${ctx}/profile?action=change-password" method="post" data-disable-on-submit>
                <fieldset class="profile-group">
                    <legend>Đổi mật khẩu</legend>

                    <c:if test="${not empty pwErrors.currentPassword or not empty pwErrors.newPassword or not empty pwErrors.confirmNewPassword}">
                        <p class="field-hint">Sửa các lỗi bên dưới để cập nhật mật khẩu của bạn.</p>
                    </c:if>

                    <div class="form-field">
                        <label for="currentPassword">Mật khẩu hiện tại</label>
                        <input type="password" id="currentPassword" name="currentPassword"
                               autocomplete="current-password" required>
                        <c:if test="${not empty pwErrors.currentPassword}">
                            <span class="field-error"><c:out value="${pwErrors.currentPassword}"/></span>
                        </c:if>
                    </div>

                    <div class="profile-grid">
                        <div class="form-field">
                            <label for="newPassword">Mật khẩu mới</label>
                            <input type="password" id="newPassword" name="newPassword"
                                   autocomplete="new-password" required minlength="6">
                            <c:if test="${not empty pwErrors.newPassword}">
                                <span class="field-error"><c:out value="${pwErrors.newPassword}"/></span>
                            </c:if>
                        </div>
                        <div class="form-field">
                            <label for="confirmNewPassword">Xác nhận mật khẩu mới</label>
                            <input type="password" id="confirmNewPassword" name="confirmNewPassword"
                                   autocomplete="new-password" required minlength="6">
                            <c:if test="${not empty pwErrors.confirmNewPassword}">
                                <span class="field-error"><c:out value="${pwErrors.confirmNewPassword}"/></span>
                            </c:if>
                        </div>
                    </div>
                </fieldset>

                <div class="profile-actions">
                    <button type="submit" class="btn btn-primary">Cập nhật mật khẩu</button>
                </div>
            </form>
        </div>
    </div>
</section>

<%@ include file="/WEB-INF/jspf/footer.jspf" %>

<script>
    // Prevent duplicate submits (same pattern as register.jsp)
    (function () {
        var form = document.querySelector('form[data-disable-on-submit]');
        if (!form) return;
        form.addEventListener('submit', function () {
            var btn = form.querySelector('button[type="submit"]');
            if (btn) {
                btn.disabled = true;
                btn.textContent = 'Đang lưu…';
            }
        });
    })();
</script>
</body>
</html>
