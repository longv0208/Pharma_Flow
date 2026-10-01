<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Admin — PharmaFlow</title>
    <link rel="stylesheet" href="${ctx}/css/main.css">
</head>
<body>

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<section class="section">
    <div class="container">
        <div class="section-head">
            <div>
                <h2>Admin Dashboard</h2>
                <p class="section-sub">Signed in as <c:out value="${sessionScope.currentUser.fullName}"/> (OWNER_ADMIN)</p>
            </div>
        </div>

        <div class="admin-card">
            <h3 style="margin-top:0">Catalog</h3>
            <ul class="admin-links">
                <li><a href="${ctx}/admin?action=categories">Category Management</a> — create, edit, deactivate storefront categories</li>
                <li><a href="${ctx}/admin?action=suppliers">Supplier Management</a> — vendors for purchase orders and stock intake</li>
                <li><a href="${ctx}/admin?action=products">Product Management</a> — catalog items, pricing, OTC/RX types, online-sale flag</li>
            </ul>
            <p class="field-hint">More admin features (products, orders, staff) will be added in later phases.</p>
        </div>
    </div>
</section>

</body>
</html>
