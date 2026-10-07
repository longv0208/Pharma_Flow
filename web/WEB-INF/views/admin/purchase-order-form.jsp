<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="purchase-orders"/>
<c:set var="isEdit" value="${mode == 'edit'}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>${isEdit ? "Sửa" : "Tạo"} Đơn đặt hàng — Admin — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <div class="section-head">
            <div>
                <h2>${isEdit ? 'Sửa Đơn đặt hàng nháp' : 'Tạo Đơn đặt hàng'}</h2>
                <p class="section-sub">
                    ${isEdit ? 'Điều chỉnh nhà cung cấp, ngày tháng và danh sách sản phẩm. Tổng tiền sẽ được tính lại khi lưu.'
                             : 'Chọn nhà cung cấp đang hoạt động, sau đó thêm thuốc cần đặt. Kho hàng chưa bị trừ.'}
                </p>
            </div>
        </div>

        <c:if test="${not empty errors.form}">
            <div class="alert alert-error" role="alert"><c:out value="${errors.form}"/></div>
        </c:if>
        <c:if test="${param.err == 'invalid'}">
            <div class="alert alert-error" role="alert">Bản nháp không còn hợp lệ — vui lòng kiểm tra lại sản phẩm trước khi đặt hàng.</div>
        </c:if>

        <c:choose>
            <c:when test="${isEdit}">
                <c:set var="reloadUrl" value="${ctx}/admin/purchase-orders?action=edit&id=${order.purchaseOrderId}"/>
            </c:when>
            <c:otherwise>
                <c:set var="reloadUrl" value="${ctx}/admin/purchase-orders?action=new"/>
            </c:otherwise>
        </c:choose>

        <div class="profile-card">
            <form class="profile-form" method="post" id="poForm"
                  action="${ctx}/admin/purchase-orders?action=${isEdit ? 'update' : 'create'}"
                  data-reload-url="${reloadUrl}">

                <c:if test="${isEdit}">
                    <input type="hidden" name="purchaseOrderId" value="${order.purchaseOrderId}">
                </c:if>

                <fieldset class="profile-group">
                    <legend>Thông tin đơn đặt hàng</legend>

                    <div class="profile-grid">
                        <div class="form-field">
                            <label for="supplierId">Nhà cung cấp <span class="req">*</span></label>
                            <select id="supplierId" name="supplierId" required>
                                <option value="">— Chọn nhà cung cấp —</option>
                                <c:forEach var="s" items="${suppliers}">
                                    <option value="${s.supplierId}"
                                            ${order.supplierId == s.supplierId ? 'selected' : ''}>
                                        <c:out value="${s.supplierName}"/>
                                    </option>
                                </c:forEach>
                            </select>
                            <c:if test="${not empty errors.supplierId}">
                                <span class="field-error"><c:out value="${errors.supplierId}"/></span>
                            </c:if>
                            <span class="field-hint">Chỉ các sản phẩm thuộc nhà cung cấp này mới được thêm vào bên dưới.</span>
                        </div>

                        <div class="form-field">
                            <label for="orderDate">Ngày đặt <span class="req">*</span></label>
                            <input type="date" id="orderDate" name="orderDate" required
                                   value="<c:out value='${order.orderDate}'/>">
                            <c:if test="${not empty errors.orderDate}">
                                <span class="field-error"><c:out value="${errors.orderDate}"/></span>
                            </c:if>
                        </div>

                        <div class="form-field">
                            <label for="expectedDeliveryDate">Ngày giao dự kiến</label>
                            <input type="date" id="expectedDeliveryDate" name="expectedDeliveryDate"
                                   value="<c:out value='${order.expectedDeliveryDate}'/>">
                            <c:if test="${not empty errors.expectedDeliveryDate}">
                                <span class="field-error"><c:out value="${errors.expectedDeliveryDate}"/></span>
                            </c:if>
                        </div>
                    </div>

                    <div class="form-field">
                        <label for="note">Ghi chú</label>
                        <textarea id="note" name="note" rows="2" maxlength="500"
                                  placeholder="Ghi chú tùy chọn cho đơn đặt hàng này"><c:out value="${order.note}"/></textarea>
                    </div>
                </fieldset>

                <fieldset class="profile-group">
                    <legend>Sản phẩm đặt hàng</legend>

                    <c:if test="${not empty errors.items}">
                        <span class="field-error"><c:out value="${errors.items}"/></span>
                    </c:if>

                    <table class="admin-table po-items-table">
                        <thead>
                        <tr>
                            <th style="width:34%">Sản phẩm</th>
                            <th style="width:14%">SKU</th>
                            <th style="width:16%">Mã NCC</th>
                            <th style="width:12%">Số lượng</th>
                            <th style="width:12%">Đơn giá</th>
                            <th style="width:8%"></th>
                        </tr>
                        </thead>
                        <tbody id="itemsBody">
                        <%-- Existing rows (edit mode or after a failed submit) --%>
                        <c:forEach var="item" items="${items}">
                            <tr class="po-item-row">
                                <td>
                                    <select name="productId" class="po-product" required>
                                        <option value="">— Sản phẩm —</option>
                                        <c:forEach var="sp" items="${supplierProducts}">
                                            <option value="${sp.productId}"
                                                    data-sku="<c:out value='${sp.sku}'/>"
                                                    data-code="<c:out value='${sp.supplierProductCode}'/>"
                                                    data-cost="<c:out value='${sp.unitCost}'/>"
                                                    ${item.productId == sp.productId ? 'selected' : ''}>
                                                <c:out value="${sp.productName}"/>
                                            </option>
                                        </c:forEach>
                                        <c:if test="${not empty item.productName}">
                                            <option value="${item.productId}" selected
                                                    data-sku="<c:out value='${item.sku}'/>" data-code="" data-cost="">
                                                <c:out value="${item.productName}"/>
                                            </option>
                                        </c:if>
                                    </select>
                                </td>
                                <td class="po-sku"><c:out value="${item.sku}"/></td>
                                <td class="po-code"><c:out value="${item.supplierProductCode}"/></td>
                                <td><input type="number" name="quantity" class="po-qty" min="1" step="1"
                                           value="<c:out value='${item.orderedQuantity}'/>" required></td>
                                <td><input type="number" name="unitCost" class="po-cost" min="0" step="1"
                                           value="<c:out value='${item.unitCost}'/>" required></td>
                                <td><button type="button" class="btn btn-ghost btn-sm btn-danger po-remove">Xóa</button></td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>

                    <div>
                        <button type="button" class="btn btn-secondary btn-sm" id="addItemBtn"
                                ${empty supplierProducts ? 'disabled' : ''}>+ Thêm thuốc</button>
                        <span class="field-hint" id="noProductsHint"
                              ${empty supplierProducts ? '' : 'hidden'}>Chọn nhà cung cấp trước — sản phẩm sẽ được tải theo nhà cung cấp đó.</span>
                    </div>
                </fieldset>

                <div class="profile-actions">
                    <a class="btn btn-ghost" href="${ctx}/admin/purchase-orders">Hủy</a>
                    <button type="submit" name="submitAction" value="draft" class="btn btn-secondary">Lưu nháp</button>
                    <button type="submit" name="submitAction" value="place" class="btn btn-primary">Đặt hàng</button>
                </div>
            </form>
        </div>
    </main>
