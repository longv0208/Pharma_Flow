<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="purchase-orders"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Purchase Orders — Admin — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <div class="section-head">
            <div>
                <h2>Purchase Orders</h2>
                <p class="section-sub">Medicines the pharmacy intends to buy from suppliers. Stock changes only when goods are received.</p>
            </div>
            <a class="btn btn-primary" href="${ctx}/admin/purchase-orders?action=new">+ Create Purchase Order</a>
        </div>

        <%-- Filter bar: GET keeps filters bookmarkable --%>
        <form class="filter-bar" method="get" action="${ctx}/admin/purchase-orders">
            <input type="search" name="q" value="<c:out value='${param.q}'/>"
                   placeholder="Search PO id or supplier name…" aria-label="Search purchase orders">
            <select name="status" aria-label="Status">
                <option value="">All statuses</option>
                <option value="DRAFT"              ${param.status == 'DRAFT'              ? 'selected' : ''}>Draft</option>
                <option value="ORDERED"            ${param.status == 'ORDERED'            ? 'selected' : ''}>Ordered</option>
                <option value="PARTIALLY_RECEIVED" ${param.status == 'PARTIALLY_RECEIVED' ? 'selected' : ''}>Partially Received</option>
                <option value="RECEIVED"           ${param.status == 'RECEIVED'           ? 'selected' : ''}>Received</option>
                <option value="CANCELLED"          ${param.status == 'CANCELLED'          ? 'selected' : ''}>Cancelled</option>
            </select>
            <button type="submit" class="btn btn-secondary btn-sm">Filter</button>
        </form>

        <c:if test="${param.ok == 'created'}">
            <div class="alert alert-success" role="status">Draft purchase order saved.</div>
        </c:if>
        <c:if test="${param.ok == 'updated'}">
            <div class="alert alert-success" role="status">Purchase order updated.</div>
        </c:if>
        <c:if test="${param.ok == 'placed'}">
            <div class="alert alert-success" role="status">Purchase order placed with the supplier.</div>
        </c:if>
        <c:if test="${param.ok == 'cancelled'}">
            <div class="alert alert-success" role="status">Purchase order cancelled.</div>
        </c:if>
        <c:if test="${param.err == 'notfound'}">
            <div class="alert alert-error" role="alert">Purchase order not found.</div>
        </c:if>
        <c:if test="${param.err == 'noteditable'}">
            <div class="alert alert-error" role="alert">Only draft purchase orders can be edited or placed.</div>
        </c:if>
        <c:if test="${param.err == 'notcancellable'}">
            <div class="alert alert-error" role="alert">This purchase order can no longer be cancelled.</div>
        </c:if>
        <c:if test="${param.err == 'invalid'}">
            <div class="alert alert-error" role="alert">The draft is no longer valid — please review and fix it before placing the order.</div>
        </c:if>

        <div class="admin-card">
            <c:choose>
                <c:when test="${empty orders}">
                    <div class="empty-state"><p>No purchase orders found.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="admin-table admin-table-fixed table-purchase-orders">
                        <thead>
                        <tr>
                            <th class="col-id">PO ID</th>
                            <th>Supplier</th>
                            <th class="col-date">Order Date</th>
                            <th class="col-date">Expected Delivery</th>
                            <th class="col-price">Total</th>
                            <th class="col-source">Source</th>
                            <th class="col-status">Status</th>
                            <th class="col-act">Actions</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="po" items="${orders}">
                            <tr>
                                <td>#<c:out value="${po.purchaseOrderId}"/></td>
                                <td><c:out value="${po.supplierName}"/></td>
                                <td><c:out value="${po.orderDate}"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${empty po.expectedDeliveryDate}">—</c:when>
                                        <c:otherwise><c:out value="${po.expectedDeliveryDate}"/></c:otherwise>
                                    </c:choose>
                                </td>
                                <td><fmt:formatNumber value="${po.totalAmount}" type="number" maxFractionDigits="0"/>&#x20AB;</td>
                                <td><c:out value="${po.sourceType}"/></td>
                                <td>
                                    <span class="status-badge ${po.statusCss}">
                                        <c:out value="${po.statusLabel}"/>
                                    </span>
                                </td>
                                <td class="col-actions">
                                    <a class="btn btn-secondary btn-sm"
                                       href="${ctx}/admin/purchase-orders?action=detail&id=${po.purchaseOrderId}">View</a>
                                    <c:if test="${po.editable}">
                                        <a class="btn btn-ghost btn-sm"
                                           href="${ctx}/admin/purchase-orders?action=edit&id=${po.purchaseOrderId}">Edit</a>
                                    </c:if>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </c:otherwise>
            </c:choose>
        </div>
    </main>
</div>

<%@ include file="/WEB-INF/jspf/admin-footer.jspf" %>

</body>
</html>
