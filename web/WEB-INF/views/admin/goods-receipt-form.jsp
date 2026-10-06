<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="stock-receiving"/>
<c:set var="isEdit" value="${mode == 'edit'}"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>${isEdit ? "Edit Receipt" : "Receive Stock"} — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <nav class="crumbs" aria-label="Breadcrumb">
            <a href="${ctx}/inventory/receipts">Inventory</a>
            <span aria-hidden="true">›</span>
            <span>Stock Receiving</span>
        </nav>

        <div class="section-head">
            <div>
                <h2>${isEdit ? 'Edit Draft Receipt' : 'Receive Stock'}</h2>
                <p class="section-sub">
                    ${isEdit ? 'Adjust the received batch lines. Inventory only changes after Confirm Receipt.'
                             : 'Record newly received medicines from a supplier delivery against a purchase order.'}
                </p>
            </div>
        </div>

        <c:if test="${not empty errors.form}">
            <div class="alert alert-error" role="alert"><c:out value="${errors.form}"/></div>
        </c:if>
        <c:if test="${param.err == 'noteditable'}">
            <div class="alert alert-error" role="alert">This receipt is no longer a draft — it cannot be changed.</div>
        </c:if>
        <c:if test="${param.err == 'poclosed'}">
            <div class="alert alert-error" role="alert">The purchase order is no longer open for receiving.</div>
        </c:if>
        <c:if test="${param.err == 'itemnotinpo'}">
            <div class="alert alert-error" role="alert">A receipt line does not belong to the selected purchase order.</div>
        </c:if>
        <c:if test="${param.err == 'batchrequired'}">
            <div class="alert alert-error" role="alert">Batch number is required on every line.</div>
        </c:if>
        <c:if test="${param.err == 'expiryrequired'}">
            <div class="alert alert-error" role="alert">Expiry date is required on every line.</div>
        </c:if>
        <c:if test="${param.err == 'expired'}">
            <div class="alert alert-error" role="alert">A line is expired on the receipt date — mark it Rejected instead of Accepted.</div>
        </c:if>
        <c:if test="${param.err == 'badquantity'}">
            <div class="alert alert-error" role="alert">Quantity must be greater than 0.</div>
        </c:if>
        <c:if test="${param.err == 'badcost'}">
            <div class="alert alert-error" role="alert">Cost price must be 0 or greater.</div>
        </c:if>
        <c:if test="${param.err == 'pendingitems'}">
            <div class="alert alert-error" role="alert">Every line must be inspected — set Accepted or Rejected before confirming.</div>
        </c:if>
        <c:if test="${param.err == 'reasonrequired'}">
            <div class="alert alert-error" role="alert">A rejection reason is required for every rejected line.</div>
        </c:if>
        <c:if test="${param.err == 'overdelivered'}">
            <div class="alert alert-error" role="alert">Accepted quantity exceeds the remaining quantity on the purchase order.</div>
        </c:if>
        <c:if test="${param.err == 'allrejected'}">
            <div class="alert alert-error" role="alert">At least one line must be accepted to confirm a stock receipt.</div>
        </c:if>
        <c:if test="${param.err == 'batchconflict'}">
            <div class="alert alert-error" role="alert">A received batch number already exists with a different expiry date — the delivery was not received.</div>
        </c:if>
        <c:if test="${param.err == 'suppliermismatch' || param.err == 'nopo' || param.err == 'ponotfound'}">
            <div class="alert alert-error" role="alert">The purchase order on this receipt is no longer valid.</div>
        </c:if>
        <c:if test="${param.err == 'noitems'}">
            <div class="alert alert-error" role="alert">The receipt has no item lines to confirm.</div>
        </c:if>
        <c:if test="${param.err == 'db'}">
            <div class="alert alert-error" role="alert">Could not confirm the receipt — nothing was written. Please try again.</div>
        </c:if>

        <c:choose>
            <c:when test="${isEdit}">
                <c:set var="reloadUrl" value="${ctx}/inventory/receipts?action=edit&id=${receipt.goodsReceiptId}"/>
            </c:when>
            <c:otherwise>
                <c:set var="reloadUrl" value="${ctx}/inventory/receipts?action=new"/>
            </c:otherwise>
        </c:choose>

        <div class="profile-card">
            <form class="profile-form" method="post" id="grForm"
                  action="${ctx}/inventory/receipts?action=${isEdit ? 'update' : 'create'}"
                  data-reload-url="${reloadUrl}">

                <c:if test="${isEdit}">
                    <input type="hidden" name="goodsReceiptId" value="${receipt.goodsReceiptId}">
                    <%-- edit keeps the original PO — the select is locked --%>
                    <input type="hidden" name="purchaseOrderId" value="${receipt.purchaseOrderId}">
                </c:if>

                <fieldset class="profile-group">
                    <legend>Receipt Information</legend>

                    <div class="profile-grid">
                        <div class="form-field">
                            <label for="purchaseOrderId">Purchase Order <span class="req">*</span></label>
                            <c:choose>
                                <c:when test="${isEdit}">
                                    <%-- PO is fixed once the draft exists --%>
                                    <input type="text" id="purchaseOrderId" readonly
                                           value="#${receipt.purchaseOrderId} — ${order.supplierName}">
                                </c:when>
                                <c:otherwise>
                                    <select id="purchaseOrderId" name="purchaseOrderId" required>
                                        <option value="">— Choose purchase order —</option>
                                        <c:forEach var="po" items="${receivableOrders}">
                                            <option value="${po.purchaseOrderId}"
                                                    ${receipt.purchaseOrderId == po.purchaseOrderId ? 'selected' : ''}>
                                                #<c:out value="${po.purchaseOrderId}"/> — <c:out value="${po.supplierName}"/>
                                                (<c:out value="${po.orderDate}"/>)
                                            </option>
                                        </c:forEach>
                                    </select>
                                </c:otherwise>
                            </c:choose>
                            <c:if test="${not empty errors.purchaseOrderId}">
                                <span class="field-error"><c:out value="${errors.purchaseOrderId}"/></span>
                            </c:if>
                            <span class="field-hint">Only ordered / partially received POs are listed.</span>
                        </div>

                        <div class="form-field">
                            <label for="supplierName">Supplier</label>
                            <input type="text" id="supplierName" readonly
                                   value="<c:out value='${order.supplierName}'/>"
                                   placeholder="— filled from the purchase order —">
                        </div>

                        <div class="form-field">
                            <label for="receiptDate">Receipt Date <span class="req">*</span></label>
                            <input type="date" id="receiptDate" name="receiptDate" required
                                   value="<c:out value='${receipt.receiptDate}'/>">
                            <c:if test="${not empty errors.receiptDate}">
                                <span class="field-error"><c:out value="${errors.receiptDate}"/></span>
                            </c:if>
                        </div>

                        <div class="form-field">
                            <label for="invoiceNumber">Invoice Number</label>
                            <input type="text" id="invoiceNumber" name="invoiceNumber" maxlength="100"
                                   value="<c:out value='${receipt.invoiceNumber}'/>"
                                   placeholder="Supplier invoice reference">
                        </div>
                    </div>

                    <div class="form-field">
                        <label for="note">Note / Remarks</label>
                        <textarea id="note" name="note" rows="2" maxlength="500"
                                  placeholder="Optional note for this receipt"><c:out value="${receipt.note}"/></textarea>
                    </div>
                </fieldset>

                <fieldset class="profile-group">
                    <legend>Received Items</legend>

                    <c:if test="${not empty errors.items}">
                        <span class="field-error"><c:out value="${errors.items}"/></span>
                    </c:if>

                    <table class="admin-table gr-items-table">
                        <thead>
                        <tr>
                            <th style="width:26%">Product</th>
                            <th style="width:13%">Batch Number</th>
                            <th style="width:11%">Expiry Date</th>
                            <th style="width:8%">Qty</th>
                            <th style="width:11%">Cost Price</th>
                            <th style="width:11%">Inspection</th>
                            <th style="width:14%">Rejection Reason</th>
                            <th style="width:6%"></th>
                        </tr>
                        </thead>
                        <tbody id="itemsBody">
                        <%-- Existing rows (edit mode or after a failed submit) --%>
                        <c:forEach var="item" items="${items}">
                            <tr class="gr-item-row">
                                <td>
                                    <select name="poItemId" class="gr-product" required>
                                        <option value="">— product —</option>
                                        <c:forEach var="p" items="${poItems}">
                                            <option value="${p.purchaseOrderItemId}"
                                                    data-product-id="${p.productId}"
                                                    data-ordered="${p.orderedQuantity}"
                                                    data-received="${p.receivedQuantity}"
                                                    data-remaining="${p.orderedQuantity - p.receivedQuantity}"
                                                    data-cost="<c:out value='${p.unitCost}'/>"
                                                    ${item.purchaseOrderItemId == p.purchaseOrderItemId ? 'selected' : ''}>
                                                <c:out value="${p.productName}"/> (<c:out value="${p.sku}"/>)
                                            </option>
                                        </c:forEach>
                                        <c:if test="${not empty item.productName}">
                                            <option value="${item.purchaseOrderItemId}" selected
                                                    data-product-id="${item.productId}" data-ordered="" data-received="" data-remaining="" data-cost="">
                                                <c:out value="${item.productName}"/>
                                            </option>
                                        </c:if>
                                    </select>
                                    <span class="gr-remaining field-hint"></span>
                                </td>
                                <td><input type="text" name="batchNumber" class="gr-batch" maxlength="100"
                                           value="<c:out value='${item.batchNumber}'/>" required></td>
                                <td><input type="date" name="expiryDate" class="gr-expiry"
                                           value="<c:out value='${item.expiryDate}'/>" required></td>
                                <td><input type="number" name="quantity" class="gr-qty" min="1" step="1"
                                           value="<c:out value='${item.quantity}'/>" required></td>
                                <td><input type="number" name="costPrice" class="gr-cost" min="0" step="1"
                                           value="<c:out value='${item.costPrice}'/>" required></td>
                                <td>
                                    <select name="inspectionResult" class="gr-inspection">
                                        <option value="PENDING"  ${item.inspectionResult == 'PENDING'  ? 'selected' : ''}>Pending</option>
                                        <option value="ACCEPTED" ${item.inspectionResult == 'ACCEPTED' ? 'selected' : ''}>Accepted</option>
                                        <option value="REJECTED" ${item.inspectionResult == 'REJECTED' ? 'selected' : ''}>Rejected</option>
                                    </select>
                                </td>
                                <td>
                                    <input type="text" name="rejectionReason" class="gr-reason" maxlength="500"
                                           value="<c:out value='${item.rejectionReason}'/>"
                                           placeholder="e.g. Expired, Damaged Package"
                                           ${item.inspectionResult == 'REJECTED' ? '' : 'hidden'}>
                                </td>
                                <td><button type="button" class="btn btn-ghost btn-sm btn-danger gr-remove">Remove</button></td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>

                    <div>
                        <button type="button" class="btn btn-secondary btn-sm" id="addItemBtn"
                                ${empty poItems ? 'disabled' : ''}>+ Add Item</button>
                        <span class="field-hint" id="noPoHint"
                              ${empty poItems ? '' : 'hidden'}>Pick a purchase order first — its products are loaded here.</span>
                        <span class="field-hint">One PO product can be delivered in several batches — add another row for the same product.</span>
                    </div>
                </fieldset>

                <div class="gr-summary" id="grSummary" hidden>
                    <span id="sumAccepted"></span>
                    <span id="sumRejected"></span>
                    <span id="sumValue"></span>
                </div>

                <div class="profile-actions">
                    <a class="btn btn-ghost" href="${ctx}/inventory/receipts">Cancel</a>
                    <button type="submit" name="submitAction" value="draft" class="btn btn-secondary">Save Draft</button>
                    <c:if test="${isEdit}">
                        <%-- Confirm is a separate POST so a draft can never be
                             created AND confirmed by one submit. --%>
                        <button type="submit" name="submitAction" value="confirm" class="btn btn-primary"
                                formaction="${ctx}/inventory/receipts?action=confirm"
                                onclick="return confirm('Confirm this receipt? Accepted quantities will enter inventory and cannot be edited here afterwards.');">
                            Confirm Receipt</button>
                    </c:if>
                </div>
            </form>
        </div>
    </main>
