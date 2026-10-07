<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="statusCode" value="${requestScope['jakarta.servlet.error.status_code']}"/>
<c:set var="errorMsg" value="${requestScope['jakarta.servlet.error.message']}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đã xảy ra lỗi — PharmaFlow</title>
    <link rel="stylesheet" href="${ctx}/css/main.css">
</head>
<body>
    <main class="error-page">
        <div class="error-card">
            <h1>${empty statusCode ? 'Lỗi' : statusCode}</h1>
            <p>
                <c:choose>
                    <c:when test="${not empty errorMsg}"><c:out value="${errorMsg}"/></c:when>
                    <c:otherwise>Đã xảy ra sự cố khi tải trang này.</c:otherwise>
                </c:choose>
            </p>
            <a class="btn btn-primary" href="${ctx}/home">Về trang chủ</a>
        </div>
    </main>
</body>
</html>
