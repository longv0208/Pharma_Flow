<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="inventory-adjustments"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Inventory Adjustments â€” PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <div class="section-head">
            <div>
                <h2>Inventory Adjustments</h2>
                <p class="section-sub">Manual corrections to batch on-hand stock — audit trail.</p>
            </div>
        </div>

        <c:if test="${param.ok == 'created'}">
            <div class="alert alert-success" role="status">Adjustment recorded.</div>
        </c:if>
        <c:if test="${param.err == 'notfound'}">
            <div class="alert alert-error" role="alert">Batch not found.</div>
        </c:if>

        <%-- Filter bar: GET keeps filters bookmarkable --%>
        <form class="filter-bar" method="get" action="${ctx}/inventory/adjustments">
            <input type="search" name="q" value="<c:out value='${param.q}'/>"
                   placeholder="Product name or SKUâ€¦" aria-label="Search product">
            <input type="search" name="batch" value="<c:out value='${param.batch}'/>"
                   placeholder="Batch numberâ€¦" aria-label="Search batch">
            <select name="reason" aria-label="Reason">
                <option value="">All reasons</option>
                <option value="DAMAGED"          ${param.reason == 'DAMAGED'          ? 'selected' : ''}>Damaged</option>
                <option value="LOST"             ${param.reason == 'LOST'             ? 'selected' : ''}>Lost</option>
                <option value="EXPIRED"          ${param.reason == 'EXPIRED'          ? 'selected' : ''}>Expired</option>
                <option value="COUNT_CORRECTION" ${param.reason == 'COUNT_CORRECTION' ? 'selected' : ''}>Count Correction</option>
                <option value="DATA_CORRECTION"  ${param.reason == 'DATA_CORRECTION'  ? 'selected' : ''}>Data Correction</option>
                <option value="OTHER"            ${param.reason == 'OTHER'            ? 'selected' : ''}>Other</option>
            </select>
            <input type="date" name="from" value="<c:out value='${param.from}'/>" aria-label="From date">
            <input type="date" name="to" value="<c:out value='${param.to}'/>" aria-label="To date">
            <button type="submit" class="btn btn-secondary btn-sm">Filter</button>
            <a href="${ctx}/inventory/adjustments" class="btn btn-ghost btn-sm">Reset</a>
        </form>

        <div class="admin-card">
            <c:choose>
                <c:when test="${empty adjustments}">
                    <div class="empty-state"><p>No adjustments found.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="admin-table admin-table-fixed">
                        <thead>
                        <tr>
                            <th>Date / Time</th>
                            <th>Product</th>
                            <th>Batch</th>
                            <th>Reason</th>
                            <th class="col-num">Qty Change</th>
                            <th class="col-num">Before</th>
                            <th class="col-num">After</th>
                            <th>Performed By</th>
                            <th>Note</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="a" items="${adjustments}">
                            <tr>
                                <td><fmt:formatDate value="${a.createdAt}" pattern="dd/MM/yyyy HH:mm"/></td>
                                <td><c:out value="${a.productName}"/></td>
                                <td>
                                    <a href="${ctx}/inventory?action=batch&id=${a.batchId}">
                                        <c:out value="${a.batchNumber}"/>
                                    </a>
                                </td>
                                <td><span class="status-badge mv-adjustment"><c:out value="${a.reasonLabel}"/></span></td>
                                <td><strong><c:out value="${a.quantityChangeLabel}"/></strong></td>
                                <td><c:out value="${a.quantityBefore}"/></td>
                                <td><c:out value="${a.quantityAfter}"/></td>
                                <td><c:out value="${a.performedByName}"/></td>
                                <td><c:out value="${empty a.note ? '—' : a.note}"/></td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>

                    <%-- Pagination --%>
                    <c:if test="${totalPages > 1}">
                        <nav class="pager" aria-label="Pages">
                            <c:forEach var="i" begin="1" end="${totalPages}">
                                <c:url var="pageUrl" value="/inventory/adjustments">
                                    <c:param name="q" value="${param.q}"/>
                                    <c:param name="batch" value="${param.batch}"/>
                                    <c:param name="reason" value="${param.reason}"/>
                                    <c:param name="from" value="${param.from}"/>
                                    <c:param name="to" value="${param.to}"/>
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