</div>

<%@ include file="/WEB-INF/jspf/admin-footer.jspf" %>

<script>
(function () {
    var poSelect = document.getElementById('purchaseOrderId');
    var form = document.getElementById('grForm');
    var itemsBody = document.getElementById('itemsBody');
    var addBtn = document.getElementById('addItemBtn');

    // Picking a PO reloads the form (GET) so item rows are rebuilt server-side
    // with ordered/received/remaining context — there is no JSON API here.
    if (poSelect && poSelect.tagName === 'SELECT') {
        poSelect.addEventListener('change', function () {
            var pid = poSelect.value;
            var hasRows = itemsBody.querySelectorAll('.gr-item-row').length > 0;
            if (hasRows && !confirm('Changing the purchase order clears the current item lines. Continue?')) {
                poSelect.value = poSelect.getAttribute('data-prev') || '';
                return;
            }
            var url = form.getAttribute('data-reload-url');
            if (pid) {
                url += '&po=' + pid;
            }
            // keep entered fields across the reload
            url += '&receiptDate=' + encodeURIComponent(document.getElementById('receiptDate').value);
            url += '&invoiceNumber=' + encodeURIComponent(document.getElementById('invoiceNumber').value);
            url += '&note=' + encodeURIComponent(document.getElementById('note').value);
            window.location.href = url;
        });
        poSelect.setAttribute('data-prev', poSelect.value);
    }

    function updateRemaining(row, opt) {
        var hint = row.querySelector('.gr-remaining');
        if (!hint) return;
        var remaining = opt.getAttribute('data-remaining');
        var ordered = opt.getAttribute('data-ordered');
        var received = opt.getAttribute('data-received');
        if (remaining !== null && remaining !== '') {
            hint.textContent = 'Ordered: ' + ordered + ' | Received: ' + received + ' | Remaining: ' + remaining;
        } else {
            hint.textContent = '';
        }
    }

    function updateReasonVisibility(row) {
        var inspection = row.querySelector('.gr-inspection').value;
        var reason = row.querySelector('.gr-reason');
        if (inspection === 'REJECTED') {
            reason.hidden = false;
            reason.required = true;
        } else {
            reason.hidden = true;
            reason.required = false;
        }
    }

    function updateSummary() {
        var rows = itemsBody.querySelectorAll('.gr-item-row');
        var acceptedLines = 0, acceptedQty = 0, rejectedLines = 0, rejectedQty = 0, value = 0;
        for (var i = 0; i < rows.length; i++) {
            var row = rows[i];
            var qty = parseInt(row.querySelector('.gr-qty').value, 10);
            var cost = parseFloat(row.querySelector('.gr-cost').value);
            var inspection = row.querySelector('.gr-inspection').value;
            if (isNaN(qty) || qty <= 0) continue;
            if (inspection === 'ACCEPTED') {
                acceptedLines++;
                acceptedQty += qty;
                if (!isNaN(cost)) value += qty * cost;
            } else if (inspection === 'REJECTED') {
                rejectedLines++;
                rejectedQty += qty;
            }
        }
        var box = document.getElementById('grSummary');
        if (rows.length === 0) {
            box.hidden = true;
            return;
        }
        box.hidden = false;
        document.getElementById('sumAccepted').textContent =
            acceptedLines + ' item(s) accepted (' + acceptedQty + ' units will be added to inventory)';
        document.getElementById('sumRejected').textContent =
            rejectedLines + ' item(s) rejected (' + rejectedQty + ' units excluded)';
        document.getElementById('sumValue').textContent =
            'Accepted value: ' + value.toLocaleString('en-US') + ' VND';
    }

    // add a new blank batch row cloning the hidden template
    addBtn.addEventListener('click', function () {
        var tpl = document.getElementById('grRowTemplate');
        var clone = tpl.content.cloneNode(true);
        itemsBody.appendChild(clone);
    });

    // delegated: PO product picked → fill cost + remaining hint;
    //            inspection changed → toggle rejection reason;
    //            any field → refresh summary
    itemsBody.addEventListener('change', function (e) {
        var row = e.target.closest('tr');
        if (e.target.classList.contains('gr-product')) {
            var opt = e.target.options[e.target.selectedIndex];
            updateRemaining(row, opt);
            var cost = opt.getAttribute('data-cost');
            var costInput = row.querySelector('.gr-cost');
            if (cost && costInput) {
                costInput.value = cost;
            }
        }
        if (e.target.classList.contains('gr-inspection')) {
            updateReasonVisibility(row);
        }
        updateSummary();
    });
    itemsBody.addEventListener('input', function (e) {
        if (e.target.classList.contains('gr-qty') || e.target.classList.contains('gr-cost')) {
            updateSummary();
        }
    });
    itemsBody.addEventListener('click', function (e) {
        if (!e.target.classList.contains('gr-remove')) return;
        e.target.closest('tr').remove();
        updateSummary();
    });

    // Prevent duplicate submits — disable ALL submit buttons after one fires
    form.addEventListener('submit', function () {
        var buttons = form.querySelectorAll('button[type="submit"]');
        for (var i = 0; i < buttons.length; i++) {
            buttons[i].disabled = true;
        }
    });

    // initial paint for repopulated rows
    var initialRows = itemsBody.querySelectorAll('.gr-item-row');
    for (var i = 0; i < initialRows.length; i++) {
        var sel = initialRows[i].querySelector('.gr-product');
        if (sel && sel.selectedIndex >= 0) {
            updateRemaining(initialRows[i], sel.options[sel.selectedIndex]);
        }
        updateReasonVisibility(initialRows[i]);
    }
    updateSummary();
})();
</script>

