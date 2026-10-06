<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="inventory"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Inventory — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <div class="section-head">
            <div>
                <h2>Inventory</h2>
                <p class="section-sub">Manage medicine stock by product and batch.</p>
            </div>
        </div>

        <%-- Filter bar: GET keeps filters bookmarkable --%>
        <form class="filter-bar" method="get" action="${ctx}/inventory">
            <input type="search" name="q" value="<c:out value='${param.q}'/>"
                   placeholder="Search product name or SKU…" aria-label="Search products">
            <select name="categoryId" aria-label="Category">
                <option value="">All categories</option>
                <c:forEach var="c" items="${categories}">
                    <option value="${c.categoryId}" ${param.categoryId == c.categoryId ? 'selected' : ''}>
                        <c:out value="${c.categoryName}"/>
                    </option>
                </c:forEach>
            </select>
            <select name="status" aria-label="Inventory status">
                <option value="">All statuses</option>
                <option value="NORMAL"       ${param.status == 'NORMAL'       ? 'selected' : ''}>Normal</option>
                <option value="LOW_STOCK"    ${param.status == 'LOW_STOCK'    ? 'selected' : ''}>Low Stock</option>
                <option value="OUT_OF_STOCK" ${param.status == 'OUT_OF_STOCK' ? 'selected' : ''}>Out of Stock</option>
            </select>
            <button type="submit" class="btn btn-secondary btn-sm">Filter</button>
            <a href="${ctx}/inventory" class="btn btn-ghost btn-sm">Reset</a>
        </form>

        <c:if test="${param.err == 'notfound'}">
            <div class="alert alert-error" role="alert">Product or batch not found.</div>
        </c:if>

        <div class="admin-card">
            <c:choose>
                <c:when test="${empty products}">
                    <div class="empty-state"><p>No products match.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="admin-table admin-table-fixed">
                        <thead>
                        <tr>
                            <th>Product</th>
                            <th class="col-sku">SKU</th>
                            <th class="col-cat">Category</th>
                            <th class="col-num">Batch Count</th>
                            <th class="col-num">On Hand</th>
                            <th class="col-num">Reserved</th>
                            <th class="col-num">Available</th>
                            <th class="col-status">Status</th>
                            <th class="col-act">Action</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="p" items="${products}">
                            <tr>
                                <td><c:out value="${p.productName}"/></td>
                                <td><c:out value="${p.sku}"/></td>
                                <td><c:out value="${p.categoryName}"/></td>
                                <td><c:out value="${p.batchCount}"/> batch(es)</td>
                                <td><c:out value="${p.onHand}"/></td>
                                <td><c:out value="${p.reserved}"/></td>
                                <td><strong><c:out value="${p.available}"/></strong></td>
                                <td>
                                    <span class="status-badge ${p.inventoryStatusCss}">
                                        <c:out value="${p.inventoryStatusLabel}"/>
                                    </span>
                                </td>
                                <td class="col-actions">
                                    <a class="btn btn-secondary btn-sm"
                                       href="${ctx}/inventory?action=product&id=${p.productId}">View</a>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>

                    <%-- Pagination --%>
                    <c:if test="${totalPages > 1}">
                        <nav class="pager" aria-label="Pages">
                            <c:forEach var="i" begin="1" end="${totalPages}">
                                <c:url var="pageUrl" value="/inventory">
                                    <c:param name="q" value="${param.q}"/>
                                    <c:param name="categoryId" value="${param.categoryId}"/>
                                    <c:param name="status" value="${param.status}"/>
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
