<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đơn hàng #${order.onlineOrderId} — PharmaFlow</title>
    <link rel="stylesheet" href="${ctx}/css/main.css">
</head>
<body>

<%@ include file="/WEB-INF/jspf/header.jspf" %>

<section class="section">
    <div class="container">
        <div class="section-head">
            <div>
                <h2>Đơn hàng #<c:out value="${order.onlineOrderId}"/></h2>
                <p class="section-sub">
                    Đặt lúc <fmt:formatDate value="${order.createdAt}" pattern="yyyy-MM-dd HH:mm"/>
                </p>
            </div>
            <a class="btn btn-ghost btn-sm" href="${ctx}/orders">&larr; Đơn hàng của tôi</a>
        </div>

        <c:if test="${not empty param.ok}">
            <div class="alert alert-success" role="status">
                <c:choose>
                    <c:when test="${param.ok == 'placed'}">Đặt hàng thành công. Chúng tôi sẽ liên hệ để xác nhận.</c:when>
                    <c:when test="${param.ok == 'cancelled'}">Đã hủy đơn hàng.</c:when>
                    <c:otherwise>Thao tác thành công.</c:otherwise>
                </c:choose>
            </div>
        </c:if>
        <c:if test="${not empty param.err}">
            <div class="alert alert-error" role="alert">
                <c:choose>
                    <c:when test="${param.err == 'NOT_CANCELLABLE'}">Đơn hàng không thể hủy ở trạng thái hiện tại.</c:when>
                    <c:when test="${param.err == 'ORDER_NOT_FOUND'}">Không tìm thấy đơn hàng.</c:when>
                    <c:otherwise>Đã có lỗi xảy ra, vui lòng thử lại.</c:otherwise>
                </c:choose>
            </div>
        </c:if>

        <div class="profile-card">
            <table class="admin-table">
                <tbody>
                <tr>
                    <th style="width:220px">Trạng thái</th>
                    <td>
                        <span class="status-badge ${order.statusCss}">
                            <c:out value="${order.statusLabel}"/>
                        </span>
                    </td>
                </tr>
                <tr>
                    <th>Thanh toán</th>
                    <td>
                        <c:out value="${order.paymentLabel}"/>
                        <span class="field-hint">(<c:out value="${order.paymentStatusLabel}"/>)</span>
                    </td>
                </tr>
                <tr>
                    <th>Người nhận</th>
                    <td><c:out value="${order.customerName}"/></td>
                </tr>
                <tr>
                    <th>Điện thoại</th>
                    <td><c:out value="${order.customerPhone}"/></td>
                </tr>
                <tr>
                    <th>Địa chỉ giao hàng</th>
                    <td><c:out value="${order.fullAddress}"/></td>
                </tr>
                </tbody>
            </table>
        </div>

        <div class="profile-card" style="margin-top:24px">
            <table class="admin-table">
                <thead>
                <tr>
                    <th>Sản phẩm</th>
                    <th>SKU</th>
                    <th>Đơn vị</th>
                    <th class="col-num">Đơn giá</th>
                    <th class="col-num">Số lượng</th>
                    <th class="col-num">Thành tiền</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="item" items="${items}">
                    <tr>
                        <td>
                            <a href="${ctx}/products/${item.productId}">
                                <c:out value="${item.productName}"/>
                            </a>
                        </td>
                        <td><c:out value="${item.sku}"/></td>
                        <td><c:out value="${item.sellingUnit}"/></td>
                        <td class="col-num">
                            <fmt:formatNumber value="${item.unitPrice}" type="number" groupingUsed="true"/>
                        </td>
                        <td class="col-num"><c:out value="${item.quantity}"/></td>
                        <td class="col-num">
                            <fmt:formatNumber value="${item.subtotal}" type="number" groupingUsed="true"/>&#x20AB;
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>

            <div class="pos-total-row">
                <span>Tổng cộng</span>
                <span class="pos-total">
                    <fmt:formatNumber value="${order.totalAmount}" type="number" groupingUsed="true"/>&#x20AB;
                </span>
            </div>

            <c:if test="${order.cancellable}">
                <div class="profile-actions">
                    <form method="post" action="${ctx}/orders?action=cancel"
                          onsubmit="return confirm('Bạn chắc chắn muốn hủy đơn hàng này?');">
                        <input type="hidden" name="orderId" value="${order.onlineOrderId}">
                        <button type="submit" class="btn btn-danger">Hủy đơn hàng</button>
                    </form>
                </div>
            </c:if>
        </div>
    </div>
</section>

<%@ include file="/WEB-INF/jspf/footer.jspf" %>

</body>
</html>
