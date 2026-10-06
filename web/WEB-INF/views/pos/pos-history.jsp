<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="pos"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Sale History — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <nav class="crumbs" aria-label="Breadcrumb">
            <span>Sales</span>
            <span aria-hidden="true">›</span>
            <span>History</span>
        </nav>

        <div class="section-head">
            <div>
                <h2>Sale History</h2>
                <p class="section-sub">Every completed counter sale — newest first.</p>
            </div>
            <a class="btn btn-primary btn-sm" href="${ctx}/pos">New Sale</a>
        </div>

        <c:if test="${param.err == 'notfound'}">
            <div class="alert alert-error">Sale not found.</div>
        </c:if>

        <%-- Filter bar --%>
        <form class="filter-bar" method="get" action="${ctx}/pos">
            <input type="hidden" name="action" value="history">
            <select name="payment" aria-label="Payment method">
                <option value="">All payments</option>
                <c:forEach var="m" items="${paymentMethods}">
                    <option value="${m}" ${payment == m ? 'selected' : ''}>
                        <c:choose>
                            <c:when test="${m == 'CASH'}">Cash</c:when>
                            <c:when test="${m == 'BANK_TRANSFER'}">Bank Transfer</c:when>
                            <c:otherwise>Card</c:otherwise>
                        </c:choose>
                    </option>
                </c:forEach>
            </select>
            <input type="date" name="from" value="<c:out value='${from}'/>" aria-label="From date">
            <input type="date" name="to" value="<c:out value='${to}'/>" aria-label="To date">
            <button type="submit" class="btn btn-secondary btn-sm">Filter</button>
            <a href="${ctx}/pos?action=history" class="btn btn-ghost btn-sm">Reset</a>
        </form>

        <div class="admin-card">
            <c:choose>
                <c:when test="${empty sales}">
                    <div class="empty-state"><p>No sales found.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="admin-table admin-table-fixed">
                        <thead>
                        <tr>
                            <th>Sale #</th>
                            <th>Date / Time</th>
                            <th>Staff</th>
                            <th>Payment</th>
                            <th>Rx</th>
                            <th>Status</th>
                            <th class="col-num">Total</th>
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
                                    <c:if test="${not empty s.prescriptionCode}">
                                        <c:out value="${s.prescriptionCode}"/>
                                    </c:if>
                                </td>
                                <td>
                                    <span class="status-badge ${s.statusCss}"><c:out value="${s.statusLabel}"/></span>
                                </td>
                                <td class="col-num"><fmt:formatNumber value="${s.totalAmount}" type="number" groupingUsed="true"/></td>
                                <td>
                                    <a class="btn btn-ghost btn-sm"
                                       href="${ctx}/pos?action=detail&id=${s.saleTransactionId}">View</a>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>

                    <%-- Pagination --%>
                    <c:if test="${totalPages > 1}">
                        <nav class="pager" aria-label="Pages">
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
