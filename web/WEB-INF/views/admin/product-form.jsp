<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="isEdit" value="${mode == 'edit'}"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${isEdit ? 'Edit' : 'New'} Product — Admin — PharmaFlow</title>
    <link rel="stylesheet" href="${ctx}/css/main.css">
</head>
<body>

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<section class="section">
    <div class="container profile-wrap">
        <div class="section-head">
            <div>
                <h2>${isEdit ? 'Edit Product' : 'New Product'}</h2>
                <p class="section-sub">
                    ${isEdit ? 'Update catalog info, pricing and visibility.' : 'Add a product to the catalog. Stock is managed via purchase orders / batches.'}
                </p>
            </div>
        </div>

        <div class="profile-card">
            <form class="profile-form" method="post" data-disable-on-submit
                  action="${ctx}/admin?action=${isEdit ? 'product-update' : 'product-create'}">

                <c:if test="${isEdit}">
                    <input type="hidden" name="productId" value="${product.productId}">
                </c:if>

                <fieldset class="profile-group">
                    <legend>Identity</legend>

                    <div class="form-field">
                        <label for="productName">Product Name <span class="req">*</span></label>
                        <input type="text" id="productName" name="productName" required maxlength="200"
                               value="<c:out value='${product.productName}'/>">
                        <c:if test="${not empty errors.productName}">
                            <span class="field-error"><c:out value="${errors.productName}"/></span>
                        </c:if>
                    </div>

                    <div class="profile-grid">
                        <div class="form-field">
                            <label for="sku">SKU <span class="req">*</span></label>
                            <input type="text" id="sku" name="sku" required maxlength="100"
                                   value="<c:out value='${product.sku}'/>">
                            <c:if test="${not empty errors.sku}">
                                <span class="field-error"><c:out value="${errors.sku}"/></span>
                            </c:if>
                        </div>

                        <div class="form-field">
                            <label for="barcode">Barcode</label>
                            <input type="text" id="barcode" name="barcode" maxlength="100"
                                   value="<c:out value='${product.barcode}'/>">
                            <c:if test="${not empty errors.barcode}">
                                <span class="field-error"><c:out value="${errors.barcode}"/></span>
                            </c:if>
                        </div>

                        <div class="form-field">
                            <label for="categoryId">Category <span class="req">*</span></label>
                            <select id="categoryId" name="categoryId" required>
                                <option value="">— Choose —</option>
                                <c:forEach var="c" items="${categories}">
                                    <option value="${c.categoryId}"
                                            ${product.categoryId == c.categoryId ? 'selected' : ''}>
                                        <c:out value="${c.categoryName}"/>
                                    </option>
                                </c:forEach>
                            </select>
                            <c:if test="${not empty errors.categoryId}">
                                <span class="field-error"><c:out value="${errors.categoryId}"/></span>
                            </c:if>
                        </div>
                    </div>
                </fieldset>

                <fieldset class="profile-group">
                    <legend>Medical Info</legend>

                    <div class="profile-grid">
                        <div class="form-field">
                            <label for="activeIngredient">Active Ingredient</label>
                            <input type="text" id="activeIngredient" name="activeIngredient" maxlength="255"
                                   value="<c:out value='${product.activeIngredient}'/>">
                        </div>
                        <div class="form-field">
                            <label for="strength">Strength</label>
                            <input type="text" id="strength" name="strength" maxlength="100"
                                   value="<c:out value='${product.strength}'/>" placeholder="500mg, 10%…">
                        </div>
                        <div class="form-field">
                            <label for="dosageForm">Dosage Form</label>
                            <input type="text" id="dosageForm" name="dosageForm" maxlength="100"
                                   value="<c:out value='${product.dosageForm}'/>" placeholder="Tablet, syrup…">
                        </div>
                    </div>

                    <div class="profile-grid">
                        <div class="form-field">
                            <label for="manufacturer">Manufacturer</label>
                            <input type="text" id="manufacturer" name="manufacturer" maxlength="200"
                                   value="<c:out value='${product.manufacturer}'/>">
                        </div>
                        <div class="form-field">
                            <label for="registrationNumber">Registration No.</label>
                            <input type="text" id="registrationNumber" name="registrationNumber" maxlength="100"
                                   value="<c:out value='${product.registrationNumber}'/>">
                        </div>
                        <div class="form-field">
                            <label for="productType">Product Type <span class="req">*</span></label>
                            <select id="productType" name="productType" required>
                                <option value="OTC"        ${product.productType.name() == 'OTC'        ? 'selected' : ''}>OTC — over the counter</option>
                                <option value="RX"         ${product.productType.name() == 'RX'         ? 'selected' : ''}>RX — prescription only</option>
                                <option value="RESTRICTED" ${product.productType.name() == 'RESTRICTED' ? 'selected' : ''}>RESTRICTED</option>
                            </select>
                        </div>
                    </div>
                </fieldset>

                <fieldset class="profile-group">
                    <legend>Sale</legend>

                    <div class="profile-grid">
                        <div class="form-field">
                            <label for="sellingPrice">Selling Price <span class="req">*</span></label>
                            <input type="number" id="sellingPrice" name="sellingPrice" required
                                   min="0" step="1"
                                   value="<c:out value='${product.sellingPrice}'/>">
                            <c:if test="${not empty errors.sellingPrice}">
                                <span class="field-error"><c:out value="${errors.sellingPrice}"/></span>
                            </c:if>
                        </div>
                        <div class="form-field">
                            <label for="sellingUnit">Selling Unit <span class="req">*</span></label>
                            <input type="text" id="sellingUnit" name="sellingUnit" required maxlength="100"
                                   value="<c:out value='${product.sellingUnit}'/>" placeholder="box, bottle, strip…">
                            <c:if test="${not empty errors.sellingUnit}">
                                <span class="field-error"><c:out value="${errors.sellingUnit}"/></span>
                            </c:if>
                        </div>
                        <div class="form-field">
                            <label class="check-label" for="onlineSaleAllowed">
                                <input type="checkbox" id="onlineSaleAllowed" name="onlineSaleAllowed" value="1"
                                       ${product.onlineSaleAllowed ? 'checked' : ''}>
                                <span>Allow online sale</span>
                            </label>
                            <span class="field-hint">Only OTC + online-sale + in-stock items appear purchasable on storefront.</span>
                        </div>
                    </div>
                </fieldset>

                <c:if test="${isEdit}">
                    <div class="form-field">
                        <label for="status">Status</label>
                        <select id="status" name="status">
                            <option value="ACTIVE"   ${product.status == 'ACTIVE'   ? 'selected' : ''}>ACTIVE — visible on storefront</option>
                            <option value="INACTIVE" ${product.status == 'INACTIVE' ? 'selected' : ''}>INACTIVE — hidden</option>
                        </select>
                    </div>
                    <c:if test="${not empty product.availableQuantity}">
                        <p class="field-hint">Current sellable stock: <c:out value="${product.availableQuantity}"/> (managed via batches, not editable here).</p>
                    </c:if>
                </c:if>

                <div class="profile-actions">
                    <button type="submit" class="btn btn-primary">${isEdit ? 'Save Changes' : 'Create Product'}</button>
                    <a class="btn btn-ghost" href="${ctx}/admin?action=products">Cancel</a>
                </div>
            </form>
        </div>
    </div>
</section>

<script>
    // Prevent duplicate submits (same pattern as other admin forms)
    (function () {
        var form = document.querySelector('form[data-disable-on-submit]');
        if (!form) return;
        form.addEventListener('submit', function () {
            var btn = form.querySelector('button[type="submit"]');
            if (btn) { btn.disabled = true; btn.textContent = 'Saving…'; }
        });
    })();
</script>
</body>
</html>
