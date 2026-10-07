<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="stock-receiving"/>
<c:set var="isEdit" value="${mode == 'edit'}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>${isEdit ? "Sửa Phiếu nhập" : "Nhập kho"} — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <nav class="crumbs" aria-label="Breadcrumb">
            <a href="${ctx}/inventory/receipts">Kho hàng</a>
            <span aria-hidden="true">›</span>
            <span>Nhập kho</span>
        </nav>

        <div class="section-head">
            <div>
                <h2>${isEdit ? 'Sửa Phiếu nhập nháp' : 'Nhập kho'}</h2>
                <p class="section-sub">
                    ${isEdit ? 'Điều chỉnh các dòng lô nhận được. Kho chỉ thay đổi sau khi Xác nhận nhập.'
                             : 'Ghi nhận thuốc mới nhận từ nhà cung cấp theo đơn đặt hàng.'}
                </p>
            </div>
        </div>

        <c:if test="${not empty errors.form}">
            <div class="alert alert-error" role="alert"><c:out value="${errors.form}"/></div>
        </c:if>
        <c:if test="${param.err == 'noteditable'}">
            <div class="alert alert-error" role="alert">Phiếu nhập không còn ở trạng thái nháp — không thể thay đổi.</div>
        </c:if>
        <c:if test="${param.err == 'poclosed'}">
            <div class="alert alert-error" role="alert">Đơn đặt hàng không còn mở để nhận hàng.</div>
        </c:if>
        <c:if test="${param.err == 'itemnotinpo'}">
            <div class="alert alert-error" role="alert">Một dòng nhập không thuộc đơn đặt hàng đã chọn.</div>
        </c:if>
        <c:if test="${param.err == 'batchrequired'}">
            <div class="alert alert-error" role="alert">Số lô là bắt buộc trên mỗi dòng.</div>
        </c:if>
        <c:if test="${param.err == 'expiryrequired'}">
            <div class="alert alert-error" role="alert">Hạn dùng là bắt buộc trên mỗi dòng.</div>
        </c:if>
        <c:if test="${param.err == 'expired'}">
            <div class="alert alert-error" role="alert">Một dòng đã hết hạn vào ngày nhập — đánh dấu Từ chối thay vì Chấp nhận.</div>
        </c:if>
        <c:if test="${param.err == 'badquantity'}">
            <div class="alert alert-error" role="alert">Số lượng phải lớn hơn 0.</div>
        </c:if>
        <c:if test="${param.err == 'badcost'}">
            <div class="alert alert-error" role="alert">Giá nhập phải lớn hơn hoặc bằng 0.</div>
        </c:if>
        <c:if test="${param.err == 'pendingitems'}">
            <div class="alert alert-error" role="alert">Mọi dòng phải được kiểm tra — đặt Chấp nhận hoặc Từ chối trước khi xác nhận.</div>
        </c:if>
        <c:if test="${param.err == 'reasonrequired'}">
            <div class="alert alert-error" role="alert">Lý do từ chối là bắt buộc cho mỗi dòng bị từ chối.</div>
        </c:if>
        <c:if test="${param.err == 'overdelivered'}">
            <div class="alert alert-error" role="alert">Số lượng chấp nhận vượt quá số lượng còn lại trên đơn đặt hàng.</div>
        </c:if>
        <c:if test="${param.err == 'allrejected'}">
            <div class="alert alert-error" role="alert">Phải có ít nhất một dòng được chấp nhận để xác nhận nhập kho.</div>
        </c:if>
        <c:if test="${param.err == 'batchconflict'}">
            <div class="alert alert-error" role="alert">Số lô đã tồn tại với hạn dùng khác — hàng không được nhập.</div>
        </c:if>
        <c:if test="${param.err == 'suppliermismatch' || param.err == 'nopo' || param.err == 'ponotfound'}">
            <div class="alert alert-error" role="alert">Đơn đặt hàng trên phiếu nhập không còn hợp lệ.</div>
        </c:if>
        <c:if test="${param.err == 'noitems'}">
            <div class="alert alert-error" role="alert">Phiếu nhập không có dòng sản phẩm nào để xác nhận.</div>
        </c:if>
        <c:if test="${param.err == 'db'}">
            <div class="alert alert-error" role="alert">Không thể xác nhận phiếu nhập — không có gì được ghi. Vui lòng thử lại.</div>
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
                    <legend>Thông tin phiếu nhập</legend>

                    <div class="profile-grid">
                        <div class="form-field">
                            <label for="purchaseOrderId">Đơn đặt hàng <span class="req">*</span></label>
                            <c:choose>
                                <c:when test="${isEdit}">
                                    <%-- PO is fixed once the draft exists --%>
                                    <input type="text" id="purchaseOrderId" readonly
                                           value="#${receipt.purchaseOrderId} — ${order.supplierName}">
                                </c:when>
                                <c:otherwise>
                                    <select id="purchaseOrderId" name="purchaseOrderId" required>
                                        <option value="">— Chọn đơn đặt hàng —</option>
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
                            <span class="field-hint">Chỉ các đơn đã đặt hoặc nhận một phần mới được hiển thị.</span>
                        </div>

                        <div class="form-field">
                            <label for="supplierName">Nhà cung cấp</label>
                            <input type="text" id="supplierName" readonly
                                   value="<c:out value='${order.supplierName}'/>"
                                   placeholder="— tự động điền từ đơn đặt hàng —">
                        </div>

                        <div class="form-field">
                            <label for="receiptDate">Ngày nhận <span class="req">*</span></label>
                            <input type="date" id="receiptDate" name="receiptDate" required
                                   value="<c:out value='${receipt.receiptDate}'/>">
                            <c:if test="${not empty errors.receiptDate}">
                                <span class="field-error"><c:out value="${errors.receiptDate}"/></span>
                            </c:if>
                        </div>

                        <div class="form-field">
                            <label for="invoiceNumber">Số hóa đơn</label>
                            <input type="text" id="invoiceNumber" name="invoiceNumber" maxlength="100"
                                   value="<c:out value='${receipt.invoiceNumber}'/>"
                                   placeholder="Tham chiếu hóa đơn nhà cung cấp">
                        </div>
                    </div>

                    <div class="form-field">
                        <label for="note">Ghi chú</label>
                        <textarea id="note" name="note" rows="2" maxlength="500"
                                  placeholder="Ghi chú tùy chọn cho phiếu nhập này"><c:out value="${receipt.note}"/></textarea>
                    </div>
                </fieldset>

                <fieldset class="profile-group">
                    <legend>Sản phẩm nhận được</legend>

                    <c:if test="${not empty errors.items}">
                        <span class="field-error"><c:out value="${errors.items}"/></span>
                    </c:if>

                    <table class="admin-table gr-items-table">
                        <thead>
                        <tr>
                            <th style="width:26%">Sản phẩm</th>
                            <th style="width:13%">Số lô</th>
                            <th style="width:11%">Hạn dùng</th>
                            <th style="width:8%">SL</th>
                            <th style="width:11%">Giá nhập</th>
                            <th style="width:11%">Kiểm tra</th>
                            <th style="width:14%">Lý do từ chối</th>
                            <th style="width:6%"></th>
                        </tr>
                        </thead>
                        <tbody id="itemsBody">
                        <%-- Existing rows (edit mode or after a failed submit) --%>
                        <c:forEach var="item" items="${items}">
                            <tr class="gr-item-row">
                                <td>
                                    <select name="poItemId" class="gr-product" required>
                                        <option value="">— Sản phẩm —</option>
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
                                        <option value="CHO_KIEM_TRA" ${item.inspectionResult == 'CHO_KIEM_TRA' ? 'selected' : ''}>Chờ kiểm tra</option>
                                        <option value="CHAP_NHAN"    ${item.inspectionResult == 'CHAP_NHAN'    ? 'selected' : ''}>Chấp nhận</option>
                                        <option value="TU_CHOI"      ${item.inspectionResult == 'TU_CHOI'      ? 'selected' : ''}>Từ chối</option>
                                    </select>
                                </td>
                                <td>
                                    <input type="text" name="rejectionReason" class="gr-reason" maxlength="500"
                                           value="<c:out value='${item.rejectionReason}'/>"
                                           placeholder="VD: Hết hạn, Vỡ hộp"
                                           ${item.inspectionResult == 'TU_CHOI' ? '' : 'hidden'}>
                                </td>
                                <td><button type="button" class="btn btn-ghost btn-sm btn-danger gr-remove">Xóa</button></td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>

                    <div>
                        <button type="button" class="btn btn-secondary btn-sm" id="addItemBtn"
                                ${empty poItems ? 'disabled' : ''}>+ Thêm sản phẩm</button>
                        <span class="field-hint" id="noPoHint"
                              ${empty poItems ? '' : 'hidden'}>Chọn đơn đặt hàng trước — sản phẩm sẽ được tải ở đây.</span>
                        <span class="field-hint">Một sản phẩm trong đơn có thể nhận nhiều lô — thêm dòng khác cho cùng sản phẩm.</span>
                    </div>
                </fieldset>

                <div class="gr-summary" id="grSummary" hidden>
                    <span id="sumAccepted"></span>
                    <span id="sumRejected"></span>
                    <span id="sumValue"></span>
                </div>

                <div class="profile-actions">
                    <a class="btn btn-ghost" href="${ctx}/inventory/receipts">Hủy</a>
                    <button type="submit" name="submitAction" value="draft" class="btn btn-secondary">Lưu nháp</button>
                    <c:if test="${isEdit}">
                        <%-- Confirm is a separate POST so a draft can never be
                             created AND confirmed by one submit. --%>
                        <button type="submit" name="submitAction" value="confirm" class="btn btn-primary"
                                formaction="${ctx}/inventory/receipts?action=confirm"
                                onclick="return confirm('Xác nhận phiếu nhập? Số lượng chấp nhận sẽ vào kho và không thể chỉnh sửa sau đó.');">
                            Xác nhận nhập</button>
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
            if (hasRows && !confirm('Đổi đơn đặt hàng sẽ xóa các dòng sản phẩm hiện tại. Tiếp tục?')) {
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
            hint.textContent = 'Đã đặt: ' + ordered + ' | Đã nhận: ' + received + ' | Còn lại: ' + remaining;
        } else {
            hint.textContent = '';
        }
    }

    function updateReasonVisibility(row) {
        var inspection = row.querySelector('.gr-inspection').value;
        var reason = row.querySelector('.gr-reason');
        if (inspection === 'TU_CHOI') {
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
            if (inspection === 'CHAP_NHAN') {
                acceptedLines++;
                acceptedQty += qty;
                if (!isNaN(cost)) value += qty * cost;
            } else if (inspection === 'TU_CHOI') {
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
            acceptedLines + ' dòng chấp nhận (' + acceptedQty + ' đơn vị sẽ vào kho)';
        document.getElementById('sumRejected').textContent =
            rejectedLines + ' dòng từ chối (' + rejectedQty + ' đơn vị loại ra)';
        document.getElementById('sumValue').textContent =
            'Giá trị chấp nhận: ' + value.toLocaleString('en-US') + ' VND';
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
                <option value="">— Sản phẩm —</option>
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
                <option value="CHO_KIEM_TRA">Chờ kiểm tra</option>
                <option value="CHAP_NHAN">Chấp nhận</option>
                <option value="TU_CHOI">Từ chối</option>
            </select>
        </td>
        <td><input type="text" name="rejectionReason" class="gr-reason" maxlength="500"
                   placeholder="VD: Hết hạn, Vỡ hộp" hidden></td>
        <td><button type="button" class="btn btn-ghost btn-sm btn-danger gr-remove">Xóa</button></td>
    </tr>
</template>

</body>
</html>
