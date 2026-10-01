<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="dashboard"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Dashboard — Admin — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <div class="section-head">
            <div>
                <h2>Dashboard</h2>
                <p class="section-sub">Welcome back, <c:out value="${sessionScope.currentUser.fullName}"/>.</p>
            </div>
        </div>

        <div class="admin-card">
            <h3 style="margin-top:0">Catalog</h3>
            <ul class="admin-links">
                <li><a href="${ctx}/admin?action=products">Product Management</a> — catalog items, pricing, OTC/RX types</li>
                <li><a href="${ctx}/admin?action=categories">Category Management</a> — storefront category groups</li>
                <li><a href="${ctx}/admin?action=suppliers">Supplier Management</a> — vendors for purchase orders</li>
            </ul>
            <p class="field-hint">Orders, staff accounts and reports will appear here in later phases.</p>
        </div>
    </main>
</div>

<%@ include file="/WEB-INF/jspf/admin-footer.jspf" %>

</body>
</html>
