<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="isEdit" value="${mode == 'edit'}"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${isEdit ? 'Edit' : 'New'} Supplier — Admin — PharmaFlow</title>
    <link rel="stylesheet" href="${ctx}/css/main.css">
</head>
<body>

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<section class="section">
    <div class="container profile-wrap">
        <div class="section-head">
            <div>
                <h2>${isEdit ? 'Edit Supplier' : 'New Supplier'}</h2>
                <p class="section-sub">
                    ${isEdit ? 'Update contact info and availability.' : 'Register a new vendor for purchase orders.'}
                </p>
            </div>
        </div>

        <div class="profile-card">
            <form class="profile-form" method="post" data-disable-on-submit
                  action="${ctx}/admin?action=${isEdit ? 'supplier-update' : 'supplier-create'}">

                <c:if test="${isEdit}">
                    <input type="hidden" name="supplierId" value="${supplier.supplierId}">
                </c:if>

                <fieldset class="profile-group">
                    <legend>Company</legend>

                    <div class="form-field">
                        <label for="supplierName">Supplier Name <span class="req">*</span></label>
                        <input type="text" id="supplierName" name="supplierName" required maxlength="200"
                               value="<c:out value='${supplier.supplierName}'/>">
                        <c:if test="${not empty errors.supplierName}">
                            <span class="field-error"><c:out value="${errors.supplierName}"/></span>
                        </c:if>
                    </div>

                    <div class="form-field">
                        <label for="taxBusinessInfo">Tax / Business Info</label>
                        <input type="text" id="taxBusinessInfo" name="taxBusinessInfo" maxlength="255"
                               value="<c:out value='${supplier.taxBusinessInfo}'/>"
                               placeholder="Tax code, business license…">
                    </div>

                    <div class="form-field">
                        <label for="address">Address</label>
                        <input type="text" id="address" name="address" maxlength="255"
                               value="<c:out value='${supplier.address}'/>">
                    </div>
                </fieldset>

                <fieldset class="profile-group">
                    <legend>Contact</legend>

                    <div class="profile-grid">
                        <div class="form-field">
                            <label for="contactPerson">Contact Person</label>
                            <input type="text" id="contactPerson" name="contactPerson" maxlength="150"
                                   value="<c:out value='${supplier.contactPerson}'/>">
                        </div>

                        <div class="form-field">
                            <label for="phone">Phone</label>
                            <input type="tel" id="phone" name="phone" maxlength="30"
                                   value="<c:out value='${supplier.phone}'/>">
                        </div>

                        <div class="form-field">
                            <label for="email">Email</label>
                            <input type="email" id="email" name="email" maxlength="150"
                                   value="<c:out value='${supplier.email}'/>">
                            <c:if test="${not empty errors.email}">
                                <span class="field-error"><c:out value="${errors.email}"/></span>
                            </c:if>
                        </div>
                    </div>
                </fieldset>

                <c:if test="${isEdit}">
                    <div class="form-field">
                        <label for="status">Status</label>
                        <select id="status" name="status">
                            <option value="ACTIVE"   ${supplier.status == 'ACTIVE'   ? 'selected' : ''}>ACTIVE — available for purchase orders</option>
                            <option value="INACTIVE" ${supplier.status == 'INACTIVE' ? 'selected' : ''}>INACTIVE — preserved for history</option>
                        </select>
                    </div>
                </c:if>

                <div class="profile-actions">
                    <button type="submit" class="btn btn-primary">${isEdit ? 'Save Changes' : 'Create Supplier'}</button>
                    <a class="btn btn-ghost" href="${ctx}/admin?action=suppliers">Cancel</a>
                </div>
            </form>
        </div>
    </div>
</section>

<script>
    // Prevent duplicate submits (same pattern as other forms)
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
