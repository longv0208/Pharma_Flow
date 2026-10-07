<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="delivery"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Đơn #${order.onlineOrderId} — Giao hàng — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <div class="section-head">
            <div>
                <h2>Đơn hàng #<c:out value="${order.onlineOrderId}"/></h2>
                <p class="section-sub">
                    Đặt lúc <fmt:formatDate value="${order.createdAt}" pattern="yyyy-MM-dd HH:mm"/>
                    — giao đúng các sản phẩm bên dưới.
                </p>
            </div>
            <a class="btn btn-ghost" href="${ctx}/delivery">← Quay lại danh sách</a>
        </div>

        <c:if test="${not empty param.ok}">
            <div class="alert alert-success" role="status">
                <c:choose>
                    <c:when test="${param.ok == 'started'}">Đã bắt đầu giao hàng và xuất kho thành công.</c:when>
                    <c:when test="${param.ok == 'completed'}">Đã xác nhận giao hàng thành công.</c:when>
                    <c:otherwise>Thao tác thành công.</c:otherwise>
                </c:choose>
            </div>
        </c:if>
        <c:if test="${not empty param.err}">
            <div class="alert alert-error" role="alert">
                <c:choose>
                    <c:when test="${param.err == 'ORDER_NOT_FOUND'}">Không tìm thấy đơn hàng.</c:when>
                    <c:when test="${param.err == 'INVALID_STATUS'}">Đơn hàng không thể thực hiện thao tác này ở trạng thái hiện tại.</c:when>
                    <c:when test="${param.err == 'RESERVATION_NOT_FOUND'}">Không tìm thấy dữ liệu hàng đã giữ của đơn.</c:when>
                    <c:when test="${param.err == 'RESERVATION_MISMATCH'}">Số lượng hàng đã giữ không khớp với đơn hàng.</c:when>
                    <c:when test="${param.err == 'BATCH_NOT_FOUND'}">Không tìm thấy lô hàng đã giữ của đơn.</c:when>
                    <c:when test="${param.err == 'BATCH_INSUFFICIENT'}">Tồn kho của lô không còn đủ để xuất giao.</c:when>
                    <c:when test="${param.err == 'BATCH_NOT_DISPATCHABLE'}">Lô hàng đã hết hạn hoặc không còn đủ điều kiện xuất giao.</c:when>
                    <c:when test="${param.err == 'FULFILLMENT_MISMATCH'}">Dữ liệu xuất kho của đơn chưa hoàn tất.</c:when>
                    <c:otherwise>Đã có lỗi xảy ra, vui lòng thử lại.</c:otherwise>
                </c:choose>
            </div>
        </c:if>

        <div class="profile-card">
            <fieldset class="profile-group">
                <legend>Thông tin đơn hàng</legend>
                <div class="profile-grid">
                    <div class="form-field">
                        <label>Mã đơn</label>
                        <span>#<c:out value="${order.onlineOrderId}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Ngày đặt</label>
                        <span><fmt:formatDate value="${order.createdAt}" pattern="yyyy-MM-dd HH:mm"/></span>
                    </div>
                    <div class="form-field">
                        <label>Trạng thái</label>
                        <span class="status-badge ${order.statusCss}">
                            <c:out value="${order.statusLabel}"/>
                        </span>
                    </div>
                    <div class="form-field">
                        <label>Thanh toán</label>
                        <span>
                            <c:out value="${order.paymentLabel}"/>
                            <span class="field-hint">(<c:out value="${order.paymentStatusLabel}"/>)</span>
                        </span>
                    </div>
                    <div class="form-field">
                        <label>Tổng tiền</label>
                        <span><fmt:formatNumber value="${order.totalAmount}" type="number" groupingUsed="true"/>&#x20AB;</span>
                    </div>
                </div>
            </fieldset>

            <fieldset class="profile-group">
                <legend>Thông tin người nhận</legend>
                <div class="profile-grid">
                    <div class="form-field">
                        <label>Người nhận</label>
                        <span><c:out value="${order.customerName}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Số điện thoại</label>
                        <span><c:out value="${order.customerPhone}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Địa chỉ giao</label>
                        <span><c:out value="${order.fullAddress}"/></span>
                    </div>
                </div>
            </fieldset>

            <fieldset class="profile-group">
                <legend>Sản phẩm cần giao</legend>
                <table class="admin-table">
                    <thead>
                    <tr>
                        <th>Sản phẩm</th>
                        <th>SKU</th>
                        <th class="col-num">Số lượng</th>
                        <th>Đơn vị</th>
                        <th class="col-price">Đơn giá</th>
                        <th class="col-price">Thành tiền</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="item" items="${items}">
                        <tr>
                            <td><c:out value="${item.productName}"/></td>
                            <td><c:out value="${item.sku}"/></td>
                            <td><c:out value="${item.quantity}"/></td>
                            <td><c:out value="${item.sellingUnit}"/></td>
                            <td><fmt:formatNumber value="${item.unitPrice}" type="number" groupingUsed="true"/>&#x20AB;</td>
                            <td><fmt:formatNumber value="${item.subtotal}" type="number" groupingUsed="true"/>&#x20AB;</td>
                        </tr>
                        <%-- Reserved batch allocations — read-only, shipper never picks --%>
                        <tr class="pick-row">
                            <td colspan="6" class="pick-cell">
                                <c:forEach var="r" items="${reservations}">
                                    <c:if test="${r.onlineOrderItemId == item.onlineOrderItemId}">
                                        <div class="pick-line">
                                            <span class="pick-label">Lô:</span>
                                            <span class="pick-batch">Lô <c:out value="${r.batchNumber}"/></span>
                                            <span class="pick-exp">HSD <fmt:formatDate value="${r.expiryDate}" pattern="yyyy-MM-dd"/></span>
                                            <span class="pick-loc"><c:out value="${r.storageLocation}"/></span>
                                            <span class="pick-qty"><c:out value="${r.reservedQuantity}"/></span>
                                        </div>
                                    </c:if>
                                </c:forEach>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </fieldset>

            <div class="profile-actions">
                <%-- Only the valid state-changing action per status --%>
                <c:if test="${order.orderStatus == 'SAN_SANG'}">
                    <form method="post" action="${ctx}/delivery?action=start" class="inline-form"
                          onsubmit="var c = confirm('Bắt đầu giao đơn hàng này? Hệ thống sẽ xuất kho ngay.'); if (c) { var b = this.querySelector('button'); if (b) b.disabled = true; } return c;">
                        <input type="hidden" name="orderId" value="${order.onlineOrderId}">
                        <button type="submit" class="btn btn-primary">Bắt đầu giao</button>
                    </form>
                </c:if>
                <c:if test="${order.orderStatus == 'DANG_GIAO'}">
                    <form method="post" action="${ctx}/delivery?action=complete" class="inline-form"
                          onsubmit="var c = confirm('Xác nhận đã giao đơn hàng này thành công?'); if (c) { var b = this.querySelector('button'); if (b) b.disabled = true; } return c;">
                        <input type="hidden" name="orderId" value="${order.onlineOrderId}">
                        <button type="submit" class="btn btn-primary">Xác nhận đã giao</button>
                    </form>
                </c:if>
                <c:if test="${order.orderStatus == 'HOAN_TAT'}">
                    <p class="field-hint">Đơn hàng đã hoàn tất.</p>
                </c:if>
            </div>
        </div>
    </main>
</div>

<%@ include file="/WEB-INF/jspf/admin-footer.jspf" %>

</body>
</html>
