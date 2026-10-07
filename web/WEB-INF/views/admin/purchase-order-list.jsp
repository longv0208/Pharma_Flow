<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="purchase-orders"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Đơn đặt hàng — Admin — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <div class="section-head">
            <div>
                <h2>Đơn đặt hàng</h2>
                <p class="section-sub">Danh sách thuốc nhà thuốc dự định mua từ nhà cung cấp. Kho chỉ thay đổi khi hàng được nhập.</p>
            </div>
            <a class="btn btn-primary" href="${ctx}/admin/purchase-orders?action=new">+ Tạo Đơn đặt hàng</a>
        </div>

        <%-- Filter bar: GET keeps filters bookmarkable --%>
        <form class="filter-bar" method="get" action="${ctx}/admin/purchase-orders">
            <input type="search" name="q" value="<c:out value='${param.q}'/>"
                   placeholder="Tìm theo mã đơn hoặc tên nhà cung cấp…" aria-label="Tìm kiếm đơn đặt hàng">
            <select name="status" aria-label="Trạng thái">
                <option value="">Tất cả trạng thái</option>
                <option value="DRAFT"              ${param.status == 'DRAFT'              ? 'selected' : ''}>Nháp</option>
                <option value="ORDERED"            ${param.status == 'ORDERED'            ? 'selected' : ''}>Đã đặt</option>
                <option value="PARTIALLY_RECEIVED" ${param.status == 'PARTIALLY_RECEIVED' ? 'selected' : ''}>Nhận một phần</option>
                <option value="RECEIVED"           ${param.status == 'RECEIVED'           ? 'selected' : ''}>Đã nhận</option>
                <option value="CANCELLED"          ${param.status == 'CANCELLED'          ? 'selected' : ''}>Đã hủy</option>
            </select>
            <button type="submit" class="btn btn-secondary btn-sm">Lọc</button>
        </form>

        <c:if test="${param.ok == 'created'}">
            <div class="alert alert-success" role="status">Đã lưu nháp đơn đặt hàng.</div>
        </c:if>
        <c:if test="${param.ok == 'updated'}">
            <div class="alert alert-success" role="status">Đã cập nhật đơn đặt hàng.</div>
        </c:if>
        <c:if test="${param.ok == 'placed'}">
            <div class="alert alert-success" role="status">Đã đặt hàng với nhà cung cấp.</div>
        </c:if>
        <c:if test="${param.ok == 'cancelled'}">
            <div class="alert alert-success" role="status">Đã hủy đơn đặt hàng.</div>
        </c:if>
        <c:if test="${param.err == 'notfound'}">
            <div class="alert alert-error" role="alert">Không tìm thấy đơn đặt hàng.</div>
        </c:if>
        <c:if test="${param.err == 'noteditable'}">
            <div class="alert alert-error" role="alert">Chỉ có đơn nháp mới có thể sửa hoặc đặt hàng.</div>
        </c:if>
        <c:if test="${param.err == 'notcancellable'}">
            <div class="alert alert-error" role="alert">Đơn đặt hàng này không thể hủy.</div>
        </c:if>
        <c:if test="${param.err == 'invalid'}">
            <div class="alert alert-error" role="alert">Bản nháp không còn hợp lệ — vui lòng kiểm tra và sửa trước khi đặt hàng.</div>
        </c:if>

        <div class="admin-card">
            <c:choose>
                <c:when test="${empty orders}">
                    <div class="empty-state"><p>Chưa có đơn đặt hàng nào.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="admin-table admin-table-fixed table-purchase-orders">
                        <thead>
                        <tr>
                            <th class="col-id">Mã đơn</th>
                            <th>Nhà cung cấp</th>
                            <th class="col-date">Ngày đặt</th>
                            <th class="col-date">Ngày giao dự kiến</th>
                            <th class="col-price">Tổng tiền</th>
                            <th class="col-source">Nguồn</th>
                            <th class="col-status">Trạng thái</th>
                            <th class="col-act">Thao tác</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="po" items="${orders}">
                            <tr>
                                <td>#<c:out value="${po.purchaseOrderId}"/></td>
                                <td><c:out value="${po.supplierName}"/></td>
                                <td><c:out value="${po.orderDate}"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${empty po.expectedDeliveryDate}">—</c:when>
                                        <c:otherwise><c:out value="${po.expectedDeliveryDate}"/></c:otherwise>
                                    </c:choose>
                                </td>
                                <td><fmt:formatNumber value="${po.totalAmount}" type="number" maxFractionDigits="0"/>&#x20AB;</td>
                                <td><c:out value="${po.sourceType}"/></td>
                                <td>
                                    <span class="status-badge ${po.statusCss}">
                                        <c:out value="${po.statusLabel}"/>
                                    </span>
                                </td>
                                <td class="col-actions">
                                    <a class="btn btn-secondary btn-sm"
                                       href="${ctx}/admin/purchase-orders?action=detail&id=${po.purchaseOrderId}">Xem</a>
                                    <c:if test="${po.editable}">
                                        <a class="btn btn-ghost btn-sm"
                                           href="${ctx}/admin/purchase-orders?action=edit&id=${po.purchaseOrderId}">Sửa</a>
                                    </c:if>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </c:otherwise>
            </c:choose>
        </div>
    </main>
</div>

<%@ include file="/WEB-INF/jspf/admin-footer.jspf" %>

</body>
</html>
