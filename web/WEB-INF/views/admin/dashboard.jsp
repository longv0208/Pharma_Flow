<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
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
            <a class="btn btn-ghost" href="${ctx}/reports">Xem báo cáo chi tiết</a>
        </div>

        <%-- ===== Live metric cards ===== --%>
        <div class="stat-grid">
            <div class="stat-card stat-ok">
                <span class="stat-label">Doanh thu hôm nay</span>
                <span class="stat-value">
                    <fmt:formatNumber value="${todaySummary.totalRevenue}" type="number" groupingUsed="true"/>&#x20AB;
                </span>
                <span class="stat-sub"><c:out value="${todaySummary.totalCount}"/> giao dịch hoàn tất</span>
            </div>
            <div class="stat-card">
                <span class="stat-label">Doanh thu tháng này</span>
                <span class="stat-value">
                    <fmt:formatNumber value="${monthSummary.totalRevenue}" type="number" groupingUsed="true"/>&#x20AB;
                </span>
                <span class="stat-sub"><c:out value="${monthSummary.totalCount}"/> giao dịch hoàn tất</span>
            </div>
            <a class="stat-card stat-link" href="${ctx}/reports">
                <span class="stat-label">Đơn online đang xử lý</span>
                <span class="stat-value"><c:out value="${orderStatus.inProgress}"/></span>
                <span class="stat-sub">Chờ xử lý → đang giao</span>
            </a>
            <a class="stat-card stat-link stat-ok" href="${ctx}/inventory">
                <span class="stat-label">Tồn kho có thể bán</span>
                <span class="stat-value"><c:out value="${inventory.saleable}"/></span>
                <span class="stat-sub"><c:out value="${inventory.physicalOnHand}"/> thực tế · <c:out value="${inventory.reserved}"/> đã giữ</span>
            </a>
            <a class="stat-card stat-link ${alertCounts[0] + alertCounts[1] > 0 ? 'stat-danger' : ''}" href="${ctx}/inventory/alerts">
                <span class="stat-label">Hết hàng / Tồn thấp</span>
                <span class="stat-value"><c:out value="${alertCounts[0] + alertCounts[1]}"/></span>
                <span class="stat-sub"><c:out value="${alertCounts[0]}"/> hết · <c:out value="${alertCounts[1]}"/> thấp</span>
            </a>
            <a class="stat-card stat-link ${alertCounts[2] + alertCounts[3] > 0 ? 'stat-warn' : ''}" href="${ctx}/inventory/alerts">
                <span class="stat-label">Lô sắp / hết hạn</span>
                <span class="stat-value"><c:out value="${alertCounts[2] + alertCounts[3]}"/></span>
                <span class="stat-sub"><c:out value="${alertCounts[2]}"/> sắp · <c:out value="${alertCounts[3]}"/> hết</span>
            </a>
        </div>

        <%-- ===== Compact detail sections ===== --%>
        <div class="admin-card" style="margin-bottom:18px;">
            <h3 style="margin:0 0 12px; font-size:15px;">Sản phẩm bán chạy tháng này</h3>
            <c:choose>
                <c:when test="${empty topMonth}">
                    <div class="empty-state"><p>Chưa có dữ liệu trong tháng này.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="admin-table admin-table-fixed">
                        <thead>
                        <tr>
                            <th>#</th>
                            <th>Sản phẩm</th>
                            <th>SKU</th>
                            <th class="col-num">Đã bán</th>
                            <th class="col-num">Doanh thu</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="p" items="${topMonth}" varStatus="st">
                            <tr>
                                <td><c:out value="${st.count}"/></td>
                                <td><c:out value="${p.productName}"/></td>
                                <td><c:out value="${p.sku}"/></td>
                                <td class="col-num"><c:out value="${p.totalQty}"/></td>
                                <td class="col-num"><fmt:formatNumber value="${p.revenue}" type="number" groupingUsed="true"/>&#x20AB;</td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </c:otherwise>
            </c:choose>
        </div>

        <div class="admin-card" style="margin-bottom:18px;">
            <h3 style="margin:0 0 12px; font-size:15px;">Đơn trực tuyến theo trạng thái</h3>
            <div class="stat-grid" style="margin-bottom:0;">
                <div class="stat-card"><span class="stat-label">Chờ xử lý</span><span class="stat-value"><c:out value="${orderStatus.choXuLy}"/></span></div>
                <div class="stat-card"><span class="stat-label">Đã xác nhận</span><span class="stat-value"><c:out value="${orderStatus.daXacNhan}"/></span></div>
                <div class="stat-card"><span class="stat-label">Đang chuẩn bị</span><span class="stat-value"><c:out value="${orderStatus.dangChuanBi}"/></span></div>
                <div class="stat-card"><span class="stat-label">Sẵn sàng</span><span class="stat-value"><c:out value="${orderStatus.sanSang}"/></span></div>
                <div class="stat-card"><span class="stat-label">Đang giao</span><span class="stat-value"><c:out value="${orderStatus.dangGiao}"/></span></div>
                <div class="stat-card"><span class="stat-label">Hoàn tất</span><span class="stat-value"><c:out value="${orderStatus.hoanTat}"/></span></div>
            </div>
        </div>

        <div class="dash-grid">
            <a class="dash-card" href="${ctx}/admin?action=products">
                <span class="dash-icon" aria-hidden="true">
                    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 8l-9-5-9 5v8l9 5 9-5V8z"/><path d="M3 8l9 5 9-5M12 13v8"/></svg>
                </span>
                <span class="dash-name">Sản phẩm</span>
                <span class="dash-desc">Danh mục hàng hóa, giá bán, loại không kê đơn / kê đơn</span>
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
            <a class="dash-card" href="${ctx}/admin/staff-accounts">
                <span class="dash-icon" aria-hidden="true">
                    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/></svg>
                </span>
                <span class="dash-name">Tài khoản nhân viên</span>
                <span class="dash-desc">Quản lý nhân viên và nhân viên giao hàng</span>
                <span class="dash-go" aria-hidden="true"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M9 18l6-6-6-6"/></svg></span>
            </a>
        </div>
    </main>
</div>

<%@ include file="/WEB-INF/jspf/admin-footer.jspf" %>

</body>
</html>
