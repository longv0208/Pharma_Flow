<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Nhân viên — PharmaFlow</title>
    <link rel="stylesheet" href="${ctx}/css/main.css">
</head>
<body>
<div class="container" style="padding:48px 16px;">
    <h1>Bảng điều khiển nhân viên</h1>
    <p>Đăng nhập với tên <strong><c:out value="${sessionScope.currentUser.fullName}"/></strong> (NHÂN VIÊN).</p>
    <p><a href="${ctx}/authen?action=logout">Đăng xuất</a> · <a href="${ctx}/home">Xem cửa hàng</a></p>
    <p><em>Các tính năng dành cho nhân viên sẽ được triển khai ở các giai đoạn sau.</em></p>
</div>
</body>
</html>
