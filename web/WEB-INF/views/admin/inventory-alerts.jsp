<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="inventory-alerts"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Inventory Alerts — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <nav class="crumbs" aria-label="Breadcrumb">
            <a href="${ctx}/inventory">Inventory</a>
            <span aria-hidden="true">›</span>
            <span>Alerts</span>
        </nav>

        <div class="section-head">
            <div>
                <h2>Inventory Alerts</h2>
                <p class="section-sub">Monitor medicines that require inventory attention.</p>
            </div>
            <c:if test="${ownerAdmin}">
                <a class="btn btn-secondary" href="${ctx}/inventory/alerts?action=settings">Alert Settings</a>
            </c:if>
        </div>

        <%-- Summary cards — same rules as the table, recalculated per request --%>
        <div class="dash-grid">
            <div class="dash-card">
                <span class="dash-name">Out of Stock</span>
                <span class="dash-desc">Saleable stock = 0</span>
                <strong class="alert-count">${summary[0]}</strong>
            </div>
            <div class="dash-card">
                <span class="dash-name">Low Stock</span>
                <span class="dash-desc">Saleable ≤ ${settings.minimumStockLevel}</span>
                <strong class="alert-count">${summary[1]}</strong>
            </div>
            <div class="dash-card">
                <span class="dash-name">Near Expiry</span>
                <span class="dash-desc">Within ${settings.nearExpiryWarningDays} days</span>
                <strong class="alert-count">${summary[2]}</strong>
            </div>
            <div class="dash-card">
                <span class="dash-name">Expired</span>
                <span class="dash-desc">Past expiry, stock left</span>
                <strong class="alert-count">${summary[3]}</strong>
            </div>
        </div>

        <%-- Filters — GET keeps them bookmarkable --%>
        <form class="filter-bar" method="get" action="${ctx}/inventory/alerts">
            <select name="type" aria-label="Alert type">
                <option value="">All Alerts</option>
                <option value="OUT_OF_STOCK" ${param.type == 'OUT_OF_STOCK' ? 'selected' : ''}>Out of Stock</option>
                <option value="LOW_STOCK"    ${param.type == 'LOW_STOCK'    ? 'selected' : ''}>Low Stock</option>
                <option value="NEAR_EXPIRY"  ${param.type == 'NEAR_EXPIRY'  ? 'selected' : ''}>Near Expiry</option>
                <option value="EXPIRED"      ${param.type == 'EXPIRED'      ? 'selected' : ''}>Expired</option>
            </select>
            <input type="search" name="q" value="<c:out value='${param.q}'/>"
                   placeholder="Product name / SKU / batch…" aria-label="Search alerts">
            <button type="submit" class="btn btn-secondary btn-sm">Filter</button>
            <a href="${ctx}/inventory/alerts" class="btn btn-ghost btn-sm">Reset</a>
        </form>

        <div class="admin-card">
            <c:choose>
                <c:when test="${empty alerts}">
                    <div class="empty-state"><p>No active alerts — inventory looks healthy.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="admin-table admin-table-fixed">
                        <thead>
                        <tr>
                            <th>Alert</th>
                            <th>Product</th>
                            <th>SKU</th>
                            <th>Batch</th>
                            <th class="col-num">Quantity</th>
                            <th>Threshold / Expiry</th>
                            <th>Details</th>
                            <th>Action</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="a" items="${alerts}">
                            <tr>
                                <td>
                                    <span class="status-badge ${a.alertCss}">
                                        <c:out value="${a.alertTypeLabel}"/>
                                    </span>
                                </td>
                                <td><c:out value="${a.productName}"/></td>
                                <td><c:out value="${a.sku}"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${empty a.batchNumber}">—</c:when>
                                        <c:otherwise><c:out value="${a.batchNumber}"/></c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${a.batchLevel}">
                                            <c:out value="${a.quantity}"/> on hand
                                        </c:when>
                                        <c:otherwise>
                                            <c:out value="${a.quantity}"/> available
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${a.batchLevel}">
                                            <c:choose>
                                                <c:when test="${a.alertType == 'EXPIRED'}">
                                                    Expired: <fmt:formatDate value="${a.expiryDate}" pattern="dd/MM/yyyy"/>
                                                </c:when>
                                                <c:otherwise>
                                                    Expires: <fmt:formatDate value="${a.expiryDate}" pattern="dd/MM/yyyy"/>
                                                </c:otherwise>
                                            </c:choose>
                                        </c:when>
                                        <c:otherwise>
                                            Minimum: <c:out value="${a.threshold}"/>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${a.batchLevel}">
                                            <c:out value="${a.expiryDetail}"/>
                                        </c:when>
                                        <c:otherwise>
                                            <c:out value="${a.message}"/>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${a.batchLevel}">
                                            <a class="btn btn-secondary btn-sm"
                                               href="${ctx}/inventory?action=batch&id=${a.batchId}">View Batch</a>
                                        </c:when>
                                        <c:otherwise>
                                            <a class="btn btn-secondary btn-sm"
                                               href="${ctx}/inventory?action=product&id=${a.productId}">View Inventory</a>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>

                    <%-- Pagination — preserves type + q --%>
                    <c:if test="${totalPages > 1}">
                        <nav class="pager" aria-label="Pages">
                            <c:forEach var="i" begin="1" end="${totalPages}">
                                <c:url var="pageUrl" value="/inventory/alerts">
                                    <c:param name="type" value="${param.type}"/>
                                    <c:param name="q" value="${param.q}"/>
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
