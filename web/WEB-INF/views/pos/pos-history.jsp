<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="pos"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Lịch sử bán hàng — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <nav class="crumbs" aria-label="Đường dẫn">
            <span>Bán hàng</span>
            <span aria-hidden="true">›</span>
            <span>Lịch sử</span>
        </nav>

        <div class="section-head">
            <div>
                <h2>Lịch sử bán hàng</h2>
                <p class="section-sub">Mọi đơn bán tại quầy đã hoàn tất — mới nhất trước.</p>
            </div>
            <a class="btn btn-primary btn-sm" href="${ctx}/pos">Đơn bán mới</a>
        </div>

        <c:if test="${param.err == 'notfound'}">
            <div class="alert alert-error">Không tìm thấy đơn bán.</div>
        </c:if>

        <%-- Filter bar --%>
        <form class="filter-bar" method="get" action="${ctx}/pos">
            <input type="hidden" name="action" value="history">
            <select name="payment" aria-label="Phương thức thanh toán">
                <option value="">Tất cả phương thức</option>
                <c:forEach var="m" items="${paymentMethods}">
                    <option value="${m}" ${payment == m ? 'selected' : ''}>
                        <c:choose>
                            <c:when test="${m == 'TIEN_MAT'}">Tiền mặt</c:when>
                            <c:when test="${m == 'CHUYEN_KHOAN'}">Chuyển khoản</c:when>
                            <c:otherwise>Thẻ</c:otherwise>
                        </c:choose>
                    </option>
                </c:forEach>
            </select>
            <input type="date" name="from" value="<c:out value='${from}'/>" aria-label="Từ ngày">
            <input type="date" name="to" value="<c:out value='${to}'/>" aria-label="Đến ngày">
            <button type="submit" class="btn btn-secondary btn-sm">Lọc</button>
            <a href="${ctx}/pos?action=history" class="btn btn-ghost btn-sm">Đặt lại</a>
        </form>

        <div class="admin-card">
            <c:choose>
                <c:when test="${empty sales}">
                    <div class="empty-state"><p>Không tìm thấy đơn bán nào.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="admin-table admin-table-fixed">
                        <thead>
                        <tr>
                            <th>Mã đơn</th>
                            <th>Ngày / Giờ</th>
                            <th>Nhân viên</th>
                            <th>Thanh toán</th>
                            <th>Đơn thuốc</th>
                            <th>Trạng thái</th>
                            <th class="col-num">Tổng tiền</th>
                            <th></th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="s" items="${sales}">
                            <tr>
                                <td>#<c:out value="${s.saleTransactionId}"/></td>
                                <td><fmt:formatDate value="${s.saleDatetime}" pattern="yyyy-MM-dd HH:mm"/></td>
                                <td><c:out value="${s.staffName}"/></td>
                                <td><c:out value="${s.paymentLabel}"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${not empty s.prescriptionId}">Đã kiểm tra</c:when>
                                        <c:otherwise>—</c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <span class="status-badge ${s.statusCss}"><c:out value="${s.statusLabel}"/></span>
                                </td>
                                <td class="col-num"><fmt:formatNumber value="${s.totalAmount}" type="number" groupingUsed="true"/></td>
                                <td>
                                    <a class="btn btn-ghost btn-sm"
                                       href="${ctx}/pos?action=detail&id=${s.saleTransactionId}">Xem</a>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>

                    <%-- Pagination --%>
                    <c:if test="${totalPages > 1}">
                        <nav class="pager" aria-label="Trang">
                            <c:forEach var="i" begin="1" end="${totalPages}">
                                <c:url var="pageUrl" value="/pos">
                                    <c:param name="action" value="history"/>
                                    <c:param name="payment" value="${payment}"/>
                                    <c:param name="from" value="${from}"/>
                                    <c:param name="to" value="${to}"/>
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
