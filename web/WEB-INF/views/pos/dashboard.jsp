<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="pos"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Point of Sale — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <nav class="crumbs" aria-label="Breadcrumb">
            <span>Sales</span>
            <span aria-hidden="true">›</span>
            <span>Point of Sale</span>
        </nav>

        <div class="section-head">
            <div>
                <h2>Point of Sale</h2>
                <p class="section-sub">Counter sale — search a product, build the cart, take payment.</p>
            </div>
            <a class="btn btn-ghost btn-sm" href="${ctx}/pos?action=history">Sale History</a>
        </div>

        <c:if test="${not empty param.err}">
            <div class="alert alert-error">
                <c:choose>
                    <c:when test="${param.err == 'EMPTY_CART'}">The cart is empty — add a product first.</c:when>
                    <c:when test="${param.err == 'STAFF_PROFILE_MISSING'}">Your account has no staff profile — ask an administrator to link one before selling.</c:when>
                    <c:when test="${param.err == 'INVALID_PAYMENT_METHOD'}">Choose a valid payment method.</c:when>
                    <c:when test="${param.err == 'PRODUCT_NOT_FOUND'}">Product not found.</c:when>
                    <c:when test="${param.err == 'PRODUCT_INACTIVE'}">That product is no longer active.</c:when>
                    <c:when test="${param.err == 'INSUFFICIENT_STOCK'}">Not enough saleable stock.</c:when>
                    <c:when test="${param.err == 'PRESCRIPTION_REQUIRED'}">This sale contains Rx products — verify a prescription first.</c:when>
                    <c:when test="${param.err == 'PRESCRIPTION_NOT_FOUND'}">No prescription with that code exists.</c:when>
                    <c:when test="${param.err == 'PRESCRIPTION_INVALID'}">That prescription exists but is not VALID.</c:when>
                    <c:when test="${param.err == 'RX_PRODUCT_NOT_IN_PRESCRIPTION'}">A cart item is not covered by the verified prescription.</c:when>
                    <c:when test="${param.err == 'RX_QUANTITY_EXCEEDED'}">Requested quantity exceeds the prescribed quantity.</c:when>
                    <c:when test="${param.err == 'RESTRICTED_NOT_ALLOWED'}">Restricted products cannot be sold at the counter.</c:when>
                    <c:when test="${param.err == 'INVALID_TOKEN'}">This checkout was already submitted — the cart is unchanged.</c:when>
                    <c:otherwise>Something went wrong — please try again.</c:otherwise>
                </c:choose>
                <c:if test="${not empty param.msg}"> <c:out value="${param.msg}"/></c:if>
            </div>
        </c:if>
        <c:if test="${not empty param.ok}">
            <div class="alert alert-success">
                <c:choose>
                    <c:when test="${param.ok == 'added'}">Added to cart.</c:when>
                    <c:when test="${param.ok == 'updated'}">Quantity updated.</c:when>
                    <c:when test="${param.ok == 'removed'}">Removed from cart.</c:when>
                    <c:when test="${param.ok == 'cleared'}">Cart cleared.</c:when>
                    <c:when test="${param.ok == 'rxverified'}">Prescription verified and attached.</c:when>
                    <c:when test="${param.ok == 'rxremoved'}">Prescription detached.</c:when>
                    <c:otherwise>Done.</c:otherwise>
                </c:choose>
            </div>
        </c:if>

        <%-- ==================== product search ==================== --%>
        <div class="admin-card">
            <form class="filter-bar" method="get" action="${ctx}/pos">
                <input type="search" name="q" value="<c:out value='${q}'/>"
                       placeholder="Product name, SKU or barcode…" aria-label="Search product"
                       autofocus>
                <button type="submit" class="btn btn-secondary btn-sm">Search</button>
                <c:if test="${not empty q}">
                    <a href="${ctx}/pos" class="btn btn-ghost btn-sm">Clear</a>
                </c:if>
            </form>

            <c:if test="${not empty q}">
                <c:choose>
                    <c:when test="${empty results}">
                        <div class="empty-state"><p>No active product matches “<c:out value='${q}'/>”.</p></div>
                    </c:when>
                    <c:otherwise>
                        <table class="admin-table">
                            <thead>
                            <tr>
                                <th>Product</th>
                                <th>SKU</th>
                                <th>Type</th>
                                <th class="col-num">Price</th>
                                <th class="col-num">Saleable</th>
                                <th></th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:forEach var="p" items="${results}">
                                <tr>
                                    <td><c:out value="${p.productName}"/></td>
                                    <td><c:out value="${p.sku}"/></td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${p.productType == 'RX'}">
                                                <span class="type-badge type-rx">Rx</span>
                                            </c:when>
                                            <c:when test="${p.productType == 'RESTRICTED'}">
                                                <span class="type-badge type-restricted">Restricted</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="type-badge type-otc">OTC</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td class="col-num"><fmt:formatNumber value="${p.sellingPrice}" type="number" groupingUsed="true"/></td>
                                    <td class="col-num"><c:out value="${p.availableQuantity}"/></td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${p.productType == 'RESTRICTED'}">
                                                <span class="field-hint">Not sellable</span>
                                            </c:when>
                                            <c:when test="${p.availableQuantity <= 0}">
                                                <span class="field-hint">Out of stock</span>
                                            </c:when>
                                            <c:otherwise>
                                                <form method="post" action="${ctx}/pos?action=add" class="pos-inline-form">
                                                    <input type="hidden" name="productId" value="${p.productId}">
                                                    <input type="hidden" name="q" value="<c:out value='${q}'/>">
                                                    <input type="number" name="qty" value="1" min="1"
                                                           max="${p.availableQuantity}" class="pos-qty-input"
                                                           aria-label="Quantity">
                                                    <button type="submit" class="btn btn-primary btn-sm">Add</button>
                                                </form>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                            </c:forEach>
                            </tbody>
                        </table>
                    </c:otherwise>
                </c:choose>
            </c:if>
        </div>

        <%-- ==================== cart ==================== --%>
        <div class="admin-card">
            <h3 class="pos-card-title">Cart</h3>
            <c:choose>
                <c:when test="${empty cartLines}">
                    <div class="empty-state"><p>Cart is empty — search a product above to start a sale.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="admin-table">
                        <thead>
                        <tr>
                            <th>Product</th>
                            <th>Type</th>
                            <th class="col-num">Unit Price</th>
                            <th class="col-num">Qty</th>
                            <th class="col-num">Subtotal</th>
                            <th></th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="line" items="${cartLines}">
                            <tr>
                                <td>
                                    <c:out value="${line.productName}"/>
                                    <div class="field-hint"><c:out value="${line.sku}"/> · <c:out value="${line.sellingUnit}"/></div>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${line.productType == 'RX'}">
                                            <span class="type-badge type-rx">Rx</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="type-badge type-otc">OTC</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="col-num"><fmt:formatNumber value="${line.unitPrice}" type="number" groupingUsed="true"/></td>
                                <td class="col-num">
                                    <form method="post" action="${ctx}/pos?action=update-qty" class="pos-inline-form">
                                        <input type="hidden" name="productId" value="${line.productId}">
                                        <input type="number" name="qty" value="${line.quantity}" min="0"
                                               max="${line.saleableQuantity}" class="pos-qty-input"
                                               aria-label="Quantity for ${line.productName}">
                                        <button type="submit" class="btn btn-ghost btn-sm">Set</button>
                                    </form>
                                    <c:if test="${line.saleableQuantity != null and line.quantity > line.saleableQuantity}">
                                        <div class="field-hint">Only <c:out value="${line.saleableQuantity}"/> saleable</div>
                                    </c:if>
                                </td>
                                <td class="col-num"><fmt:formatNumber value="${line.subtotal}" type="number" groupingUsed="true"/></td>
                                <td>
                                    <form method="post" action="${ctx}/pos?action=remove" class="pos-inline-form">
                                        <input type="hidden" name="productId" value="${line.productId}">
                                        <button type="submit" class="btn btn-ghost btn-sm">Remove</button>
                                    </form>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </c:otherwise>
            </c:choose>
        </div>

        <%-- ==================== prescription (only when the cart has Rx) ==================== --%>
        <c:if test="${cartHasRx}">
            <div class="admin-card">
                <h3 class="pos-card-title">Prescription</h3>
                <c:choose>
                    <c:when test="${prescription == null}">
                        <p class="field-hint">This cart contains Rx products — a VALID prescription code is required before checkout.</p>
                        <form class="filter-bar" method="post" action="${ctx}/pos?action=verify-prescription">
                            <input type="text" name="prescriptionCode"
                                   placeholder="Prescription code…" aria-label="Prescription code">
                            <button type="submit" class="btn btn-secondary btn-sm">Verify</button>
                        </form>
                    </c:when>
                    <c:otherwise>
                        <div class="profile-grid">
                            <div class="profile-group">
                                <span class="field-hint">Code</span>
                                <strong><c:out value="${prescription.prescriptionCode}"/></strong>
                            </div>
                            <div class="profile-group">
                                <span class="field-hint">Patient</span>
                                <strong><c:out value="${prescription.patientName}"/></strong>
                            </div>
                            <div class="profile-group">
                                <span class="field-hint">Prescriber</span>
                                <strong><c:out value="${prescription.prescriber}"/></strong>
                            </div>
                            <div class="profile-group">
                                <span class="field-hint">Facility</span>
                                <strong><c:out value="${prescription.healthcareFacility}"/></strong>
                            </div>
                            <div class="profile-group">
                                <span class="field-hint">Date</span>
                                <strong><fmt:formatDate value="${prescription.prescriptionDate}" pattern="yyyy-MM-dd"/></strong>
                            </div>
                        </div>
                        <c:if test="${not empty prescriptionItems}">
                            <table class="admin-table">
                                <thead>
                                <tr>
                                    <th>Drug</th>
                                    <th>Strength</th>
                                    <th class="col-num">Prescribed Qty</th>
                                    <th>Usage</th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach var="rxItem" items="${prescriptionItems}">
                                    <tr>
                                        <td><c:out value="${rxItem.drugName}"/></td>
                                        <td><c:out value="${rxItem.strength}"/></td>
                                        <td class="col-num"><c:out value="${rxItem.prescribedQuantity}"/></td>
                                        <td><c:out value="${rxItem.usageInstruction}"/></td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </c:if>
                        <form method="post" action="${ctx}/pos?action=remove-prescription">
                            <button type="submit" class="btn btn-ghost btn-sm">Detach prescription</button>
                        </form>
                    </c:otherwise>
                </c:choose>
            </div>
        </c:if>

        <%-- ==================== checkout ==================== --%>
        <%-- clear uses a separate form referenced by id — HTML forbids nested forms --%>
        <form id="pos-clear-form" method="post" action="${ctx}/pos?action=clear"></form>
        <div class="admin-card">
            <h3 class="pos-card-title">Payment</h3>
            <form method="post" action="${ctx}/pos?action=checkout" data-disable-on-submit>
                <input type="hidden" name="checkoutToken" value="<c:out value='${checkoutToken}'/>">
                <div class="pos-payment-row">
                    <c:forEach var="m" items="${paymentMethods}">
                        <label class="pos-payment-option">
                            <input type="radio" name="paymentMethod" value="${m}"
                                   ${m == 'CASH' ? 'checked' : ''}>
                            <c:choose>
                                <c:when test="${m == 'CASH'}">Cash</c:when>
                                <c:when test="${m == 'BANK_TRANSFER'}">Bank Transfer</c:when>
                                <c:otherwise>Card</c:otherwise>
                            </c:choose>
                        </label>
                    </c:forEach>
                </div>
                <div class="pos-total-row">
                    <span>Total</span>
                    <strong class="pos-total"><fmt:formatNumber value="${cartTotal}" type="number" groupingUsed="true"/> ₫</strong>
                </div>
                <div class="profile-actions">
                    <button type="submit" class="btn btn-primary"
                            ${empty cartLines or (cartHasRx and prescription == null) ? 'disabled' : ''}>
                        Complete Sale
                    </button>
                    <button type="submit" form="pos-clear-form" class="btn btn-ghost">Clear Cart</button>
                </div>
                <c:if test="${cartHasRx and prescription == null}">
                    <p class="field-hint">Verify a prescription above to enable checkout.</p>
                </c:if>
            </form>
        </div>
    </main>
</div>

<script>
    // Prevent duplicate submits — disable the submit button while processing.
    // (The session checkout token is the real guard; this is just the UI half.)
    var form = document.querySelector('form[data-disable-on-submit]');
    if (form) {
        form.addEventListener('submit', function () {
            var btn = form.querySelector('button[type="submit"]');
            if (btn) { btn.disabled = true; btn.textContent = 'Processing…'; }
        });
    }
</script>

<%@ include file="/WEB-INF/jspf/admin-footer.jspf" %>

</body>
</html>
