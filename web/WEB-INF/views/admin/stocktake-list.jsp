<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="stocktake"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Stocktake — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <div class="section-head">
            <div>
                <h2>Stocktake</h2>
                <p class="section-sub">Periodic physical inventory counting and reconciliation.</p>
            </div>
            <form method="post" action="${ctx}/inventory/stocktakes"
                  onsubmit="this.querySelector('button').disabled = true;">
                <input type="hidden" name="action" value="create">
                <button type="submit" class="btn btn-primary">New Stocktake</button>
            </form>
        </div>

        <c:if test="${param.ok == 'created'}">
            <div class="alert alert-success" role="status">Stocktake created.</div>
        </c:if>
        <c:if test="${param.err == 'notfound'}">
            <div class="alert alert-error" role="alert">Stocktake not found.</div>
        </c:if>
        <c:if test="${param.err == 'db'}">
            <div class="alert alert-error" role="alert">Database error — the stocktake was not saved.</div>
        </c:if>

        <%-- Status filter --%>
        <div class="filter-bar">
            <a class="btn ${empty param.status ? 'btn-secondary' : 'btn-ghost'} btn-sm"
               href="${ctx}/inventory/stocktakes">All</a>
            <a class="btn ${param.status == 'DRAFT' ? 'btn-secondary' : 'btn-ghost'} btn-sm"
               href="${ctx}/inventory/stocktakes?status=DRAFT">Draft</a>
            <a class="btn ${param.status == 'IN_PROGRESS' ? 'btn-secondary' : 'btn-ghost'} btn-sm"
               href="${ctx}/inventory/stocktakes?status=IN_PROGRESS">In Progress</a>
            <a class="btn ${param.status == 'COMPLETED' ? 'btn-secondary' : 'btn-ghost'} btn-sm"
               href="${ctx}/inventory/stocktakes?status=COMPLETED">Completed</a>
        </div>

        <div class="admin-card">
            <c:choose>
                <c:when test="${empty stocktakes}">
                    <div class="empty-state"><p>No stocktakes found.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="admin-table admin-table-fixed">
                        <thead>
                        <tr>
                            <th>Stocktake ID</th>
                            <th>Created Date</th>
                            <th>Created By</th>
                            <th>Status</th>
                            <th class="col-num">Items</th>
                            <th class="col-num">Differences</th>
                            <th>Completed At</th>
                            <th>Action</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="s" items="${stocktakes}">
                            <tr>
                                <td><strong>ST-<c:out value="${s.stocktakeId}"/></strong></td>
                                <td><fmt:formatDate value="${s.createdAt}" pattern="dd/MM/yyyy HH:mm"/></td>
                                <td><c:out value="${s.createdByName}"/></td>
                                <td>
                                    <span class="status-badge ${s.statusCss}">
                                        <c:out value="${s.statusLabel}"/>
                                    </span>
                                </td>
                                <td><c:out value="${s.itemCount}"/></td>
                                <td><c:out value="${s.differenceCount}"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${empty s.completedAt}">—</c:when>
                                        <c:otherwise><fmt:formatDate value="${s.completedAt}" pattern="dd/MM/yyyy HH:mm"/></c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <a class="btn btn-ghost btn-sm"
                                       href="${ctx}/inventory/stocktakes?action=detail&id=${s.stocktakeId}">
                                        <c:out value="${s.actionLabel}"/>
                                    </a>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>

                    <%-- Pagination --%>
                    <c:if test="${totalPages > 1}">
                        <nav class="pager" aria-label="Pages">
                            <c:forEach var="i" begin="1" end="${totalPages}">
                                <c:url var="pageUrl" value="/inventory/stocktakes">
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
