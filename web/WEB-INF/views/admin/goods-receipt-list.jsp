<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="stock-receiving"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Stock Receiving — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <div class="section-head">
            <div>
                <h2>Stock Receiving</h2>
                <p class="section-sub">Goods receipts against purchase orders. Only confirmed receipts move stock into inventory.</p>
            </div>
            <a class="btn btn-primary" href="${ctx}/inventory/receipts?action=new">+ Receive Medicine</a>
        </div>

        <%-- Filter bar: GET keeps filters bookmarkable --%>
        <form class="filter-bar" method="get" action="${ctx}/inventory/receipts">
            <input type="search" name="q" value="<c:out value='${param.q}'/>"
                   placeholder="Search receipt id or supplier name…" aria-label="Search receipts">
            <select name="status" aria-label="Status">
                <option value="">All statuses</option>
                <option value="DRAFT"              ${param.status == 'DRAFT'              ? 'selected' : ''}>Draft</option>
                <option value="CONFIRMED"          ${param.status == 'CONFIRMED'          ? 'selected' : ''}>Confirmed</option>
                <option value="PARTIALLY_ACCEPTED" ${param.status == 'PARTIALLY_ACCEPTED' ? 'selected' : ''}>Partially Accepted</option>
                <option value="CANCELLED"          ${param.status == 'CANCELLED'          ? 'selected' : ''}>Cancelled</option>
            </select>
            <button type="submit" class="btn btn-secondary btn-sm">Filter</button>
        </form>

        <c:if test="${param.ok == 'created'}">
            <div class="alert alert-success" role="status">Draft receipt saved — nothing entered inventory yet.</div>
        </c:if>
        <c:if test="${param.ok == 'updated'}">
            <div class="alert alert-success" role="status">Receipt draft updated.</div>
        </c:if>
        <c:if test="${param.ok == 'cancelled'}">
            <div class="alert alert-success" role="status">Draft receipt cancelled.</div>
        </c:if>
        <c:if test="${param.err == 'notfound'}">
            <div class="alert alert-error" role="alert">Goods receipt not found.</div>
        </c:if>
        <c:if test="${param.err == 'noteditable'}">
            <div class="alert alert-error" role="alert">Only draft receipts can be edited.</div>
        </c:if>
        <c:if test="${param.err == 'notcancellable'}">
            <div class="alert alert-error" role="alert">This receipt can no longer be cancelled.</div>
        </c:if>

        <div class="admin-card">
            <c:choose>
                <c:when test="${empty receipts}">
                    <div class="empty-state"><p>No goods receipts found.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="admin-table admin-table-fixed table-goods-receipts">
                        <thead>
                        <tr>
                            <th class="col-id">Receipt</th>
                            <th class="col-po">PO</th>
                            <th>Supplier</th>
                            <th class="col-date">Receipt Date</th>
                            <th class="col-invoice">Invoice</th>
                            <th class="col-status">Status</th>
                            <th>Received By</th>
                            <th class="col-act">Actions</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="r" items="${receipts}">
                            <tr>
                                <td>#<c:out value="${r.goodsReceiptId}"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${empty r.purchaseOrderId}">—</c:when>
                                        <c:otherwise>#<c:out value="${r.purchaseOrderId}"/></c:otherwise>
                                    </c:choose>
                                </td>
                                <td><c:out value="${r.supplierName}"/></td>
                                <td><c:out value="${r.receiptDate}"/></td>
                                <td><c:out value="${empty r.invoiceNumber ? '—' : r.invoiceNumber}"/></td>
                                <td>
                                    <span class="status-badge ${r.statusCss}">
                                        <c:out value="${r.statusLabel}"/>
                                    </span>
                                </td>
                                <td><c:out value="${r.receivedByName}"/></td>
                                <td class="col-actions">
                                    <a class="btn btn-secondary btn-sm"
                                       href="${ctx}/inventory/receipts?action=detail&id=${r.goodsReceiptId}">View</a>
                                    <c:if test="${r.editable}">
                                        <a class="btn btn-ghost btn-sm"
                                           href="${ctx}/inventory/receipts?action=edit&id=${r.goodsReceiptId}">Edit</a>
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
