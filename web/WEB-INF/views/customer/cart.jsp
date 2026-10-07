<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Giỏ hàng — PharmaFlow</title>
    <link rel="stylesheet" href="${ctx}/css/main.css">
</head>
<body>

<%@ include file="/WEB-INF/jspf/header.jspf" %>

<section class="section">
    <div class="container">
        <div class="section-head">
            <div>
                <h2>Giỏ hàng của tôi</h2>
                <p class="section-sub">Kiểm tra lại sản phẩm và số lượng trước khi đặt hàng.</p>
            </div>
        </div>

        <%-- Flash messages --%>
        <c:if test="${not empty param.err}">
            <div class="alert alert-error" role="alert">
                <c:choose>
                    <c:when test="${param.err == 'empty'}">Giỏ hàng trống.</c:when>
                    <c:when test="${param.err == 'stock'}">Số lượng vượt quá tồn kho có thể bán.</c:when>
                    <c:when test="${param.err == 'token'}">Phiên đặt hàng đã hết hạn, vui lòng thử lại.</c:when>
                    <c:when test="${param.err == 'EMPTY_CART'}">Giỏ hàng trống.</c:when>
                    <c:when test="${param.err == 'INSUFFICIENT_STOCK'}">
                        Tồn kho đã thay đổi, vui lòng kiểm tra lại giỏ hàng.
                        <c:if test="${not empty param.msg}"> (<c:out value="${param.msg}"/>)</c:if>
                    </c:when>
                    <c:when test="${param.err == 'PRODUCT_INACTIVE'}">Sản phẩm không còn hoạt động.</c:when>
                    <c:when test="${param.err == 'PRODUCT_NOT_FOUND'}">Sản phẩm không tồn tại.</c:when>
                    <c:when test="${param.err == 'PRESCRIPTION_REQUIRED'}">Thuốc kê đơn không thể đặt trực tuyến.</c:when>
                    <c:when test="${param.err == 'RESTRICTED_NOT_ALLOWED'}">Sản phẩm này không được bán trực tuyến.</c:when>
                    <c:when test="${param.err == 'NOT_SELLABLE_ONLINE'}">Sản phẩm này không được bán trực tuyến.</c:when>
                    <c:otherwise>Đã có lỗi xảy ra, vui lòng thử lại.</c:otherwise>
                </c:choose>
            </div>
        </c:if>
        <c:if test="${not empty param.ok}">
            <div class="alert alert-success" role="status">
                <c:choose>
                    <c:when test="${param.ok == 'updated'}">Đã cập nhật số lượng.</c:when>
                    <c:when test="${param.ok == 'removed'}">Đã xóa sản phẩm khỏi giỏ.</c:when>
                    <c:when test="${param.ok == 'cleared'}">Đã xóa toàn bộ giỏ hàng.</c:when>
                    <c:otherwise>Thao tác thành công.</c:otherwise>
                </c:choose>
            </div>
        </c:if>

        <c:choose>
            <c:when test="${empty items}">
                <div class="empty-state">
                    <p>Giỏ hàng của bạn đang trống.</p>
                    <a class="btn btn-primary" href="${ctx}/products">Tiếp tục mua sắm</a>
                </div>
            </c:when>
            <c:otherwise>
                <div class="profile-card">
                    <table class="admin-table">
                        <thead>
                        <tr>
                            <th>Sản phẩm</th>
                            <th>SKU</th>
                            <th>Đơn vị</th>
                            <th class="col-num">Đơn giá</th>
                            <th>Số lượng</th>
                            <th class="col-num">Tồn có thể bán</th>
                            <th class="col-num">Thành tiền</th>
                            <th></th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="item" items="${items}">
                            <tr>
                                <td>
                                    <a href="${ctx}/products/${item.productId}">
                                        <c:out value="${item.productName}"/>
                                    </a>
                                    <c:if test="${item.unavailable}">
                                        <div class="field-hint" style="color:var(--red-500,#ef4444)">
                                            Sản phẩm này không còn bán trực tuyến — vui lòng xóa khỏi giỏ.
                                        </div>
                                    </c:if>
                                    <c:if test="${!item.unavailable and item.overStock}">
                                        <div class="field-hint" style="color:#92400e">
                                            Vượt quá tồn kho có thể bán — vui lòng giảm số lượng.
                                        </div>
                                    </c:if>
                                </td>
                                <td><c:out value="${item.sku}"/></td>
                                <td><c:out value="${item.sellingUnit}"/></td>
                                <td class="col-num">
                                    <fmt:formatNumber value="${item.sellingPrice}" type="number" groupingUsed="true"/>
                                </td>
                                <td>
                                    <form class="pos-inline-form" method="post" action="${ctx}/cart?action=update">
                                        <input type="hidden" name="productId" value="${item.productId}">
                                        <input class="pos-qty-input" type="number" name="quantity"
                                               min="0" step="1" value="${item.quantity}"
                                               ${item.unavailable ? 'disabled' : ''}>
                                        <button type="submit" class="btn btn-ghost btn-sm"
                                                ${item.unavailable ? 'disabled' : ''}>Cập nhật</button>
                                    </form>
                                </td>
                                <td class="col-num"><c:out value="${item.saleableQuantity}"/></td>
                                <td class="col-num">
                                    <fmt:formatNumber value="${item.subtotal}" type="number" groupingUsed="true"/>&#x20AB;
                                </td>
                                <td>
                                    <form method="post" action="${ctx}/cart?action=remove">
                                        <input type="hidden" name="productId" value="${item.productId}">
                                        <button type="submit" class="btn btn-danger btn-sm">Xóa</button>
                                    </form>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>

                    <div class="pos-total-row">
                        <span>Tổng cộng</span>
                        <span class="pos-total">
                            <fmt:formatNumber value="${cartTotal}" type="number" groupingUsed="true"/>&#x20AB;
                        </span>
                    </div>

                    <div class="profile-actions">
                        <a class="btn btn-primary btn-lg" href="${ctx}/orders?action=checkout">Đặt hàng</a>
                        <a class="btn btn-ghost" href="${ctx}/products">Tiếp tục mua sắm</a>
                        <form method="post" action="${ctx}/cart?action=clear"
                              onsubmit="return confirm('Xóa toàn bộ giỏ hàng?');">
                            <button type="submit" class="btn btn-danger">Xóa giỏ hàng</button>
                        </form>
                    </div>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</section>

<%@ include file="/WEB-INF/jspf/footer.jspf" %>

</body>
</html>
