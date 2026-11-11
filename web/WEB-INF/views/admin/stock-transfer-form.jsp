<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="stock-transfer"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Chuyển vị trí — PharmaFlow</title>
</head>
<body class="admin-layout">
<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>
<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>
    <main class="admin-main">
        <h2>Chuyển vị trí trong kho</h2>
        <c:if test="${param.ok == 'transferred'}">
            <div class="alert alert-success">Đã chuyển tồn giữa các ngăn.</div>
        </c:if>
        <c:if test="${not empty param.err}">
            <div class="alert alert-error">Không thể chuyển: <c:out value="${param.err}"/></div>
        </c:if>
        <div class="profile-card">
            <form method="post" action="${ctx}/inventory/transfers" class="profile-form">
                <div class="form-field">
                    <label for="batchId">Mã lô (batch_id)</label>
                    <input type="number" name="batchId" id="batchId" required min="1">
                </div>
                <div class="form-field">
                    <label for="fromZone">Từ — Khu</label>
                    <select id="fromZone" class="gr-zone">
                        <option value="">— Khu —</option>
                        <c:forEach var="z" items="${zones}">
                            <option value="${z.zoneId}"><c:out value="${z.zoneCode}"/></option>
                        </c:forEach>
                    </select>
                </div>
                <div class="form-field">
                    <label for="fromRack">Từ — Kệ</label>
                    <select id="fromRack" disabled><option value="">— Kệ —</option></select>
                </div>
                <div class="form-field">
                    <label for="fromBinId">Từ — Ngăn</label>
                    <select name="fromBinId" id="fromBinId" required disabled><option value="">— Ngăn —</option></select>
                </div>
                <div class="form-field">
                    <label for="toZone">Đến — Khu</label>
                    <select id="toZone" class="gr-zone">
                        <option value="">— Khu —</option>
                        <c:forEach var="z" items="${zones}">
                            <option value="${z.zoneId}"><c:out value="${z.zoneCode}"/></option>
                        </c:forEach>
                    </select>
                </div>
                <div class="form-field">
                    <label for="toRack">Đến — Kệ</label>
                    <select id="toRack" disabled><option value="">— Kệ —</option></select>
                </div>
                <div class="form-field">
                    <label for="toBinId">Đến — Ngăn</label>
                    <select name="toBinId" id="toBinId" required disabled><option value="">— Ngăn —</option></select>
                </div>
                <div class="form-field">
                    <label for="quantity">Số lượng</label>
                    <input type="number" name="quantity" id="quantity" min="1" required>
                </div>
                <div class="form-field">
                    <label for="reason">Lý do</label>
                    <input type="text" name="reason" id="reason" maxlength="300">
                </div>
                <button type="submit" class="btn btn-primary">Chuyển</button>
            </form>
        </div>
    </main>
</div>
<script>
(function () {
    var ctx = '${ctx}';
    function wire(zoneId, rackId, binId) {
        var z = document.getElementById(zoneId);
        var r = document.getElementById(rackId);
        var b = document.getElementById(binId);
        z.addEventListener('change', function () {
            r.innerHTML = '<option value="">— Kệ —</option>';
            b.innerHTML = '<option value="">— Ngăn —</option>';
            r.disabled = true; b.disabled = true;
            if (!z.value) return;
            fetch(ctx + '/inventory/locations?action=api-racks&zoneId=' + z.value)
                .then(function (res) { return res.json(); })
                .then(function (data) {
                    data.forEach(function (x) {
                        var o = document.createElement('option');
                        o.value = x.rackId; o.textContent = x.rackCode;
                        r.appendChild(o);
                    });
                    r.disabled = false;
                });
        });
        r.addEventListener('change', function () {
            b.innerHTML = '<option value="">— Ngăn —</option>';
            b.disabled = true;
            if (!r.value) return;
            fetch(ctx + '/inventory/locations?action=api-bins&rackId=' + r.value)
                .then(function (res) { return res.json(); })
                .then(function (data) {
                    data.forEach(function (x) {
                        var o = document.createElement('option');
                        o.value = x.binId; o.textContent = x.binCode;
                        b.appendChild(o);
                    });
                    b.disabled = false;
                });
        });
    }
    wire('fromZone', 'fromRack', 'fromBinId');
    wire('toZone', 'toRack', 'toBinId');
})();
</script>
</body>
</html>
