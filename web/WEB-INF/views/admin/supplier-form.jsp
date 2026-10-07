<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="suppliers"/>
<c:set var="isEdit" value="${mode == 'edit'}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>${isEdit ? "Sửa" : "Thêm"} Nhà cung cấp — Admin — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <div class="section-head">
            <div>
                <h2>${isEdit ? 'Sửa Nhà cung cấp' : 'Thêm Nhà cung cấp'}</h2>
                <p class="section-sub">
                    ${isEdit ? 'Cập nhật thông tin liên hệ và trạng thái hoạt động.' : 'Đăng ký nhà cung cấp mới cho đơn mua hàng.'}
                </p>
            </div>
        </div>

        <div class="profile-card">
            <form class="profile-form" method="post" data-disable-on-submit
                  action="${ctx}/admin?action=${isEdit ? 'supplier-update' : 'supplier-create'}">

                <c:if test="${isEdit}">
                    <input type="hidden" name="supplierId" value="${supplier.supplierId}">
                </c:if>

                <fieldset class="profile-group">
                    <legend>Công ty</legend>

                    <div class="form-field">
                        <label for="supplierName">Tên nhà cung cấp <span class="req">*</span></label>
                        <input type="text" id="supplierName" name="supplierName" required maxlength="200"
                               value="<c:out value='${supplier.supplierName}'/>">
                        <c:if test="${not empty errors.supplierName}">
                            <span class="field-error"><c:out value="${errors.supplierName}"/></span>
                        </c:if>
                    </div>

                    <div class="form-field">
                        <label for="taxBusinessInfo">Mã số thuế / Thông tin doanh nghiệp</label>
                        <input type="text" id="taxBusinessInfo" name="taxBusinessInfo" maxlength="255"
                               value="<c:out value='${supplier.taxBusinessInfo}'/>"
                               placeholder="Mã số thuế, giấy phép kinh doanh…">
                    </div>

                    <div class="form-field">
                        <label for="address">Địa chỉ</label>
                        <input type="text" id="address" name="address" maxlength="255"
                               value="<c:out value='${supplier.address}'/>">
                    </div>
                </fieldset>

                <fieldset class="profile-group">
                    <legend>Liên hệ</legend>

                    <div class="profile-grid">
                        <div class="form-field">
                            <label for="contactPerson">Người liên hệ</label>
                            <input type="text" id="contactPerson" name="contactPerson" maxlength="150"
                                   value="<c:out value='${supplier.contactPerson}'/>">
                        </div>

                        <div class="form-field">
                            <label for="phone">Số điện thoại</label>
                            <input type="tel" id="phone" name="phone" maxlength="30"
                                   value="<c:out value='${supplier.phone}'/>">
                        </div>

                        <div class="form-field">
                            <label for="email">Email</label>
                            <input type="email" id="email" name="email" maxlength="150"
                                   value="<c:out value='${supplier.email}'/>">
                            <c:if test="${not empty errors.email}">
                                <span class="field-error"><c:out value="${errors.email}"/></span>
                            </c:if>
                        </div>
                    </div>
                </fieldset>

                <c:if test="${isEdit}">
                    <div class="form-field">
                        <label for="status">Trạng thái</label>
                        <select id="status" name="status">
                            <option value="HOAT_DONG"       ${supplier.status == 'HOAT_DONG'       ? 'selected' : ''}>HOẠT ĐỘNG — có thể tạo đơn mua hàng</option>
                            <option value="NGUNG_HOAT_DONG" ${supplier.status == 'NGUNG_HOAT_DONG' ? 'selected' : ''}>NGỪNG HOẠT ĐỘNG — giữ lại lịch sử</option>
                        </select>
                    </div>
                </c:if>

                <div class="profile-actions">
                    <button type="submit" class="btn btn-primary">${isEdit ? 'Lưu thay đổi' : 'Tạo Nhà cung cấp'}</button>
                    <a class="btn btn-ghost" href="${ctx}/admin?action=suppliers">Hủy</a>
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
