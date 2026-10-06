<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="inventory-alerts"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Inventory Alert Settings — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <nav class="crumbs" aria-label="Breadcrumb">
            <a href="${ctx}/inventory">Inventory</a>
            <span aria-hidden="true">›</span>
            <a href="${ctx}/inventory/alerts">Alerts</a>
            <span aria-hidden="true">›</span>
            <span>Settings</span>
        </nav>

        <div class="section-head">
            <div>
                <h2>Inventory Alert Settings</h2>
                <p class="section-sub">Global thresholds applied to all products and batches.</p>
            </div>
        </div>

        <c:if test="${param.ok == 'saved'}">
            <div class="alert alert-success" role="status">Alert settings saved.</div>
        </c:if>
        <c:if test="${param.err == 'invalid'}">
            <div class="alert alert-error" role="alert">Both values must be whole numbers of zero or more.</div>
        </c:if>
        <c:if test="${param.err == 'db'}">
            <div class="alert alert-error" role="alert">Database error — the settings were not saved.</div>
        </c:if>

        <div class="profile-card">
            <form class="profile-form" method="post" data-disable-on-submit
                  action="${ctx}/inventory/alerts?action=save-settings">

                <fieldset class="profile-group">
                    <legend>Global Settings</legend>
                    <p class="field-hint">
                        These thresholds apply to every product and batch — no per-product
                        overrides. Changes take effect on the next alerts reload.
                    </p>
                    <div class="profile-grid">
                        <div class="form-field">
                            <label for="minimumStockLevel">Minimum Stock Level <span class="req">*</span></label>
                            <input type="number" id="minimumStockLevel" name="minimumStockLevel"
                                   required min="0" step="1"
                                   value="<c:out value='${settings.minimumStockLevel}'/>">
                            <span class="field-hint">Products at or below this saleable quantity raise LOW_STOCK.</span>
                        </div>
                        <div class="form-field">
                            <label for="nearExpiryWarningDays">Near Expiry Warning <span class="req">*</span></label>
                            <input type="number" id="nearExpiryWarningDays" name="nearExpiryWarningDays"
                                   required min="0" step="1"
                                   value="<c:out value='${settings.nearExpiryWarningDays}'/>">
                            <span class="field-hint">Batches expiring within this many days raise NEAR_EXPIRY.</span>
                        </div>
                    </div>
                </fieldset>

                <div class="profile-actions">
                    <button type="submit" class="btn btn-primary">Save Settings</button>
                    <a class="btn btn-ghost" href="${ctx}/inventory/alerts">Back to Alerts</a>
                </div>
            </form>
        </div>
    </main>
</div>

<%@ include file="/WEB-INF/jspf/admin-footer.jspf" %>

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
