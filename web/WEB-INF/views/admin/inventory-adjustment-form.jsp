<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="inventory-adjustments"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>New Adjustment â€” PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <nav class="crumbs" aria-label="Breadcrumb">
            <a href="${ctx}/inventory">Inventory</a>
            <span aria-hidden="true">â€º</span>
            <a href="${ctx}/inventory?action=batch&id=${batch.batchId}">Batch <c:out value="${batch.batchNumber}"/></a>
            <span aria-hidden="true">â€º</span>
            <span>New Adjustment</span>
        </nav>

        <div class="section-head">
            <div>
                <h2>New Inventory Adjustment</h2>
                <p class="section-sub">Manual correction to this batch's on-hand quantity.</p>
            </div>
            <a class="btn btn-ghost" href="${ctx}/inventory?action=batch&id=${batch.batchId}">â† Back to Batch Detail</a>
        </div>

        <%-- Error messages --%>
        <c:if test="${param.err == 'zerochange'}">
            <div class="alert alert-error" role="alert">Quantity change cannot be zero.</div>
        </c:if>
        <c:if test="${param.err == 'badquantity'}">
            <div class="alert alert-error" role="alert">Quantity change must be a whole number.</div>
        </c:if>
        <c:if test="${param.err == 'negativestock'}">
            <div class="alert alert-error" role="alert">Adjustment would take on-hand below zero.</div>
        </c:if>
        <c:if test="${param.err == 'belowreserved'}">
            <div class="alert alert-error" role="alert">Adjustment would reduce stock below the currently reserved quantity.</div>
        </c:if>
        <c:if test="${param.err == 'invalidreason'}">
            <div class="alert alert-error" role="alert">Please choose a valid reason.</div>
        </c:if>
        <c:if test="${param.err == 'noterequired'}">
            <div class="alert alert-error" role="alert">A note is required when the reason is Other.</div>
        </c:if>
        <c:if test="${param.err == 'notfound'}">
            <div class="alert alert-error" role="alert">Batch not found.</div>
        </c:if>
        <c:if test="${param.err == 'db'}">
            <div class="alert alert-error" role="alert">Database error — the adjustment was not saved.</div>
        </c:if>

        <div class="profile-card">
            <%-- Batch information (read-only) --%>
            <fieldset class="profile-group">
                <legend>Batch Information</legend>
                <div class="profile-grid">
                    <div class="form-field">
                        <label>Product</label>
                        <span><c:out value="${batch.productName}"/></span>
                    </div>
                    <div class="form-field">
                        <label>SKU</label>
                        <span><c:out value="${batch.sku}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Batch</label>
                        <span><strong><c:out value="${batch.batchNumber}"/></strong></span>
                    </div>
                    <div class="form-field">
                        <label>Expiry Date</label>
                        <span><c:out value="${batch.expiryDate}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Status</label>
                        <span class="status-badge batch-${batch.status.toLowerCase().replace('_','-')}">
                            <c:out value="${batch.status}"/>
                        </span>
                    </div>
                </div>
            </fieldset>

            <%-- Current inventory (read-only) --%>
            <fieldset class="profile-group">
                <legend>Current Inventory</legend>
                <div class="profile-grid">
                    <div class="form-field">
                        <label>On Hand</label>
                        <span><strong id="curOnHand"><c:out value="${batch.onHandQuantity}"/></strong></span>
                    </div>
                    <div class="form-field">
                        <label>Reserved</label>
                        <span id="curReserved"><c:out value="${batch.reservedQuantity}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Available</label>
                        <span><c:out value="${batch.onHandQuantity - batch.reservedQuantity}"/></span>
                    </div>
                </div>
            </fieldset>

            <form method="post" action="${ctx}/inventory/adjustments?action=create" id="adjustForm">
                <input type="hidden" name="batchId" value="${batch.batchId}">

                <fieldset class="profile-group">
                    <legend>Adjustment</legend>
                    <div class="form-field">
                        <label for="quantityChange">Quantity Change <span class="req">*</span></label>
                        <input type="number" id="quantityChange" name="quantityChange" required step="1"
                               placeholder="e.g. -5 or +3" aria-describedby="qtyHint">
                        <span class="field-hint" id="qtyHint">Negative removes stock, positive adds it. Cannot be 0.</span>
                    </div>
                    <div class="form-field">
                        <label for="reason">Reason <span class="req">*</span></label>
                        <select id="reason" name="reason" required>
                            <option value="">— Select a reason —</option>
                            <option value="DAMAGED">Damaged</option>
                            <option value="LOST">Lost</option>
                            <option value="EXPIRED">Expired</option>
                            <option value="COUNT_CORRECTION">Count Correction</option>
                            <option value="DATA_CORRECTION">Data Correction</option>
                            <option value="OTHER">Other</option>
                        </select>
                    </div>
                    <div class="form-field">
                        <label for="note">Note</label>
                        <textarea id="note" name="note" rows="3" maxlength="500"
                                  placeholder="Required when reason is Other. e.g. Damaged boxes discovered during inspection."></textarea>
                    </div>
                </fieldset>

                <%-- Live preview — UX only, backend recomputes everything --%>
                <fieldset class="profile-group">
                    <legend>Adjustment Preview</legend>
                    <div class="profile-grid">
                        <div class="form-field">
                            <label>Current On Hand</label>
                            <span><c:out value="${batch.onHandQuantity}"/></span>
                        </div>
                        <div class="form-field">
                            <label>Quantity Change</label>
                            <span id="pvChange">—</span>
                        </div>
                        <div class="form-field">
                            <label>New On Hand</label>
                            <span><strong id="pvAfter">—</strong></span>
                        </div>
                        <div class="form-field">
                            <label>Reserved</label>
                            <span><c:out value="${batch.reservedQuantity}"/></span>
                        </div>
                        <div class="form-field">
                            <label>Available After Adjustment</label>
                            <span><strong id="pvAvail">—</strong></span>
                        </div>
                    </div>
                    <span class="field-hint" id="pvWarn" style="display:none;color:#b91c1c;"></span>
                </fieldset>

                <div class="profile-actions">
                    <a class="btn btn-ghost" href="${ctx}/inventory?action=batch&id=${batch.batchId}">Cancel</a>
                    <button type="submit" class="btn btn-primary">Confirm Adjustment</button>
                </div>
            </form>
        </div>
    </main>
