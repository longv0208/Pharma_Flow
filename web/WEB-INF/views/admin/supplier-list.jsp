<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="suppliers"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Suppliers — Admin — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <div class="section-head">
            <div>
                <h2>Supplier Management</h2>
                <p class="section-sub">Vendors that supply products to the pharmacy. Deactivated suppliers are kept for purchase history.</p>
            </div>
            <a class="btn btn-primary" href="${ctx}/admin?action=supplier-new">+ New Supplier</a>
        </div>

        <c:if test="${param.ok == 'created'}">
            <div class="alert alert-success" role="status">Supplier created.</div>
        </c:if>
        <c:if test="${param.ok == 'updated'}">
            <div class="alert alert-success" role="status">Supplier updated.</div>
        </c:if>
        <c:if test="${param.ok == 'deactivated'}">
            <div class="alert alert-success" role="status">Supplier deactivated.</div>
        </c:if>
        <c:if test="${param.err == 'notfound'}">
            <div class="alert alert-error" role="alert">Supplier not found.</div>
        </c:if>

        <div class="admin-card">
            <c:choose>
                <c:when test="${empty suppliers}">
                    <div class="empty-state"><p>No suppliers yet.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="admin-table">
                        <thead>
                        <tr>
                            <th style="width:60px">ID</th>
                            <th>Supplier Name</th>
                            <th>Contact Person</th>
                            <th>Phone</th>
                            <th>Email</th>
                            <th style="width:100px">Status</th>
                            <th style="width:170px">Actions</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="s" items="${suppliers}">
                            <tr>
                                <td><c:out value="${s.supplierId}"/></td>
                                <td><c:out value="${s.supplierName}"/></td>
                                <td><c:out value="${s.contactPerson}"/></td>
                                <td><c:out value="${s.phone}"/></td>
                                <td class="col-desc"><c:out value="${s.email}"/></td>
                                <td>
                                    <span class="status-badge status-${s.status == 'ACTIVE' ? 'active' : 'inactive'}">
                                        <c:out value="${s.status}"/>
                                    </span>
                                </td>
                                <td class="col-actions">
                                    <a class="btn btn-secondary btn-sm"
                                       href="${ctx}/admin?action=supplier-edit&id=${s.supplierId}">Edit</a>
                                    <c:if test="${s.status == 'ACTIVE'}">
                                        <form method="post" action="${ctx}/admin?action=supplier-delete" class="inline-form"
                                              onsubmit="return confirm('Deactivate this supplier? Purchase history is preserved.');">
                                            <input type="hidden" name="supplierId" value="${s.supplierId}">
                                            <button type="submit" class="btn btn-ghost btn-sm btn-danger">Deactivate</button>
                                        </form>
                                    </c:if>
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
