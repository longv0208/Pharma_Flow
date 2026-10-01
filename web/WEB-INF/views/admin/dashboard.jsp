<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Admin — PharmaFlow</title>
    <link rel="stylesheet" href="${ctx}/css/main.css">
</head>
<body>
<div class="container" style="padding:48px 16px;">
    <h1>Admin Dashboard</h1>
    <p>Signed in as <strong><c:out value="${sessionScope.currentUser.fullName}"/></strong> (OWNER_ADMIN).</p>
    <p><a href="${ctx}/authen?action=logout">Sign out</a> · <a href="${ctx}/home">View storefront</a></p>
    <p><em>Admin features will be implemented in later phases.</em></p>
</div>
</body>
</html>
