<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="inventory"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title><c:out value="${product.productName}"/> — Inventory — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <nav class="crumbs" aria-label="Breadcrumb">
            <a href="${ctx}/inventory">Inventory</a>
            <span aria-hidden="true">›</span>
            <span><c:out value="${product.productName}"/></span>
        </nav>

        <div class="section-head">
            <div>
                <h2><c:out value="${product.productName}"/></h2>
                <p class="section-sub">Product inventory detail — batches ordered by expiry (FEFO).</p>
            </div>
            <a class="btn btn-ghost" href="${ctx}/inventory">← Back to Inventory</a>
        </div>

        <div class="profile-card">
            <%-- Product information --%>
            <fieldset class="profile-group">
                <legend>Product Information</legend>
                <div class="profile-grid">
                    <div class="form-field">
                        <label>Product Name</label>
                        <span><c:out value="${product.productName}"/></span>
                    </div>
                    <div class="form-field">
                        <label>SKU</label>
                        <span><c:out value="${product.sku}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Category</label>
                        <span><c:out value="${product.categoryName}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Product Type</label>
                        <span><c:out value="${product.productType}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Selling Unit</label>
                        <span><c:out value="${product.sellingUnit}"/></span>
                    </div>
                </div>
            </fieldset>

            <%-- Inventory summary --%>
            <fieldset class="profile-group">
                <legend>Inventory Summary</legend>
                <div class="profile-grid">
                    <div class="form-field">
                        <label>On Hand</label>
                        <span><strong><c:out value="${product.onHand}"/></strong></span>
                    </div>
                    <div class="form-field">
                        <label>Reserved</label>
                        <span><c:out value="${product.reserved}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Available</label>
                        <span><strong><c:out value="${product.available}"/></strong></span>
                    </div>
                    <div class="form-field">
                        <label>Batch Count</label>
                        <span><c:out value="${product.batchCount}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Inventory Status</label>
                        <span class="status-badge ${product.inventoryStatusCss}">
                            <c:out value="${product.inventoryStatusLabel}"/>
                        </span>
                    </div>
                </div>
            </fieldset>

            <%-- Batch list --%>
            <fieldset class="profile-group">
                <legend>Batch List</legend>
                <table class="admin-table">
                    <thead>
                    <tr>
                        <th>Batch Number</th>
                        <th>Expiry Date</th>
                        <th>Supplier</th>
                        <th class="col-num">On Hand</th>
                        <th class="col-num">Reserved</th>
                        <th class="col-num">Available</th>
                        <th>Status</th>
                        <th>Storage Location</th>
                        <th class="col-act">Action</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="b" items="${batches}">
                        <tr>
                            <td><c:out value="${b.batchNumber}"/></td>
                            <td><c:out value="${b.expiryDate}"/></td>
                            <td><c:out value="${b.supplierName}"/></td>
                            <td><c:out value="${b.onHandQuantity}"/></td>
                            <td><c:out value="${b.reservedQuantity}"/></td>
                            <td><strong><c:out value="${b.onHandQuantity - b.reservedQuantity}"/></strong></td>
                            <td>
                                <span class="status-badge batch-${b.status.toLowerCase().replace('_','-')}">
                                    <c:out value="${b.status}"/>
                                </span>
                            </td>
                            <td><c:out value="${empty b.storageLocation ? '—' : b.storageLocation}"/></td>
                            <td class="col-actions">
                                <a class="btn btn-secondary btn-sm"
                                   href="${ctx}/inventory?action=batch&id=${b.batchId}">View</a>
                            </td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty batches}">
                        <tr><td colspan="9"><span class="field-hint">No batches recorded for this product.</span></td></tr>
                    </c:if>
                    </tbody>
                </table>
            </fieldset>
        </div>
    </main>
</div>

<%@ include file="/WEB-INF/jspf/admin-footer.jspf" %>

</body>
</html>
