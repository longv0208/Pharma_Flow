<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="inventory"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Batch <c:out value="${batch.batchNumber}"/> — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <nav class="crumbs" aria-label="Breadcrumb">
            <a href="${ctx}/inventory">Inventory</a>
            <span aria-hidden="true">›</span>
            <a href="${ctx}/inventory?action=product&id=${batch.productId}"><c:out value="${batch.productName}"/></a>
            <span aria-hidden="true">›</span>
            <span>Batch <c:out value="${batch.batchNumber}"/></span>
        </nav>

        <div class="section-head">
            <div>
                <h2>Batch <c:out value="${batch.batchNumber}"/></h2>
                <p class="section-sub">Batch detail — traceability and status control.</p>
            </div>
            <a class="btn btn-ghost" href="${ctx}/inventory?action=product&id=${batch.productId}">← Back to Product</a>
        </div>

        <c:if test="${param.ok == 'blocked'}">
            <div class="alert alert-success" role="status">Batch blocked successfully.</div>
        </c:if>
        <c:if test="${param.ok == 'unblocked'}">
            <div class="alert alert-success" role="status">Batch unblocked successfully.</div>
        </c:if>
        <c:if test="${param.err == 'reasonrequired'}">
            <div class="alert alert-error" role="alert">A reason is required to block a batch.</div>
        </c:if>
        <c:if test="${param.err == 'alreadyblocked'}">
            <div class="alert alert-error" role="alert">This batch is already blocked.</div>
        </c:if>
        <c:if test="${param.err == 'notblocked'}">
            <div class="alert alert-error" role="alert">This batch is not currently blocked.</div>
        </c:if>
        <c:if test="${param.err == 'expiredbatch'}">
            <div class="alert alert-error" role="alert">Cannot unblock an expired batch — it stays blocked.</div>
        </c:if>
        <c:if test="${param.err == 'expired'}">
            <div class="alert alert-error" role="alert">An expired batch cannot be blocked — it is already unusable.</div>
        </c:if>
        <c:if test="${param.err == 'notfound'}">
            <div class="alert alert-error" role="alert">Batch not found.</div>
        </c:if>
        <c:if test="${param.err == 'db'}">
            <div class="alert alert-error" role="alert">Database error — the status change was not saved.</div>
        </c:if>

        <div class="profile-card">
            <%-- Basic information --%>
            <fieldset class="profile-group">
                <legend>Basic Information</legend>
                <div class="profile-grid">
                    <div class="form-field">
                        <label>Product Name</label>
                        <span><c:out value="${batch.productName}"/></span>
                    </div>
                    <div class="form-field">
                        <label>SKU</label>
                        <span><c:out value="${batch.sku}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Batch Number</label>
                        <span><strong><c:out value="${batch.batchNumber}"/></strong></span>
                    </div>
                    <div class="form-field">
                        <label>Expiry Date</label>
                        <span>
                            <c:out value="${batch.expiryDate}"/>
                            <c:if test="${expiryWarning == 'EXPIRED'}">
                                <span class="status-badge batch-expired">Expired</span>
                            </c:if>
                            <c:if test="${expiryWarning == 'NEAR_EXPIRY'}">
                                <span class="status-badge batch-near-expiry">Near Expiry</span>
                            </c:if>
                        </span>
                    </div>
                    <div class="form-field">
                        <label>Supplier</label>
                        <span><c:out value="${batch.supplierName}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Goods Receipt</label>
                        <span>
                            <c:choose>
                                <c:when test="${empty batch.sourceGoodsReceiptId}">—</c:when>
                                <c:otherwise>#<c:out value="${batch.sourceGoodsReceiptId}"/></c:otherwise>
                            </c:choose>
                        </span>
                    </div>
                    <div class="form-field">
                        <label>Unit Cost Price</label>
                        <span><fmt:formatNumber value="${batch.costPrice}" type="number" maxFractionDigits="0"/>&#x20AB;</span>
                    </div>
                    <div class="form-field">
                        <label>Storage Location</label>
                        <span><c:out value="${empty batch.storageLocation ? '—' : batch.storageLocation}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Batch Status</label>
                        <span class="status-badge batch-${batch.status.toLowerCase().replace('_','-')}">
                            <c:out value="${batch.status}"/>
                        </span>
                    </div>
                </div>
            </fieldset>

            <%-- Quantity --%>
            <fieldset class="profile-group">
                <legend>Quantity</legend>
                <div class="profile-grid">
                    <div class="form-field">
                        <label>On Hand</label>
                        <span><strong><c:out value="${batch.onHandQuantity}"/></strong></span>
                    </div>
                    <div class="form-field">
                        <label>Reserved</label>
                        <span><c:out value="${batch.reservedQuantity}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Available</label>
                        <span><strong><c:out value="${batch.onHandQuantity - batch.reservedQuantity}"/></strong></span>
                    </div>
                    <c:if test="${batch.reservedQuantity > 0}">
                        <div class="form-field">
                            <label>Notice</label>
                            <span class="field-hint">This batch has reserved stock.</span>
                        </div>
                    </c:if>
                </div>
            </fieldset>

            <%-- Actions --%>
            <div class="profile-actions">
                <a class="btn btn-secondary" href="${ctx}/inventory?action=history&batchId=${batch.batchId}">
                    View Inventory History
                </a>

                <c:if test="${batch.status != 'BLOCKED'}">
                    <button type="button" class="btn btn-ghost btn-danger"
                            onclick="document.getElementById('blockForm').hidden = false;">
                        Block Batch
                    </button>
                </c:if>
                <c:if test="${batch.status == 'BLOCKED'}">
                    <form method="post" action="${ctx}/inventory?action=unblock-batch" class="inline-form"
                          onsubmit="return confirm('Unblock this batch?');">
                        <input type="hidden" name="batchId" value="${batch.batchId}">
                        <button type="submit" class="btn btn-primary">Unblock Batch</button>
                    </form>
                </c:if>
            </div>

            <%-- Block form (hidden until clicked) --%>
            <c:if test="${batch.status != 'BLOCKED'}">
                <form method="post" action="${ctx}/inventory?action=block-batch" id="blockForm" hidden>
                    <input type="hidden" name="batchId" value="${batch.batchId}">
                    <fieldset class="profile-group">
                        <legend>Block Batch</legend>
                        <div class="form-field">
                            <label for="reason">Reason <span class="req">*</span></label>
                            <textarea id="reason" name="reason" rows="3" maxlength="500" required
                                      placeholder="e.g. Damaged packaging, Quality concern, Recall, Storage issue, Investigation, Other"></textarea>
                        </div>
                        <div class="profile-actions">
                            <button type="submit" class="btn btn-danger">Confirm Block</button>
                            <button type="button" class="btn btn-ghost"
                                    onclick="document.getElementById('blockForm').hidden = true;">Cancel</button>
                        </div>
                    </fieldset>
                </form>
            </c:if>
        </div>
    </main>
</div>

<%@ include file="/WEB-INF/jspf/admin-footer.jspf" %>

</body>
</html>
