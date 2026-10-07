<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="purchase-orders"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Đơn #${order.purchaseOrderId} — Admin — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <div class="section-head">
            <div>
                <h2>Đơn đặt hàng #<c:out value="${order.purchaseOrderId}"/></h2>
                <p class="section-sub">Chế độ xem. Số lượng nhận được cập nhật sau qua chức năng Nhập kho.</p>
            </div>
            <a class="btn btn-ghost" href="${ctx}/admin/purchase-orders">← Quay lại danh sách</a>
        </div>

        <c:if test="${param.err == 'notcancellable'}">
            <div class="alert alert-error" role="alert">Đơn đặt hàng này không thể hủy.</div>
        </c:if>
        <c:if test="${param.err == 'noteditable'}">
            <div class="alert alert-error" role="alert">Chỉ có đơn nháp mới có thể sửa.</div>
        </c:if>

        <div class="profile-card">
            <fieldset class="profile-group">
                <legend>Thông tin đơn đặt hàng</legend>
                <div class="profile-grid">
                    <div class="form-field">
                        <label>Mã đơn</label>
                        <span>#<c:out value="${order.purchaseOrderId}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Nhà cung cấp</label>
                        <span><c:out value="${order.supplierName}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Người tạo</label>
                        <span><c:out value="${order.createdByName}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Ngày đặt</label>
                        <span><c:out value="${order.orderDate}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Ngày giao dự kiến</label>
                        <span>
                            <c:choose>
                                <c:when test="${empty order.expectedDeliveryDate}">—</c:when>
                                <c:otherwise><c:out value="${order.expectedDeliveryDate}"/></c:otherwise>
                            </c:choose>
                        </span>
                    </div>
                    <div class="form-field">
                        <label>Trạng thái</label>
                        <span class="status-badge ${order.statusCss}">
                            <c:out value="${order.statusLabel}"/>
                        </span>
                    </div>
                    <div class="form-field">
                        <label>Nguồn</label>
                        <span><c:out value="${order.sourceType}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Ngày tạo</label>
                        <span><c:out value="${order.createdAt}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Tổng tiền</label>
                        <span><fmt:formatNumber value="${order.totalAmount}" type="number" maxFractionDigits="0"/>&#x20AB;</span>
                    </div>
                </div>
                <c:if test="${not empty order.note}">
                    <div class="form-field">
                        <label>Ghi chú</label>
                        <span><c:out value="${order.note}"/></span>
                    </div>
                </c:if>
            </fieldset>

            <fieldset class="profile-group">
                <legend>Sản phẩm đặt hàng</legend>
                <table class="admin-table">
                    <thead>
                    <tr>
                        <th>Sản phẩm</th>
                        <th>SKU</th>
                        <th class="col-num">Đã đặt</th>
                        <th class="col-num">Đã nhận</th>
                        <th class="col-price">Đơn giá</th>
                        <th class="col-price">Thành tiền</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="item" items="${items}">
                        <tr>
                            <td><c:out value="${item.productName}"/></td>
                            <td><c:out value="${item.sku}"/></td>
                            <td><c:out value="${item.orderedQuantity}"/></td>
                            <td><c:out value="${item.receivedQuantity}"/></td>
                            <td><fmt:formatNumber value="${item.unitCost}" type="number" maxFractionDigits="0"/>&#x20AB;</td>
                            <td><fmt:formatNumber value="${item.subtotal}" type="number" maxFractionDigits="0"/>&#x20AB;</td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </fieldset>

            <div class="profile-actions">
                <c:if test="${order.editable}">
                    <a class="btn btn-secondary" href="${ctx}/admin/purchase-orders?action=edit&id=${order.purchaseOrderId}">Sửa</a>
                    <form method="post" action="${ctx}/admin/purchase-orders?action=place" class="inline-form"
                          onsubmit="return confirm('Đặt hàng với nhà cung cấp? Đơn sẽ chuyển sang chế độ chỉ đọc.');">
                        <input type="hidden" name="purchaseOrderId" value="${order.purchaseOrderId}">
                        <button type="submit" class="btn btn-primary">Đặt hàng</button>
                    </form>
                </c:if>
                <c:if test="${order.cancellable}">
                    <form method="post" action="${ctx}/admin/purchase-orders?action=cancel" class="inline-form"
                          onsubmit="return confirm('Hủy đơn đặt hàng này? Hành động không thể hoàn tác.');">
                        <input type="hidden" name="purchaseOrderId" value="${order.purchaseOrderId}">
                        <button type="submit" class="btn btn-ghost btn-danger">Hủy đơn</button>
                    </form>
                </c:if>
            </div>
        </div>
    </main>
</div>

<%@ include file="/WEB-INF/jspf/admin-footer.jspf" %>

</body>
</html>