</div>

<%@ include file="/WEB-INF/jspf/admin-footer.jspf" %>

<script>
(function () {
    var onHand = parseInt(document.getElementById('curOnHand').textContent, 10) || 0;
    var reserved = parseInt(document.getElementById('curReserved').textContent, 10) || 0;
    var qty = document.getElementById('quantityChange');
    var pvChange = document.getElementById('pvChange');
    var pvAfter = document.getElementById('pvAfter');
    var pvAvail = document.getElementById('pvAvail');
    var pvWarn = document.getElementById('pvWarn');

    function update() {
        var v = parseInt(qty.value, 10);
        if (isNaN(v)) {
            pvChange.textContent = '—';
            pvAfter.textContent = '—';
            pvAvail.textContent = '—';
            pvWarn.style.display = 'none';
            return;
        }
        var after = onHand + v;
        var avail = after - reserved;
        pvChange.textContent = (v > 0 ? '+' : '') + v;
        pvAfter.textContent = after;
        pvAvail.textContent = avail;
        var msg = '';
        if (v === 0) {
            msg = 'Quantity change cannot be zero.';
        } else if (after < 0) {
            msg = 'This would take on-hand below zero.';
        } else if (after < reserved) {
            msg = 'This would reduce stock below the reserved quantity (' + reserved + ').';
        }
        if (msg) {
            pvWarn.textContent = msg;
            pvWarn.style.display = 'inline';
        } else {
            pvWarn.style.display = 'none';
        }
    }
    qty.addEventListener('input', update);
})();
</script>

</body>
</html>
