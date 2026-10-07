<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="delivery"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Đơn giao hàng — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <nav class="crumbs" aria-label="Đường dẫn">
            <span>Giao hàng</span>
            <span aria-hidden="true">›</span>
            <span>Đơn giao hàng</span>
        </nav>

        <div class="section-head">
            <div>
                <h2>Đơn giao hàng</h2>
                <p class="section-sub">${total} đơn — đang giao trước, sau đó sẵn sàng, rồi hoàn tất.</p>
            </div>
        </div>

        <c:if test="${param.err == 'ORDER_NOT_FOUND'}">
            <div class="alert alert-error" role="alert">Không tìm thấy đơn hàng.</div>
        </c:if>

        <%-- Filter bar — only the delivery-relevant statuses are offered --%>
        <form class="filter-bar" method="get" action="${ctx}/delivery">
            <input type="search" name="q" value="<c:out value='${q}'/>"
                   placeholder="Mã đơn, tên hoặc SĐT người nhận…" aria-label="Tìm kiếm đơn">
            <select name="status" aria-label="Trạng thái">
                <option value="">Tất cả trạng thái</option>
                <option value="SAN_SANG"  ${status == 'SAN_SANG'  ? 'selected' : ''}>Sẵn sàng</option>
                <option value="DANG_GIAO" ${status == 'DANG_GIAO' ? 'selected' : ''}>Đang giao</option>
                <option value="HOAN_TAT"  ${status == 'HOAN_TAT'  ? 'selected' : ''}>Hoàn tất</option>
            </select>
            <button type="submit" class="btn btn-secondary btn-sm">Lọc</button>
            <a href="${ctx}/delivery" class="btn btn-ghost btn-sm">Đặt lại</a>
        </form>

        <div class="admin-card">
            <c:choose>
                <c:when test="${empty orders}">
                    <div class="empty-state"><p>Không có đơn giao hàng nào.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="admin-table admin-table-fixed">
                        <thead>
                        <tr>
                            <th>Mã đơn</th>
                            <th>Người nhận</th>
                            <th>Số điện thoại</th>
                            <th>Địa chỉ</th>
                            <th class="col-num">Tổng tiền</th>
                            <th>Thanh toán</th>
                            <th>Trạng thái</th>
                            <th></th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="o" items="${orders}">
                            <tr>
                                <td>#<c:out value="${o.onlineOrderId}"/></td>
                                <td><c:out value="${o.customerName}"/></td>
                                <td><c:out value="${o.customerPhone}"/></td>
                                <td><c:out value="${o.fullAddress}"/></td>
                                <td class="col-num">
                                    <fmt:formatNumber value="${o.totalAmount}" type="number" groupingUsed="true"/>&#x20AB;
                                </td>
                                <td><c:out value="${o.paymentLabel}"/></td>
                                <td>
                                    <span class="status-badge ${o.statusCss}">
                                        <c:out value="${o.statusLabel}"/>
                                    </span>
                                </td>
                                <td>
                                    <a class="btn btn-ghost btn-sm"
                                       href="${ctx}/delivery?action=detail&id=${o.onlineOrderId}">Xem</a>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>

                    <%-- Pagination --%>
                    <c:if test="${totalPages > 1}">
                        <nav class="pager" aria-label="Trang">
                            <c:forEach var="i" begin="1" end="${totalPages}">
                                <c:url var="pageUrl" value="/delivery">
                                    <c:param name="q" value="${q}"/>
                                    <c:param name="status" value="${status}"/>
                                    <c:param name="page" value="${i}"/>
                                </c:url>
                                <a class="pager-num ${i == page ? 'current' : ''}" href="${pageUrl}">${i}</a>
                            </c:forEach>
                        </nav>
                    </c:if>
                </c:otherwise>
            </c:choose>
        </div>
    </main>
</div>

<%@ include file="/WEB-INF/jspf/admin-footer.jspf" %>

</body>
</html>
