<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="purchase-orders"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>PO #${order.purchaseOrderId} — Admin — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <div class="section-head">
            <div>
                <h2>Purchase Order #<c:out value="${order.purchaseOrderId}"/></h2>
                <p class="section-sub">Read-only view. Received quantities are updated by Receive Stock later.</p>
            </div>
            <a class="btn btn-ghost" href="${ctx}/admin/purchase-orders">← Back to list</a>
        </div>

        <c:if test="${param.err == 'notcancellable'}">
            <div class="alert alert-error" role="alert">This purchase order can no longer be cancelled.</div>
        </c:if>
        <c:if test="${param.err == 'noteditable'}">
            <div class="alert alert-error" role="alert">Only draft purchase orders can be edited.</div>
        </c:if>

        <div class="profile-card">
            <fieldset class="profile-group">
                <legend>Purchase Order Information</legend>
                <div class="profile-grid">
                    <div class="form-field">
                        <label>PO ID</label>
                        <span>#<c:out value="${order.purchaseOrderId}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Supplier</label>
                        <span><c:out value="${order.supplierName}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Created By</label>
                        <span><c:out value="${order.createdByName}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Order Date</label>
                        <span><c:out value="${order.orderDate}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Expected Delivery</label>
                        <span>
                            <c:choose>
                                <c:when test="${empty order.expectedDeliveryDate}">—</c:when>
                                <c:otherwise><c:out value="${order.expectedDeliveryDate}"/></c:otherwise>
                            </c:choose>
                        </span>
                    </div>
                    <div class="form-field">
                        <label>Status</label>
                        <span class="status-badge ${order.statusCss}">
                            <c:out value="${order.statusLabel}"/>
                        </span>
                    </div>
                    <div class="form-field">
                        <label>Source</label>
                        <span><c:out value="${order.sourceType}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Created At</label>
                        <span><c:out value="${order.createdAt}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Total Amount</label>
                        <span><fmt:formatNumber value="${order.totalAmount}" type="number" maxFractionDigits="0"/>&#x20AB;</span>
                    </div>
                </div>
                <c:if test="${not empty order.note}">
                    <div class="form-field">
                        <label>Note</label>
                        <span><c:out value="${order.note}"/></span>
                    </div>
                </c:if>
            </fieldset>

            <fieldset class="profile-group">
                <legend>Order Items</legend>
                <table class="admin-table">
                    <thead>
                    <tr>
                        <th>Product</th>
                        <th>SKU</th>
                        <th class="col-num">Ordered</th>
                        <th class="col-num">Received</th>
                        <th class="col-price">Unit Cost</th>
                        <th class="col-price">Subtotal</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="item" items="${items}">
                        <tr>
                            <td><c:out value="${item.productName}"/></td>
                            <td><c:out value="${item.sku}"/></td>
                            <td><c:out value="${item.orderedQuantity}"/></td>
                            <td><c:out value="${item.receivedQuantity}"/></td>
                            <td><fmt:formatNumber value="${item.unitCost}" type="number" maxFractionDigits="0"/>&#x20AB;</td>
                            <td><fmt:formatNumber value="${item.subtotal}" type="number" maxFractionDigits="0"/>&#x20AB;</td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </fieldset>

            <div class="profile-actions">
                <c:if test="${order.editable}">
                    <a class="btn btn-secondary" href="${ctx}/admin/purchase-orders?action=edit&id=${order.purchaseOrderId}">Edit</a>
                    <form method="post" action="${ctx}/admin/purchase-orders?action=place" class="inline-form"
                          onsubmit="return confirm('Place this order with the supplier? It becomes read-only.');">
                        <input type="hidden" name="purchaseOrderId" value="${order.purchaseOrderId}">
                        <button type="submit" class="btn btn-primary">Place Order</button>
                    </form>
                </c:if>
                <c:if test="${order.cancellable}">
                    <form method="post" action="${ctx}/admin/purchase-orders?action=cancel" class="inline-form"
                          onsubmit="return confirm('Cancel this purchase order? This cannot be undone.');">
                        <input type="hidden" name="purchaseOrderId" value="${order.purchaseOrderId}">
                        <button type="submit" class="btn btn-ghost btn-danger">Cancel Order</button>
                    </form>
                </c:if>
            </div>
        </div>
    </main>
</div>

<%@ include file="/WEB-INF/jspf/admin-footer.jspf" %>

</body>
</html>
