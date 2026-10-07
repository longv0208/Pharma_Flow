<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="online-orders"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Đơn #${order.onlineOrderId} — Chuẩn bị đơn — PharmaFlow</title>
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
                    — lấy đúng các lô đã giữ bên dưới.
                </p>
            </div>
            <a class="btn btn-ghost" href="${ctx}/fulfillment">← Quay lại danh sách</a>
        </div>

        <c:if test="${not empty param.ok}">
            <div class="alert alert-success" role="status">
                <c:choose>
                    <c:when test="${param.ok == 'confirmed'}">Đã xác nhận đơn hàng.</c:when>
                    <c:when test="${param.ok == 'prepared'}">Đã bắt đầu chuẩn bị đơn hàng.</c:when>
                    <c:when test="${param.ok == 'ready'}">Đơn hàng đã sẵn sàng để giao.</c:when>
                    <c:when test="${param.ok == 'rejected'}">Đã từ chối đơn hàng và giải phóng hàng đã giữ.</c:when>
                    <c:otherwise>Thao tác thành công.</c:otherwise>
                </c:choose>
            </div>
        </c:if>
        <c:if test="${not empty param.err}">
            <div class="alert alert-error" role="alert">
                <c:choose>
                    <c:when test="${param.err == 'ORDER_NOT_FOUND'}">Không tìm thấy đơn hàng.</c:when>
                    <c:when test="${param.err == 'INVALID_STATUS'}">Đơn hàng không thể thực hiện thao tác này ở trạng thái hiện tại.</c:when>
                    <c:when test="${param.err == 'RESERVATION_MISMATCH'}">Dữ liệu giữ hàng của đơn không còn hợp lệ. Vui lòng kiểm tra tồn kho.</c:when>
                    <c:when test="${param.err == 'RESERVATION_NOT_FOUND'}">Đơn hàng không có dữ liệu giữ hàng. Vui lòng kiểm tra tồn kho.</c:when>
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
                <legend>Thông tin giao hàng</legend>
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
                        <label>Tỉnh / Thành phố</label>
                        <span><c:out value="${order.provinceCity}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Quận / Huyện</label>
                        <span><c:out value="${order.district}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Phường / Xã</label>
                        <span><c:out value="${order.ward}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Địa chỉ chi tiết</label>
                        <span><c:out value="${order.detailedAddress}"/></span>
                    </div>
                </div>
            </fieldset>

            <fieldset class="profile-group">
                <legend>Sản phẩm &amp; lô đã giữ</legend>
                <table class="admin-table">
                    <thead>
                    <tr>
                        <th>Sản phẩm</th>
                        <th>SKU</th>
                        <th class="col-num">Đã đặt</th>
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
                        <%-- Reserved batch allocations for this line — the picking list --%>
                        <tr class="pick-row">
                            <td colspan="6" class="pick-cell">
                                <c:forEach var="r" items="${reservations}">
                                    <c:if test="${r.onlineOrderItemId == item.onlineOrderItemId}">
                                        <div class="pick-line">
                                            <span class="pick-label">Lấy:</span>
                                            <span class="pick-batch">Lô <c:out value="${r.batchNumber}"/></span>
                                            <span class="pick-exp">HSD <fmt:formatDate value="${r.expiryDate}" pattern="yyyy-MM-dd"/></span>
                                            <span class="pick-loc"><c:out value="${r.storageLocation}"/></span>
                                            <span class="pick-qty"><c:out value="${r.reservedQuantity}"/></span>
                                            <c:if test="${r.status != 'DANG_GIU'}">
                                                <span class="pick-released">(đã giải phóng)</span>
                                            </c:if>
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
                <%-- Action buttons: only the valid state-changing ones per status --%>
                <c:if test="${order.orderStatus == 'CHO_XU_LY'}">
                    <form method="post" action="${ctx}/fulfillment?action=confirm" class="inline-form"
                          onsubmit="var c = confirm('Xác nhận đơn hàng này?'); if (c) { var b = this.querySelector('button'); if (b) b.disabled = true; } return c;">
                        <input type="hidden" name="orderId" value="${order.onlineOrderId}">
                        <button type="submit" class="btn btn-primary">Xác nhận đơn</button>
                    </form>
                    <form method="post" action="${ctx}/fulfillment?action=reject" class="inline-form"
                          onsubmit="var c = confirm('Từ chối đơn hàng này? Hàng đã giữ sẽ được giải phóng.'); if (c) { var b = this.querySelector('button'); if (b) b.disabled = true; } return c;">
                        <input type="hidden" name="orderId" value="${order.onlineOrderId}">
                        <button type="submit" class="btn btn-ghost btn-danger">Từ chối đơn</button>
                    </form>
                </c:if>
                <c:if test="${order.orderStatus == 'DA_XAC_NHAN'}">
                    <form method="post" action="${ctx}/fulfillment?action=prepare" class="inline-form"
                          onsubmit="var c = confirm('Bắt đầu chuẩn bị đơn hàng này?'); if (c) { var b = this.querySelector('button'); if (b) b.disabled = true; } return c;">
                        <input type="hidden" name="orderId" value="${order.onlineOrderId}">
                        <button type="submit" class="btn btn-primary">Bắt đầu chuẩn bị</button>
                    </form>
                </c:if>
                <c:if test="${order.orderStatus == 'DANG_CHUAN_BI'}">
                    <form method="post" action="${ctx}/fulfillment?action=ready" class="inline-form"
                          onsubmit="var c = confirm('Đánh dấu đơn hàng đã sẵn sàng?'); if (c) { var b = this.querySelector('button'); if (b) b.disabled = true; } return c;">
                        <input type="hidden" name="orderId" value="${order.onlineOrderId}">
                        <button type="submit" class="btn btn-primary">Đánh dấu sẵn sàng</button>
                    </form>
                </c:if>
                <c:if test="${order.orderStatus == 'SAN_SANG'}">
                    <p class="field-hint">Đơn hàng đang chờ bàn giao cho nhân viên giao hàng.</p>
                </c:if>
            </div>
        </div>
    </main>
</div>

<%@ include file="/WEB-INF/jspf/admin-footer.jspf" %>

</body>
</html>
