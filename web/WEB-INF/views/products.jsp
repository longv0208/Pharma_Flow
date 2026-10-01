<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Medicines — PharmaFlow</title>
    <link rel="stylesheet" href="${ctx}/css/main.css">
</head>
<body>

<%@ include file="/WEB-INF/jspf/header.jspf" %>

<section class="section">
    <div class="container">
        <div class="section-head">
            <div>
                <h2>Product Catalog</h2>
                <p class="section-sub">${total} product(s) available for online purchase.</p>
            </div>
        </div>

        <%-- Filter bar (GET → bookmarkable) --%>
        <form class="filter-bar" method="get" action="${ctx}/products">
            <input type="search" name="q" value="<c:out value='${param.q}'/>"
                   placeholder="Search name, ingredient, manufacturer…" aria-label="Search products">
            <select name="category" aria-label="Category">
                <option value="">All categories</option>
                <c:forEach var="c" items="${categories}">
                    <option value="${c.categoryId}" ${param.category == c.categoryId ? 'selected' : ''}>
                        <c:out value="${c.categoryName}"/>
                    </option>
                </c:forEach>
            </select>
            <select name="type" aria-label="Product type">
                <option value="">All types</option>
                <option value="OTC"        ${param.type == 'OTC'        ? 'selected' : ''}>OTC</option>
                <option value="RX"         ${param.type == 'RX'         ? 'selected' : ''}>RX</option>
                <option value="RESTRICTED" ${param.type == 'RESTRICTED' ? 'selected' : ''}>RESTRICTED</option>
            </select>
            <button type="submit" class="btn btn-secondary btn-sm">Filter</button>
            <c:if test="${not empty param.q or not empty param.category or not empty param.type}">
                <a class="btn btn-ghost btn-sm" href="${ctx}/products">Clear</a>
            </c:if>
        </form>

        <c:choose>
            <c:when test="${empty products}">
                <div class="empty-state">
                    <p>No products match your filters.</p>
                    <a class="btn btn-ghost btn-sm" href="${ctx}/products">Browse all</a>
                </div>
            </c:when>
            <c:otherwise>
                <div class="product-grid">
                    <c:forEach var="p" items="${products}">
                        <c:set var="product" value="${p}" scope="request"/>
                        <jsp:include page="/WEB-INF/jspf/product-card.jsp"/>
                    </c:forEach>
                </div>

                <c:if test="${pages > 1}">
                    <nav class="pager" aria-label="Pages">
                        <c:forEach var="i" begin="1" end="${pages}">
                            <c:url var="pageUrl" value="/products">
                                <c:param name="q" value="${param.q}"/>
                                <c:param name="category" value="${param.category}"/>
                                <c:param name="type" value="${param.type}"/>
                                <c:param name="page" value="${i}"/>
                            </c:url>
                            <a class="pager-num ${i == page ? 'current' : ''}" href="${pageUrl}">${i}</a>
                        </c:forEach>
                    </nav>
                </c:if>
            </c:otherwise>
        </c:choose>
    </div>
</section>

<%@ include file="/WEB-INF/jspf/footer.jspf" %>

</body>
</html>
