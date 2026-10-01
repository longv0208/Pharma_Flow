<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="products"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Products — Admin — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <div class="section-head">
            <div>
                <h2>Product Management</h2>
                <p class="section-sub">${total} product(s). Deactivated products stay in order history but leave the storefront.</p>
            </div>
            <a class="btn btn-primary" href="${ctx}/admin?action=product-new">+ New Product</a>
        </div>

        <%-- Filter bar: GET keeps filters bookmarkable --%>
        <form class="filter-bar" method="get" action="${ctx}/admin">
            <input type="hidden" name="action" value="products">
            <input type="search" name="q" value="<c:out value='${param.q}'/>"
                   placeholder="Search name, SKU, barcode…" aria-label="Search products">
            <select name="categoryId" aria-label="Category">
                <option value="">All categories</option>
                <c:forEach var="c" items="${categories}">
                    <option value="${c.categoryId}" ${param.categoryId == c.categoryId ? 'selected' : ''}>
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
            <select name="status" aria-label="Status">
                <option value="">All status</option>
                <option value="ACTIVE"   ${param.status == 'ACTIVE'   ? 'selected' : ''}>ACTIVE</option>
                <option value="INACTIVE" ${param.status == 'INACTIVE' ? 'selected' : ''}>INACTIVE</option>
            </select>
            <button type="submit" class="btn btn-secondary btn-sm">Filter</button>
        </form>

        <c:if test="${param.ok == 'created'}">
            <div class="alert alert-success" role="status">Product created.</div>
        </c:if>
        <c:if test="${param.ok == 'updated'}">
            <div class="alert alert-success" role="status">Product updated.</div>
        </c:if>
        <c:if test="${param.ok == 'deactivated'}">
            <div class="alert alert-success" role="status">Product deactivated.</div>
        </c:if>
        <c:if test="${param.ok == 'activated'}">
            <div class="alert alert-success" role="status">Product activated.</div>
        </c:if>
        <c:if test="${param.err == 'notfound'}">
            <div class="alert alert-error" role="alert">Product not found.</div>
        </c:if>

        <div class="admin-card">
            <c:choose>
                <c:when test="${empty products}">
                    <div class="empty-state"><p>No products match.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="admin-table admin-table-fixed table-products">
                        <thead>
                        <tr>
                            <th class="col-id">ID</th>
                            <th>Name</th>
                            <th class="col-cat">Category</th>
                            <th class="col-sku">SKU</th>
                            <th class="col-type">Type</th>
                            <th class="col-price">Price</th>
                            <th class="col-num">Stock</th>
                            <th class="col-num">Online</th>
                            <th class="col-status">Status</th>
                            <th class="col-act">Actions</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="p" items="${products}">
                            <tr>
                                <td><c:out value="${p.productId}"/></td>
                                <td>
                                    <c:out value="${p.productName}"/>
                                    <c:if test="${not empty p.strength}">
                                        <span class="field-hint"> · <c:out value="${p.strength}"/></span>
                                    </c:if>
                                </td>
                                <td class="col-desc"><c:out value="${p.categoryName}"/></td>
                                <td><c:out value="${p.sku}"/></td>
                                <td>
                                    <span class="type-badge type-${p.productType.name().toLowerCase()}">
                                        <c:out value="${p.productType}"/>
                                    </span>
                                </td>
                                <td>
                                    <fmt:formatNumber value="${p.sellingPrice}" type="number" maxFractionDigits="0"/>&#x20AB;
                                    <span class="field-hint">/ <c:out value="${p.sellingUnit}"/></span>
                                </td>
                                <td><c:out value="${p.availableQuantity}"/></td>
                                <td>${p.onlineSaleAllowed ? 'Yes' : 'No'}</td>
                                <td>
                                    <span class="status-badge status-${p.status == 'ACTIVE' ? 'active' : 'inactive'}">
                                        <c:out value="${p.status}"/>
                                    </span>
                                </td>
                                <td class="col-actions">
                                    <a class="btn btn-secondary btn-sm"
                                       href="${ctx}/admin?action=product-edit&id=${p.productId}">Edit</a>
                                    <c:choose>
    <c:when test="${p.status == 'ACTIVE'}">
        <form method="post" action="${ctx}/admin?action=product-delete" class="inline-form"
                                              onsubmit="return confirm('Deactivate this product? Order and batch history is preserved.');">
                                            <input type="hidden" name="productId" value="${p.productId}">
                                            <button type="submit" class="btn btn-ghost btn-sm btn-danger">Deactivate</button>
                                        </form>
    </c:when>
    <c:otherwise>
        <form method="post" action="${ctx}/admin?action=product-activate" class="inline-form">
            <input type="hidden" name="productId" value="${p.productId}">
            <button type="submit" class="btn btn-ghost btn-sm btn-success">Activate</button>
        </form>
    </c:otherwise>
</c:choose>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>

                    <%-- Pagination --%>
                    <c:if test="${pages > 1}">
                        <nav class="pager" aria-label="Pages">
                            <c:forEach var="i" begin="1" end="${pages}">
                                <c:url var="pageUrl" value="/admin">
                                    <c:param name="action" value="products"/>
                                    <c:param name="q" value="${param.q}"/>
                                    <c:param name="categoryId" value="${param.categoryId}"/>
                                    <c:param name="type" value="${param.type}"/>
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
