<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="stock-receiving"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Receipt #${receipt.goodsReceiptId} — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <nav class="crumbs" aria-label="Breadcrumb">
            <a href="${ctx}/inventory/receipts">Inventory</a>
            <span aria-hidden="true">›</span>
            <a href="${ctx}/inventory/receipts">Stock Receiving</a>
            <span aria-hidden="true">›</span>
            <span>Receipt #<c:out value="${receipt.goodsReceiptId}"/></span>
        </nav>

        <div class="section-head">
            <div>
                <h2>Goods Receipt #<c:out value="${receipt.goodsReceiptId}"/></h2>
                <p class="section-sub">What the supplier actually delivered — accepted lines already entered inventory.</p>
            </div>
            <a class="btn btn-ghost" href="${ctx}/inventory/receipts">← Back to list</a>
        </div>

        <c:if test="${param.ok == 'confirmed'}">
            <div class="alert alert-success" role="status">Receipt confirmed — accepted quantities are now in inventory.</div>
        </c:if>
        <c:if test="${param.err == 'noteditable'}">
            <div class="alert alert-error" role="alert">Only draft receipts can be edited.</div>
        </c:if>
        <c:if test="${param.err == 'notcancellable'}">
            <div class="alert alert-error" role="alert">This receipt can no longer be cancelled.</div>
        </c:if>

        <div class="profile-card">
            <fieldset class="profile-group">
                <legend>Receipt Information</legend>
                <div class="profile-grid">
                    <div class="form-field">
                        <label>Receipt ID</label>
                        <span>#<c:out value="${receipt.goodsReceiptId}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Purchase Order</label>
                        <span>
                            <c:choose>
                                <c:when test="${empty receipt.purchaseOrderId}">—</c:when>
                                <c:otherwise>#<c:out value="${receipt.purchaseOrderId}"/></c:otherwise>
                            </c:choose>
                        </span>
                    </div>
                    <div class="form-field">
                        <label>Supplier</label>
                        <span><c:out value="${receipt.supplierName}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Receipt Date</label>
                        <span><c:out value="${receipt.receiptDate}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Invoice Number</label>
                        <span><c:out value="${empty receipt.invoiceNumber ? '—' : receipt.invoiceNumber}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Received By</label>
                        <span><c:out value="${receipt.receivedByName}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Status</label>
                        <span class="status-badge ${receipt.statusCss}">
                            <c:out value="${receipt.statusLabel}"/>
                        </span>
                    </div>
                    <div class="form-field">
                        <label>Created At</label>
                        <span><c:out value="${receipt.createdAt}"/></span>
                    </div>
                </div>
                <c:if test="${not empty receipt.note}">
                    <div class="form-field">
                        <label>Note</label>
                        <span><c:out value="${receipt.note}"/></span>
                    </div>
                </c:if>
            </fieldset>

            <fieldset class="profile-group">
                <legend>Received Items</legend>
                <table class="admin-table">
                    <thead>
                    <tr>
                        <th>Product</th>
                        <th>SKU</th>
                        <th>Batch</th>
                        <th>Expiry</th>
                        <th class="col-num">Qty</th>
                        <th class="col-price">Cost Price</th>
                        <th>Inspection</th>
                        <th>Rejection Reason</th>
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
                        <tr><td colspan="8"><span class="field-hint">No items recorded.</span></td></tr>
                    </c:if>
                    </tbody>
                </table>
            </fieldset>

            <div class="profile-actions">
                <c:if test="${receipt.editable}">
                    <a class="btn btn-secondary" href="${ctx}/inventory/receipts?action=edit&id=${receipt.goodsReceiptId}">Edit</a>
                    <form method="post" action="${ctx}/inventory/receipts?action=confirm" class="inline-form"
                          onsubmit="return confirm('Confirm this receipt? Accepted quantities will enter inventory and cannot be edited here afterwards.');">
                        <input type="hidden" name="goodsReceiptId" value="${receipt.goodsReceiptId}">
                        <button type="submit" class="btn btn-primary">Confirm Receipt</button>
                    </form>
                </c:if>
                <c:if test="${receipt.cancellable}">
                    <form method="post" action="${ctx}/inventory/receipts?action=cancel" class="inline-form"
                          onsubmit="return confirm('Cancel this draft receipt? Nothing was received into inventory.');">
                        <input type="hidden" name="goodsReceiptId" value="${receipt.goodsReceiptId}">
                        <button type="submit" class="btn btn-ghost btn-danger">Cancel Draft</button>
                    </form>
                </c:if>
            </div>
        </div>
    </main>
</div>

<%@ include file="/WEB-INF/jspf/admin-footer.jspf" %>

</body>
</html>
