<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="inventory-history"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Inventory History — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <nav class="crumbs" aria-label="Breadcrumb">
            <a href="${ctx}/inventory">Inventory</a>
            <span aria-hidden="true">›</span>
            <span>History</span>
        </nav>

        <div class="section-head">
            <div>
                <h2>Inventory History</h2>
                <p class="section-sub">Complete chronological audit trail of stock movements.</p>
            </div>
        </div>

        <%-- Filter bar --%>
        <form class="filter-bar" method="get" action="${ctx}/inventory">
            <input type="hidden" name="action" value="history">
            <input type="search" name="q" value="<c:out value='${param.q}'/>"
                   placeholder="Product name or SKU…" aria-label="Search product">
            <input type="search" name="batch" value="<c:out value='${param.batch}'/>"
                   placeholder="Batch number…" aria-label="Search batch">
            <select name="type" aria-label="Movement type">
                <option value="">All types</option>
                <option value="STOCK_RECEIPT"       ${param.type == 'STOCK_RECEIPT'       ? 'selected' : ''}>Stock Receipt</option>
                <option value="POS_SALE"            ${param.type == 'POS_SALE'            ? 'selected' : ''}>POS Sale</option>
                <option value="ONLINE_RESERVATION"  ${param.type == 'ONLINE_RESERVATION'  ? 'selected' : ''}>Online Reservation</option>
                <option value="RESERVATION_RELEASE" ${param.type == 'RESERVATION_RELEASE' ? 'selected' : ''}>Reservation Release</option>
                <option value="ONLINE_SALE"         ${param.type == 'ONLINE_SALE'         ? 'selected' : ''}>Online Sale</option>
                <option value="ADJUSTMENT"          ${param.type == 'ADJUSTMENT'          ? 'selected' : ''}>Adjustment</option>
                <option value="STOCKTAKE_ADJUSTMENT" ${param.type == 'STOCKTAKE_ADJUSTMENT' ? 'selected' : ''}>Stocktake Adjustment</option>
                <option value="BLOCK"               ${param.type == 'BLOCK'               ? 'selected' : ''}>Block</option>
                <option value="UNBLOCK"             ${param.type == 'UNBLOCK'             ? 'selected' : ''}>Unblock</option>
            </select>
            <input type="date" name="from" value="<c:out value='${param.from}'/>" aria-label="From date">
            <input type="date" name="to" value="<c:out value='${param.to}'/>" aria-label="To date">
            <button type="submit" class="btn btn-secondary btn-sm">Filter</button>
            <a href="${ctx}/inventory?action=history" class="btn btn-ghost btn-sm">Reset</a>
        </form>

        <div class="admin-card">
            <c:choose>
                <c:when test="${empty movements}">
                    <div class="empty-state"><p>No movements found.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="admin-table admin-table-fixed">
                        <thead>
                        <tr>
                            <th>Date / Time</th>
                            <th>Product</th>
                            <th>Batch</th>
                            <th>Movement Type</th>
                            <th class="col-num">On Hand Change</th>
                            <th class="col-num">Reserved Change</th>
                            <th class="col-num">On Hand Before</th>
                            <th class="col-num">On Hand After</th>
                            <th class="col-num">Reserved Before</th>
                            <th class="col-num">Reserved After</th>
                            <th>Reference</th>
                            <th>Performed By</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="m" items="${movements}">
                            <tr>
                                <td><fmt:formatDate value="${m.createdAt}" pattern="yyyy-MM-dd HH:mm"/></td>
                                <td><c:out value="${m.productName}"/></td>
                                <td><c:out value="${m.batchNumber}"/></td>
                                <td>
                                    <span class="status-badge ${m.movementCss}">
                                        <c:out value="${m.movementLabel}"/>
                                    </span>
                                </td>
                                <td><c:out value="${m.onHandChangeLabel}"/></td>
                                <td><c:out value="${m.reservedChangeLabel}"/></td>
                                <td><c:out value="${m.onHandBefore}"/></td>
                                <td><c:out value="${m.onHandAfter}"/></td>
                                <td><c:out value="${m.reservedBefore}"/></td>
                                <td><c:out value="${m.reservedAfter}"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${m.referenceType == 'GOODS_RECEIPT'}">
                                            <a href="${ctx}/inventory/receipts?action=detail&id=${m.referenceId}">
                                                Goods Receipt #<c:out value="${m.referenceId}"/>
                                            </a>
                                        </c:when>
                                        <c:otherwise>
                                            <c:out value="${m.referenceType}"/> #<c:out value="${m.referenceId}"/>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td><c:out value="${m.performedByName}"/></td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>

                    <%-- Pagination --%>
                    <c:if test="${totalPages > 1}">
                        <nav class="pager" aria-label="Pages">
                            <c:forEach var="i" begin="1" end="${totalPages}">
                                <c:url var="pageUrl" value="/inventory">
                                    <c:param name="action" value="history"/>
                                    <c:param name="q" value="${param.q}"/>
                                    <c:param name="batch" value="${param.batch}"/>
                                    <c:param name="type" value="${param.type}"/>
                                    <c:param name="productId" value="${param.productId}"/>
                                    <c:param name="batchId" value="${param.batchId}"/>
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
