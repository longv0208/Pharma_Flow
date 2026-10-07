<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đơn hàng của tôi — PharmaFlow</title>
    <link rel="stylesheet" href="${ctx}/css/main.css">
</head>
<body>

<%@ include file="/WEB-INF/jspf/header.jspf" %>

<section class="section">
    <div class="container">
        <div class="section-head">
            <div>
                <h2>Đơn hàng của tôi</h2>
                <p class="section-sub">${total} đơn hàng gần nhất.</p>
            </div>
        </div>

        <c:if test="${param.err == 'notfound'}">
            <div class="alert alert-error" role="alert">Không tìm thấy đơn hàng.</div>
        </c:if>

        <c:choose>
            <c:when test="${empty orders}">
                <div class="empty-state">
                    <p>Bạn chưa có đơn hàng nào.</p>
                    <a class="btn btn-primary" href="${ctx}/products">Tiếp tục mua sắm</a>
                </div>
            </c:when>
            <c:otherwise>
                <div class="profile-card">
                    <table class="admin-table">
                        <thead>
                        <tr>
                            <th>Mã đơn</th>
                            <th>Ngày đặt</th>
                            <th>Thanh toán</th>
                            <th>Trạng thái</th>
                            <th class="col-num">Tổng tiền</th>
                            <th></th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="o" items="${orders}">
                            <tr>
                                <td>#<c:out value="${o.onlineOrderId}"/></td>
                                <td><fmt:formatDate value="${o.createdAt}" pattern="yyyy-MM-dd HH:mm"/></td>
                                <td><c:out value="${o.paymentLabel}"/></td>
                                <td>
                                    <span class="status-badge ${o.statusCss}">
                                        <c:out value="${o.statusLabel}"/>
                                    </span>
                                </td>
                                <td class="col-num">
                                    <fmt:formatNumber value="${o.totalAmount}" type="number" groupingUsed="true"/>&#x20AB;
                                </td>
                                <td>
                                    <a class="btn btn-ghost btn-sm"
                                       href="${ctx}/orders?action=detail&id=${o.onlineOrderId}">Xem</a>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>

                    <c:if test="${totalPages > 1}">
                        <nav class="pager" aria-label="Trang">
                            <c:forEach var="i" begin="1" end="${totalPages}">
                                <c:url var="pageUrl" value="/orders">
                                    <c:param name="page" value="${i}"/>
                                </c:url>
                                <a class="pager-num ${i == page ? 'current' : ''}" href="${pageUrl}">${i}</a>
                            </c:forEach>
                        </nav>
                    </c:if>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</section>

<%@ include file="/WEB-INF/jspf/footer.jspf" %>

</body>
</html>
