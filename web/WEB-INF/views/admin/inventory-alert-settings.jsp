<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="inventory-alerts"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Cài đặt Cảnh báo Tồn kho — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <nav class="crumbs" aria-label="Breadcrumb">
            <a href="${ctx}/inventory">Tồn kho</a>
            <span aria-hidden="true">›</span>
            <a href="${ctx}/inventory/alerts">Cảnh báo</a>
            <span aria-hidden="true">›</span>
            <span>Cài đặt</span>
        </nav>

        <div class="section-head">
            <div>
                <h2>Cài đặt Cảnh báo Tồn kho</h2>
                <p class="section-sub">Ngưỡng chung áp dụng cho tất cả sản phẩm và lô.</p>
            </div>
        </div>

        <c:if test="${param.ok == 'saved'}">
            <div class="alert alert-success" role="status">Đã lưu cài đặt cảnh báo.</div>
        </c:if>
        <c:if test="${param.err == 'invalid'}">
            <div class="alert alert-error" role="alert">Cả hai giá trị phải là số nguyên lớn hơn hoặc bằng 0.</div>
        </c:if>
        <c:if test="${param.err == 'db'}">
            <div class="alert alert-error" role="alert">Lỗi cơ sở dữ liệu — cài đặt chưa được lưu.</div>
        </c:if>

        <div class="profile-card">
            <form class="profile-form" method="post" data-disable-on-submit
                  action="${ctx}/inventory/alerts?action=save-settings">

                <fieldset class="profile-group">
                    <legend>Cài đặt Chung</legend>
                    <p class="field-hint">
                        Các ngưỡng này áp dụng cho mọi sản phẩm và lô — không có
                        tùy chỉnh riêng theo sản phẩm. Thay đổi có hiệu lực ở lần tải cảnh báo tiếp theo.
                    </p>
                    <div class="profile-grid">
                        <div class="form-field">
                            <label for="minimumStockLevel">Mức tồn tối thiểu <span class="req">*</span></label>
                            <div class="input-suffix-wrap">
                                <input type="number" id="minimumStockLevel" name="minimumStockLevel"
                                       required min="0" step="1"
                                       value="<c:out value='${settings.minimumStockLevel}'/>">
                                <span class="input-suffix">đơn vị</span>
                            </div>
                            <span class="field-hint">Sản phẩm có số đơn vị có thể bán nhỏ hơn hoặc bằng mức này sẽ kích hoạt LOW_STOCK. Ví dụ: 10 = cảnh báo khi còn 10 đơn vị trở xuống.</span>
                        </div>
                        <div class="form-field">
                            <label for="nearExpiryWarningDays">Cảnh báo Sắp hết hạn <span class="req">*</span></label>
                            <div class="input-suffix-wrap">
                                <input type="number" id="nearExpiryWarningDays" name="nearExpiryWarningDays"
                                       required min="0" step="1"
                                       value="<c:out value='${settings.nearExpiryWarningDays}'/>">
                                <span class="input-suffix">ngày</span>
                            </div>
                            <span class="field-hint">Lô hết hạn trong khoảng số ngày này sẽ kích hoạt NEAR_EXPIRY. Ví dụ: 90 = cảnh báo trước 90 ngày hết hạn.</span>
                        </div>
                    </div>
                </fieldset>

                <div class="profile-actions">
                    <button type="submit" class="btn btn-primary">Lưu Cài đặt</button>
                    <a class="btn btn-ghost" href="${ctx}/inventory/alerts">Quay lại Cảnh báo</a>
                </div>
            </form>
        </div>
    </main>
</div>

<%@ include file="/WEB-INF/jspf/admin-footer.jspf" %>

<script>
    // Prevent duplicate submits (same pattern as other admin forms)
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