</div>

<%@ include file="/WEB-INF/jspf/admin-footer.jspf" %>

<%--
    Product options per supplier. Rendered once as hidden JSON-ish <option>
    templates per supplier? Simpler: a full page reload on supplier change
    keeps everything server-side (no JSON API in this project). The servlet's
    "new"/"edit" GET accepts ?supplier= to pre-pick and fill the picker.
--%>
<script>
(function () {
    var supplierSelect = document.getElementById('supplierId');
    var form = document.getElementById('poForm');
    var itemsBody = document.getElementById('itemsBody');
    var addBtn = document.getElementById('addItemBtn');

    // Changing supplier reloads the form (GET) so the product list is rebuilt
    // server-side; item rows would otherwise keep products of the old supplier.
    supplierSelect.addEventListener('change', function () {
        var sid = supplierSelect.value;
        var hasRows = itemsBody.querySelectorAll('.po-item-row').length > 0;
        if (hasRows && !confirm('Đổi nhà cung cấp sẽ xóa các dòng sản phẩm hiện tại. Tiếp tục?')) {
            // restore previous selection
            supplierSelect.value = supplierSelect.getAttribute('data-prev') || '';
            return;
        }
        var url = form.getAttribute('data-reload-url');
        if (sid) {
            url += '&supplier=' + sid;
        }
        // keep entered dates/note across the reload
        url += '&orderDate=' + encodeURIComponent(document.getElementById('orderDate').value);
        url += '&expectedDeliveryDate=' + encodeURIComponent(document.getElementById('expectedDeliveryDate').value);
        url += '&note=' + encodeURIComponent(document.getElementById('note').value);
        window.location.href = url;
    });
    supplierSelect.setAttribute('data-prev', supplierSelect.value);

    function fillRow(row, opt) {
        row.querySelector('.po-sku').textContent = opt.getAttribute('data-sku') || '';
        row.querySelector('.po-code').textContent = opt.getAttribute('data-code') || '';
        var cost = opt.getAttribute('data-cost');
        var costInput = row.querySelector('.po-cost');
        if (cost && costInput) {
            costInput.value = cost;
        }
    }

    // add a new blank item row cloning the hidden template select
    addBtn.addEventListener('click', function () {
        var tpl = document.getElementById('poRowTemplate');
        var clone = tpl.content.cloneNode(true);
        itemsBody.appendChild(clone);
    });

    // delegated: product picked → fill sku/code/cost; remove → drop row
    itemsBody.addEventListener('change', function (e) {
        if (!e.target.classList.contains('po-product')) return;
        var opt = e.target.options[e.target.selectedIndex];
        fillRow(e.target.closest('tr'), opt);
    });
    itemsBody.addEventListener('click', function (e) {
        if (!e.target.classList.contains('po-remove')) return;
        e.target.closest('tr').remove();
    });

    // Prevent duplicate submits — disable BOTH submit buttons after one fires
    form.addEventListener('submit', function () {
        var buttons = form.querySelectorAll('button[type="submit"]');
        for (var i = 0; i < buttons.length; i++) {
            buttons[i].disabled = true;
        }
    });
})();
</script>

<%-- Row template cloned by "Add Medicine" — keeps markup in one place --%>
<template id="poRowTemplate">
    <tr class="po-item-row">
        <td>
            <select name="productId" class="po-product" required>
                <option value="">— Sản phẩm —</option>
                <c:forEach var="sp" items="${supplierProducts}">
                    <option value="${sp.productId}"
                            data-sku="<c:out value='${sp.sku}'/>"
                            data-code="<c:out value='${sp.supplierProductCode}'/>"
                            data-cost="<c:out value='${sp.unitCost}'/>">
                        <c:out value="${sp.productName}"/>
                    </option>
                </c:forEach>
            </select>
        </td>
        <td class="po-sku"></td>
        <td class="po-code"></td>
        <td><input type="number" name="quantity" class="po-qty" min="1" step="1" required></td>
        <td><input type="number" name="unitCost" class="po-cost" min="0" step="1" required></td>
        <td><button type="button" class="btn btn-ghost btn-sm btn-danger po-remove">Xóa</button></td>
    </tr>
</template>

</body>
</html>
