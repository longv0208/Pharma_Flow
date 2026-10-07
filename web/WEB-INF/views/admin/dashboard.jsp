<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="dashboard"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Bảng điều khiển — Quản trị — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <div class="section-head">
            <div>
                <h2>Bảng điều khiển</h2>
                <p class="section-sub">Chào mừng trở lại, <c:out value="${sessionScope.currentUser.fullName}"/>.</p>
            </div>
        </div>

        <div class="dash-grid">
            <a class="dash-card" href="${ctx}/admin?action=products">
                <span class="dash-icon" aria-hidden="true">
                    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 8l-9-5-9 5v8l9 5 9-5V8z"/><path d="M3 8l9 5 9-5M12 13v8"/></svg>
                </span>
                <span class="dash-name">Sản phẩm</span>
                <span class="dash-desc">Danh mục hàng hóa, giá bán, loại OTC/RX</span>
                <span class="dash-go" aria-hidden="true"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M9 18l6-6-6-6"/></svg></span>
            </a>
            <a class="dash-card" href="${ctx}/admin?action=categories">
                <span class="dash-icon" aria-hidden="true">
                    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/><rect x="3" y="14" width="7" height="7" rx="1.5"/><rect x="14" y="14" width="7" height="7" rx="1.5"/></svg>
                </span>
                <span class="dash-name">Danh mục</span>
                <span class="dash-desc">Nhóm danh mục hiển thị trên cửa hàng</span>
                <span class="dash-go" aria-hidden="true"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M9 18l6-6-6-6"/></svg></span>
            </a>
            <a class="dash-card" href="${ctx}/admin?action=suppliers">
                <span class="dash-icon" aria-hidden="true">
                    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 8h14v9H1zM15 11h4l3 3v3h-7z"/><circle cx="6" cy="19" r="1.6"/><circle cx="18" cy="19" r="1.6"/></svg>
                </span>
                <span class="dash-name">Nhà cung cấp</span>
                <span class="dash-desc">Đối tác cung cấp cho đơn nhập hàng</span>
                <span class="dash-go" aria-hidden="true"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M9 18l6-6-6-6"/></svg></span>
            </a>
            <a class="dash-card" href="${ctx}/admin/purchase-orders">
                <span class="dash-icon" aria-hidden="true">
                    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 2h6l1 3h4v16H4V5h4l1-3z"/><path d="M9 12h6M9 16h4"/></svg>
                </span>
                <span class="dash-name">Đơn nhập hàng</span>
                <span class="dash-desc">Bản nháp và đơn đặt với nhà cung cấp</span>
                <span class="dash-go" aria-hidden="true"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M9 18l6-6-6-6"/></svg></span>
            </a>
            <a class="dash-card" href="${ctx}/inventory">
                <span class="dash-icon" aria-hidden="true">
                    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 8l-9-5-9 5v8l9 5 9-5V8z"/><path d="M3 8l9 5 9-5"/><path d="M12 13v8M9 15.5l2 2 4-4"/></svg>
                </span>
                <span class="dash-name">Tồn kho</span>
                <span class="dash-desc">Mức tồn kho, lô hàng và khả dụng</span>
                <span class="dash-go" aria-hidden="true"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M9 18l6-6-6-6"/></svg></span>
            </a>
            <a class="dash-card" href="${ctx}/inventory/receipts">
                <span class="dash-icon" aria-hidden="true">
                    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 8l-9-5-9 5v8l9 5 9-5V8z"/><path d="M3 8l9 5 9-5"/><path d="M12 13v8M9 15.5l2 2 4-4"/></svg>
                </span>
                <span class="dash-name">Nhập kho</span>
                <span class="dash-desc">Nhận và kiểm tra hàng từ nhà cung cấp</span>
                <span class="dash-go" aria-hidden="true"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M9 18l6-6-6-6"/></svg></span>
            </a>
            <a class="dash-card" href="${ctx}/inventory?action=history">
                <span class="dash-icon" aria-hidden="true">
                    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                </span>
                <span class="dash-name">Lịch sử tồn kho</span>
                <span class="dash-desc">Nhật ký tất cả biến động kho</span>
                <span class="dash-go" aria-hidden="true"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M9 18l6-6-6-6"/></svg></span>
            </a>
        </div>
        <p class="field-hint">Tài khoản nhân viên và báo cáo sẽ xuất hiện ở đây trong các giai đoạn sau.</p>
    </main>
</div>

<%@ include file="/WEB-INF/jspf/admin-footer.jspf" %>

</body>
</html>
