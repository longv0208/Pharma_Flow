<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="stock-receiving"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Phiếu nhập #${receipt.goodsReceiptId} — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <nav class="crumbs" aria-label="Breadcrumb">
            <a href="${ctx}/inventory/receipts">Kho hàng</a>
            <span aria-hidden="true">›</span>
            <a href="${ctx}/inventory/receipts">Nhập kho</a>
            <span aria-hidden="true">›</span>
            <span>Phiếu nhập #<c:out value="${receipt.goodsReceiptId}"/></span>
        </nav>

        <div class="section-head">
            <div>
                <h2>Phiếu nhập #<c:out value="${receipt.goodsReceiptId}"/></h2>
                <p class="section-sub">Thực tế nhà cung cấp giao — các dòng chấp nhận đã vào kho.</p>
            </div>
            <a class="btn btn-ghost" href="${ctx}/inventory/receipts">← Quay lại danh sách</a>
        </div>

        <c:if test="${param.ok == 'confirmed'}">
            <div class="alert alert-success" role="status">Đã xác nhận phiếu nhập — số lượng chấp nhận đã vào kho.</div>
        </c:if>
        <c:if test="${param.err == 'noteditable'}">
            <div class="alert alert-error" role="alert">Chỉ có phiếu nháp mới có thể sửa.</div>
        </c:if>
        <c:if test="${param.err == 'notcancellable'}">
            <div class="alert alert-error" role="alert">Phiếu nhập này không thể hủy.</div>
        </c:if>

        <div class="profile-card">
            <fieldset class="profile-group">
                <legend>Thông tin phiếu nhập</legend>
                <div class="profile-grid">
                    <div class="form-field">
                        <label>Mã phiếu nhập</label>
                        <span>#<c:out value="${receipt.goodsReceiptId}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Đơn đặt hàng</label>
                        <span>
                            <c:choose>
                                <c:when test="${empty receipt.purchaseOrderId}">—</c:when>
                                <c:otherwise>#<c:out value="${receipt.purchaseOrderId}"/></c:otherwise>
                            </c:choose>
                        </span>
                    </div>
                    <div class="form-field">
                        <label>Nhà cung cấp</label>
                        <span><c:out value="${receipt.supplierName}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Ngày nhận</label>
                        <span><c:out value="${receipt.receiptDate}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Số hóa đơn</label>
                        <span><c:out value="${empty receipt.invoiceNumber ? '—' : receipt.invoiceNumber}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Người nhận</label>
                        <span><c:out value="${receipt.receivedByName}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Trạng thái</label>
                        <span class="status-badge ${receipt.statusCss}">
                            <c:out value="${receipt.statusLabel}"/>
                        </span>
                    </div>
                    <div class="form-field">
                        <label>Ngày tạo</label>
                        <span><c:out value="${receipt.createdAt}"/></span>
                    </div>
                </div>
                <c:if test="${not empty receipt.note}">
                    <div class="form-field">
                        <label>Ghi chú</label>
                        <span><c:out value="${receipt.note}"/></span>
                    </div>
                </c:if>
            </fieldset>

            <fieldset class="profile-group">
                <legend>Sản phẩm nhận được</legend>
                <table class="admin-table">
                    <thead>
                    <tr>
                        <th>Sản phẩm</th>
                        <th>SKU</th>
                        <th>Số lô</th>
                        <th>Hạn dùng</th>
                        <th class="col-num">SL</th>
                        <th class="col-price">Giá nhập</th>
                        <th>Kiểm tra</th>
                        <th>Lý do từ chối</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="item" items="${items}">
                        <tr>
                            <td><c:out value="${item.productName}"/></td>
                            <td><c:out value="${item.sku}"/></td>
                            <td><c:out value="${item.batchNumber}"/></td>
                            <td><c:out value="${item.expiryDate}"/></td>
                            <td><c:out value="${item.quantity}"/></td>
                            <td><fmt:formatNumber value="${item.costPrice}" type="number" maxFractionDigits="0"/>&#x20AB;</td>
                            <td>
                                <span class="status-badge ${item.inspectionCss}">
                                    <c:out value="${item.inspectionLabel}"/>
                                </span>
                            </td>
                            <td><c:out value="${empty item.rejectionReason ? '—' : item.rejectionReason}"/></td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty items}">
                        <tr><td colspan="8"><span class="field-hint">Chưa có sản phẩm nào.</span></td></tr>
                    </c:if>
                    </tbody>
                </table>
            </fieldset>

            <div class="profile-actions">
                <c:if test="${receipt.editable}">
                    <a class="btn btn-secondary" href="${ctx}/inventory/receipts?action=edit&id=${receipt.goodsReceiptId}">Sửa</a>
                    <form method="post" action="${ctx}/inventory/receipts?action=confirm" class="inline-form"
                          onsubmit="return confirm('Xác nhận phiếu nhập? Số lượng chấp nhận sẽ vào kho và không thể chỉnh sửa sau đó.');">
                        <input type="hidden" name="goodsReceiptId" value="${receipt.goodsReceiptId}">
                        <button type="submit" class="btn btn-primary">Xác nhận nhập</button>
                    </form>
                </c:if>
                <c:if test="${receipt.cancellable}">
                    <form method="post" action="${ctx}/inventory/receipts?action=cancel" class="inline-form"
                          onsubmit="return confirm('Hủy phiếu nhập nháp? Chưa có gì được nhập vào kho.');">
                        <input type="hidden" name="goodsReceiptId" value="${receipt.goodsReceiptId}">
                        <button type="submit" class="btn btn-ghost btn-danger">Hủy nháp</button>
                    </form>
                </c:if>
            </div>
        </div>
    </main>
</div>

<%@ include file="/WEB-INF/jspf/admin-footer.jspf" %>

</body>
</html>
