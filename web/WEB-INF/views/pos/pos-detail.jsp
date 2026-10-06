<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="pos"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Sale #${sale.saleTransactionId} — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <nav class="crumbs" aria-label="Breadcrumb">
            <span>Sales</span>
            <span aria-hidden="true">›</span>
            <a href="${ctx}/pos?action=history">History</a>
            <span aria-hidden="true">›</span>
            <span>Sale #<c:out value="${sale.saleTransactionId}"/></span>
        </nav>

        <div class="section-head">
            <div>
                <h2>Sale #<c:out value="${sale.saleTransactionId}"/></h2>
                <p class="section-sub">Receipt — a completed sale is read-only audit history.</p>
            </div>
            <a class="btn btn-primary btn-sm" href="${ctx}/pos">New Sale</a>
        </div>

        <c:if test="${param.ok == 'completed'}">
            <div class="alert alert-success">Sale completed — stock has been deducted via FEFO allocation.</div>
        </c:if>

        <%-- ==================== header ==================== --%>
        <div class="admin-card">
            <div class="profile-grid">
                <div class="profile-group">
                    <span class="field-hint">Date / Time</span>
                    <strong><fmt:formatDate value="${sale.saleDatetime}" pattern="yyyy-MM-dd HH:mm"/></strong>
                </div>
                <div class="profile-group">
                    <span class="field-hint">Staff</span>
                    <strong><c:out value="${sale.staffName}"/></strong>
                </div>
                <div class="profile-group">
                    <span class="field-hint">Payment</span>
                    <strong><c:out value="${sale.paymentLabel}"/></strong>
                </div>
                <div class="profile-group">
                    <span class="field-hint">Status</span>
                    <strong><span class="status-badge ${sale.statusCss}"><c:out value="${sale.statusLabel}"/></span></strong>
                </div>
                <c:if test="${prescription != null}">
                    <div class="profile-group">
                        <span class="field-hint">Prescription</span>
                        <strong><c:out value="${prescription.prescriptionCode}"/></strong>
                        <div class="field-hint">
                            <c:out value="${prescription.patientName}"/> — <c:out value="${prescription.prescriber}"/>
                        </div>
                    </div>
                </c:if>
            </div>
        </div>

        <%-- ==================== lines ==================== --%>
        <div class="admin-card">
            <h3 class="pos-card-title">Items</h3>
            <table class="admin-table">
                <thead>
                <tr>
                    <th>Product</th>
                    <th>Type</th>
                    <th class="col-num">Unit Price</th>
                    <th class="col-num">Qty</th>
                    <th class="col-num">Subtotal</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="item" items="${items}">
                    <tr>
                        <td>
                            <c:out value="${item.productName}"/>
                            <div class="field-hint"><c:out value="${item.sku}"/> · <c:out value="${item.sellingUnit}"/></div>
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${item.productType == 'RX'}">
                                    <span class="type-badge type-rx">Rx</span>
                                </c:when>
                                <c:when test="${item.productType == 'RESTRICTED'}">
                                    <span class="type-badge type-restricted">Restricted</span>
                                </c:when>
                                <c:otherwise>
                                    <span class="type-badge type-otc">OTC</span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td class="col-num"><fmt:formatNumber value="${item.unitPrice}" type="number" groupingUsed="true"/></td>
                        <td class="col-num"><c:out value="${item.quantity}"/></td>
                        <td class="col-num"><fmt:formatNumber value="${item.subtotal}" type="number" groupingUsed="true"/></td>
                    </tr>
                </c:forEach>
                </tbody>
                <tfoot>
                <tr>
                    <td colspan="4" class="col-num"><strong>Total</strong></td>
                    <td class="col-num"><strong><fmt:formatNumber value="${sale.totalAmount}" type="number" groupingUsed="true"/> ₫</strong></td>
                </tr>
                </tfoot>
            </table>
        </div>

        <%-- ==================== batch traceability ==================== --%>
        <div class="admin-card">
            <h3 class="pos-card-title">Batch Allocations</h3>
            <p class="field-hint">Which batches the units came from — FEFO (earliest expiry first).</p>
            <c:choose>
                <c:when test="${empty allocations}">
                    <div class="empty-state"><p>No allocations recorded.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="admin-table">
                        <thead>
                        <tr>
                            <th>Product</th>
                            <th>Batch</th>
                            <th>Expiry</th>
                            <th class="col-num">Qty</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="a" items="${allocations}">
                            <tr>
                                <td><c:out value="${a.productName}"/></td>
                                <td>
                                    <a href="${ctx}/inventory?action=batch&id=${a.batchId}">
                                        <c:out value="${a.batchNumber}"/>
                                    </a>
                                </td>
                                <td><fmt:formatDate value="${a.expiryDate}" pattern="yyyy-MM-dd"/></td>
                                <td class="col-num"><c:out value="${a.quantity}"/></td>
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
