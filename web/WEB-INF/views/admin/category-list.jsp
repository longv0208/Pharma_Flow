<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="categories"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Categories — Admin — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <div class="section-head">
            <div>
                <h2>Category Management</h2>
                <p class="section-sub">Organize the product catalog. Deactivated categories are hidden from the storefront but keep their products.</p>
            </div>
            <a class="btn btn-primary" href="${ctx}/admin?action=category-new">+ New Category</a>
        </div>

        <c:if test="${param.ok == 'created'}">
            <div class="alert alert-success" role="status">Category created.</div>
        </c:if>
        <c:if test="${param.ok == 'updated'}">
            <div class="alert alert-success" role="status">Category updated.</div>
        </c:if>
        <c:if test="${param.ok == 'deactivated'}">
            <div class="alert alert-success" role="status">Category deactivated.</div>
        </c:if>
        <c:if test="${param.ok == 'activated'}">
            <div class="alert alert-success" role="status">Category activated.</div>
        </c:if>
        <c:if test="${param.err == 'notfound'}">
            <div class="alert alert-error" role="alert">Category not found.</div>
        </c:if>

        <div class="admin-card">
            <c:choose>
                <c:when test="${empty categories}">
                    <div class="empty-state"><p>No categories yet.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="admin-table admin-table-fixed table-categories">
                        <thead>
                        <tr>
                            <th class="col-id">ID</th>
                            <th class="col-name">Name</th>
                            <th>Description</th>
                            <th class="col-status">Status</th>
                            <th class="col-act">Actions</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="c" items="${categories}">
                            <tr>
                                <td><c:out value="${c.categoryId}"/></td>
                                <td><c:out value="${c.categoryName}"/></td>
                                <td class="col-desc"><c:out value="${c.description}"/></td>
                                <td>
                                    <span class="status-badge status-${c.status == 'ACTIVE' ? 'active' : 'inactive'}">
                                        <c:out value="${c.status}"/>
                                    </span>
                                </td>
                                <td class="col-actions">
                                    <a class="btn btn-secondary btn-sm"
                                       href="${ctx}/admin?action=category-edit&id=${c.categoryId}">Edit</a>
                                    <c:choose>
                                        <c:when test="${c.status == 'ACTIVE'}">
                                            <form method="post" action="${ctx}/admin?action=category-delete" class="inline-form"
                                                  onsubmit="return confirm('Deactivate this category? Products inside it are kept but hidden.');">
                                                <input type="hidden" name="categoryId" value="${c.categoryId}">
                                                <button type="submit" class="btn btn-ghost btn-sm btn-danger">Deactivate</button>
                                            </form>
                                        </c:when>
                                        <c:otherwise>
                                            <form method="post" action="${ctx}/admin?action=category-activate" class="inline-form">
                                                <input type="hidden" name="categoryId" value="${c.categoryId}">
                                                <button type="submit" class="btn btn-ghost btn-sm btn-success">Activate</button>
                                            </form>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </c:otherwise>
            </c:choose>
        </div>
    </main>
</div>

<%@ include file="/WEB-INF/jspf/admin-footer.jspf" %>

</body>
</html>
