<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="inventory"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title><c:out value="${product.productName}"/> — Tồn kho — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <nav class="crumbs" aria-label="Breadcrumb">
            <a href="${ctx}/inventory">Tồn kho</a>
            <span aria-hidden="true">›</span>
            <span><c:out value="${product.productName}"/></span>
        </nav>

        <div class="section-head">
            <div>
                <h2><c:out value="${product.productName}"/></h2>
                <p class="section-sub">Chi tiết tồn kho sản phẩm — các lô sắp xếp theo hạn dùng (FEFO).</p>
            </div>
            <a class="btn btn-ghost" href="${ctx}/inventory">← Quay lại Tồn kho</a>
        </div>

        <div class="profile-card">
            <%-- Product information --%>
            <fieldset class="profile-group">
                <legend>Thông tin Sản phẩm</legend>
                <div class="profile-grid">
                    <div class="form-field">
                        <label>Tên sản phẩm</label>
                        <span><c:out value="${product.productName}"/></span>
                    </div>
                    <div class="form-field">
                        <label>SKU</label>
                        <span><c:out value="${product.sku}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Danh mục</label>
                        <span><c:out value="${product.categoryName}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Loại sản phẩm</label>
                        <span><c:out value="${product.productType}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Đơn vị bán</label>
                        <span><c:out value="${product.sellingUnit}"/></span>
                    </div>
                </div>
            </fieldset>

            <%-- Inventory summary --%>
            <fieldset class="profile-group">
                <legend>Tóm tắt Tồn kho</legend>
                <div class="profile-grid">
                    <div class="form-field">
                        <label>Tồn thực tế</label>
                        <span><strong><c:out value="${product.onHand}"/></strong></span>
                    </div>
                    <div class="form-field">
                        <label>Đã đặt</label>
                        <span><c:out value="${product.reserved}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Khả dụng</label>
                        <span><strong><c:out value="${product.available}"/></strong></span>
                    </div>
                    <div class="form-field">
                        <label>Số lô</label>
                        <span><c:out value="${product.batchCount}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Trạng thái tồn kho</label>
                        <span class="status-badge ${product.inventoryStatusCss}">
                            <c:out value="${product.inventoryStatusLabel}"/>
                        </span>
                    </div>
                </div>
            </fieldset>

            <%-- Batch list --%>
            <fieldset class="profile-group">
                <legend>Danh sách Lô</legend>
                <table class="admin-table">
                    <thead>
                    <tr>
                        <th>Số lô</th>
                        <th>Hạn dùng</th>
                        <th>Nhà cung cấp</th>
                        <th class="col-num">Tồn thực tế</th>
                        <th class="col-num">Đã đặt</th>
                        <th class="col-num">Khả dụng</th>
                        <th>Trạng thái</th>
                        <th>Vị trí lưu trữ</th>
                        <th class="col-act">Thao tác</th>
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
                                   href="${ctx}/inventory?action=batch&id=${b.batchId}">Xem</a>
                            </td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty batches}">
                        <tr><td colspan="9"><span class="field-hint">Chưa có lô nào được ghi nhận cho sản phẩm này.</span></td></tr>
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