<%-- Row template cloned by "Add Item" — keeps markup in one place --%>
<template id="grRowTemplate">
    <tr class="gr-item-row">
        <td>
            <select name="poItemId" class="gr-product" required>
                <option value="">— product —</option>
                <c:forEach var="p" items="${poItems}">
                    <option value="${p.purchaseOrderItemId}"
                            data-product-id="${p.productId}"
                            data-ordered="${p.orderedQuantity}"
                            data-received="${p.receivedQuantity}"
                            data-remaining="${p.orderedQuantity - p.receivedQuantity}"
                            data-cost="<c:out value='${p.unitCost}'/>">
                        <c:out value="${p.productName}"/> (<c:out value="${p.sku}"/>)
                    </option>
                </c:forEach>
            </select>
            <span class="gr-remaining field-hint"></span>
        </td>
        <td><input type="text" name="batchNumber" class="gr-batch" maxlength="100" required></td>
        <td><input type="date" name="expiryDate" class="gr-expiry" required></td>
        <td><input type="number" name="quantity" class="gr-qty" min="1" step="1" required></td>
        <td><input type="number" name="costPrice" class="gr-cost" min="0" step="1" required></td>
        <td>
            <select name="inspectionResult" class="gr-inspection">
                <option value="PENDING">Pending</option>
                <option value="ACCEPTED">Accepted</option>
                <option value="REJECTED">Rejected</option>
            </select>
        </td>
        <td><input type="text" name="rejectionReason" class="gr-reason" maxlength="500"
                   placeholder="e.g. Expired, Damaged Package" hidden></td>
        <td><button type="button" class="btn btn-ghost btn-sm btn-danger gr-remove">Remove</button></td>
    </tr>
</template>

</body>
</html>
