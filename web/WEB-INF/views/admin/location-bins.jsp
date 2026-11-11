<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="locations"/>
<c:set var="isForm" value="${not empty formMode}"/>
<c:set var="isEdit" value="${formMode == 'edit'}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Vị trí kho — Ô — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <div class="section-head">
            <div>
                <h2>Vị trí kho — Ô (bin)</h2>
                <p class="section-sub">
                    <c:if test="${not empty filterRack}">
                        <c:out value="${filterRack.zoneCode}"/> / <c:out value="${filterRack.rackCode}"/> — <c:out value="${filterRack.rackName}"/>
                    </c:if>
                    <c:if test="${empty filterRack && not empty filterZone}">
                        Khu vực <c:out value="${filterZone.zoneCode}"/>
                    </c:if>
                    <c:if test="${empty filterRack && empty filterZone}">Danh sách ô lưu trữ.</c:if>
                </p>
            </div>
            <c:if test="${!isForm}">
                <c:url var="newBinUrl" value="/inventory/locations">
                    <c:param name="action" value="bin-form"/>
                    <c:if test="${not empty param.zoneId}"><c:param name="zoneId" value="${param.zoneId}"/></c:if>
                    <c:if test="${not empty param.rackId}"><c:param name="rackId" value="${param.rackId}"/></c:if>
                </c:url>
                <a class="btn btn-primary" href="${newBinUrl}">+ Thêm ô</a>
            </c:if>
        </div>

        <nav class="breadcrumb" aria-label="Cấp vị trí">
            <a href="${ctx}/inventory/locations">Khu vực</a>
            <c:url var="racksNavUrl" value="/inventory/locations">
                <c:param name="action" value="racks"/>
                <c:if test="${not empty param.zoneId}"><c:param name="zoneId" value="${param.zoneId}"/></c:if>
            </c:url>
            <a href="${racksNavUrl}">Kệ</a>
            <span class="breadcrumb-current">Ô</span>
        </nav>

        <c:if test="${param.ok == 'created'}">
            <div class="alert alert-success" role="status">Đã tạo ô.</div>
        </c:if>
        <c:if test="${param.ok == 'updated'}">
            <div class="alert alert-success" role="status">Đã cập nhật ô.</div>
        </c:if>
        <c:if test="${param.ok == 'deactivated'}">
            <div class="alert alert-success" role="status">Đã ngừng dùng ô.</div>
        </c:if>
        <c:if test="${param.ok == 'activated'}">
            <div class="alert alert-success" role="status">Đã kích hoạt ô.</div>
        </c:if>
        <c:if test="${param.err == 'required'}">
            <div class="alert alert-error" role="alert">Vui lòng chọn kệ và nhập mã ô.</div>
        </c:if>
        <c:if test="${param.err == 'duplicate'}">
            <div class="alert alert-error" role="alert">Mã ô đã tồn tại trên kệ.</div>
        </c:if>
        <c:if test="${param.err == 'hasstock'}">
            <div class="alert alert-error" role="alert">Không thể ngừng dùng — ô còn tồn kho.</div>
        </c:if>
        <c:if test="${param.err == 'notfound'}">
            <div class="alert alert-error" role="alert">Không tìm thấy ô.</div>
        </c:if>
        <c:if test="${param.err == 'db'}">
            <div class="alert alert-error" role="alert">Lỗi hệ thống. Thử lại sau.</div>
        </c:if>
        <c:if test="${param.err == 'badstatus'}">
            <div class="alert alert-error" role="alert">Trạng thái không hợp lệ.</div>
        </c:if>

        <c:if test="${isForm}">
            <div class="profile-card">
                <h3>${isEdit ? 'Sửa ô' : 'Thêm ô'}</h3>
                <form class="profile-form" method="post" data-disable-on-submit
                      action="${ctx}/inventory/locations?action=${isEdit ? 'bin-update' : 'bin-create'}">
                    <c:if test="${isEdit}">
                        <input type="hidden" name="binId" value="${bin.binId}">
                        <input type="hidden" name="rackId" value="${bin.rackId}">
                        <input type="hidden" name="zoneId" value="${bin.zoneId}">
                    </c:if>
                    <div class="profile-grid">
                        <c:if test="${!isEdit}">
                            <div class="form-field">
                                <label for="binZoneId">Khu vực <span class="req">*</span></label>
                                <select id="binZoneId" aria-label="Khu vực">
                                    <option value="">— Chọn khu vực —</option>
                                    <c:forEach var="z" items="${zones}">
                                        <option value="${z.zoneId}"
                                            ${prefillZoneId == z.zoneId || param.zoneId == z.zoneId ? 'selected' : ''}>
                                            <c:out value="${z.zoneCode}"/> — <c:out value="${z.zoneName}"/>
                                        </option>
                                    </c:forEach>
                                </select>
                            </div>
                            <div class="form-field">
                                <label for="rackId">Kệ <span class="req">*</span></label>
                                <select id="rackId" name="rackId" required>
                                    <option value="">— Chọn kệ —</option>
                                    <c:forEach var="r" items="${racksInZone}">
                                        <option value="${r.rackId}" ${prefillRackId == r.rackId ? 'selected' : ''}>
                                            <c:out value="${r.rackCode}"/> — <c:out value="${r.rackName}"/>
                                        </option>
                                    </c:forEach>
                                </select>
                            </div>
                            <input type="hidden" id="zoneIdHidden" name="zoneId" value="${prefillZoneId != null ? prefillZoneId : ''}">
                        </c:if>
                        <div class="form-field">
                            <label for="binCode">Mã ô <span class="req">*</span></label>
                            <input type="text" id="binCode" name="binCode" required maxlength="50"
                                   value="<c:out value='${isEdit ? bin.binCode : ""}'/>"
                                   placeholder="VD: B1, BIN-01">
                        </div>
                    </div>
                    <div class="profile-actions">
                        <button type="submit" class="btn btn-primary">${isEdit ? 'Lưu' : 'Tạo'}</button>
                        <c:url var="cancelBinUrl" value="/inventory/locations">
                            <c:param name="action" value="bins"/>
                            <c:if test="${not empty param.zoneId}"><c:param name="zoneId" value="${param.zoneId}"/></c:if>
                            <c:if test="${not empty param.rackId}"><c:param name="rackId" value="${param.rackId}"/></c:if>
                        </c:url>
                        <a class="btn btn-ghost" href="${cancelBinUrl}">Hủy</a>
                    </div>
                </form>
            </div>
        </c:if>

        <form class="filter-bar" method="get" action="${ctx}/inventory/locations" id="binFilterForm">
            <input type="hidden" name="action" value="bins">
            <select name="zoneId" id="filterZoneId" aria-label="Khu vực">
                <option value="">Tất cả khu vực</option>
                <c:forEach var="z" items="${zones}">
                    <option value="${z.zoneId}" ${param.zoneId == z.zoneId ? 'selected' : ''}>
                        <c:out value="${z.zoneCode}"/>
                    </option>
                </c:forEach>
            </select>
            <select name="rackId" id="filterRackId" aria-label="Kệ">
                <option value="">Tất cả kệ</option>
                <c:forEach var="r" items="${racksInZone}">
                    <option value="${r.rackId}" ${param.rackId == r.rackId ? 'selected' : ''}>
                        <c:out value="${r.rackCode}"/> — <c:out value="${r.rackName}"/>
                    </option>
                </c:forEach>
            </select>
            <input type="search" name="q" value="<c:out value='${param.q}'/>"
                   placeholder="Tìm mã ô…" aria-label="Tìm kiếm">
            <select name="status" aria-label="Trạng thái">
                <option value="">Tất cả trạng thái</option>
                <option value="HOAT_DONG" ${param.status == 'HOAT_DONG' ? 'selected' : ''}>Đang dùng</option>
                <option value="NGUNG_HOAT_DONG" ${param.status == 'NGUNG_HOAT_DONG' ? 'selected' : ''}>Ngừng dùng</option>
            </select>
            <button type="submit" class="btn btn-secondary btn-sm">Lọc</button>
            <a href="${ctx}/inventory/locations?action=bins" class="btn btn-ghost btn-sm">Đặt lại</a>
        </form>

        <div class="admin-card">
            <c:choose>
                <c:when test="${empty bins}">
                    <div class="empty-state"><p>Không có ô phù hợp.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="admin-table admin-table-fixed">
                        <thead>
                        <tr>
                            <th class="col-id">ID</th>
                            <th>Đường dẫn</th>
                            <th>Mã ô</th>
                            <th class="col-num">Tồn thực tế</th>
                            <th class="col-num">Đã đặt</th>
                            <th class="col-status">Trạng thái</th>
                            <th class="col-act">Thao tác</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="b" items="${bins}">
                            <tr>
                                <td><c:out value="${b.binId}"/></td>
                                <td><c:out value="${b.fullCode}"/></td>
                                <td><strong><c:out value="${b.binCode}"/></strong></td>
                                <td><c:out value="${b.onHandTotal != null ? b.onHandTotal : 0}"/></td>
                                <td><c:out value="${b.reservedTotal != null ? b.reservedTotal : 0}"/></td>
                                <td>
                                    <span class="status-badge status-${b.status == 'HOAT_DONG' ? 'active' : 'inactive'}">
                                        <c:out value="${b.statusLabel}"/>
                                    </span>
                                </td>
                                <td class="col-actions">
                                    <a class="btn btn-secondary btn-sm"
                                       href="${ctx}/inventory/locations?action=bin-form&amp;id=${b.binId}">Sửa</a>
                                    <c:choose>
                                        <c:when test="${b.status == 'HOAT_DONG'}">
                                            <c:choose>
                                                <c:when test="${b.onHandTotal != null && b.onHandTotal > 0}">
                                                    <span class="btn btn-ghost btn-sm" title="Còn tồn kho">Ngừng dùng</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <form method="post" action="${ctx}/inventory/locations?action=bin-deactivate" class="inline-form"
                                                          onsubmit="return confirm('Ngừng dùng ô này?');">
                                                        <input type="hidden" name="binId" value="${b.binId}">
                                                        <input type="hidden" name="rackId" value="${b.rackId}">
                                                        <input type="hidden" name="zoneId" value="${b.zoneId}">
                                                        <button type="submit" class="btn btn-ghost btn-sm btn-danger">Ngừng dùng</button>
                                                    </form>
                                                </c:otherwise>
                                            </c:choose>
                                        </c:when>
                                        <c:otherwise>
                                            <form method="post" action="${ctx}/inventory/locations?action=bin-activate" class="inline-form">
                                                <input type="hidden" name="binId" value="${b.binId}">
                                                <input type="hidden" name="rackId" value="${b.rackId}">
                                                <input type="hidden" name="zoneId" value="${b.zoneId}">
                                                <button type="submit" class="btn btn-ghost btn-sm btn-success">Kích hoạt</button>
                                            </form>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                    <c:url var="pagerBase" value="/inventory/locations">
                        <c:param name="action" value="bins"/>
                        <c:param name="zoneId" value="${param.zoneId}"/>
                        <c:param name="rackId" value="${param.rackId}"/>
                        <c:param name="q" value="${param.q}"/>
                        <c:param name="status" value="${param.status}"/>
                    </c:url>
                    <c:set var="pagerPages" value="${totalPages}"/>
                    <%@ include file="/WEB-INF/jspf/pager.jspf" %>
                </c:otherwise>
            </c:choose>
        </div>
    </main>
</div>

<%@ include file="/WEB-INF/jspf/admin-footer.jspf" %>

<script>
    (function () {
        var ctx = '<c:out value="${ctx}"/>';
        var binZone = document.getElementById('binZoneId');
        var rackSelect = document.getElementById('rackId');
        var zoneHidden = document.getElementById('zoneIdHidden');
        if (binZone && rackSelect) {
            binZone.addEventListener('change', function () {
                var zid = binZone.value;
                if (zoneHidden) zoneHidden.value = zid;
                rackSelect.innerHTML = '<option value="">— Chọn kệ —</option>';
                if (!zid) return;
                fetch(ctx + '/inventory/locations?action=api-racks&zoneId=' + encodeURIComponent(zid))
                    .then(function (r) { return r.json(); })
                    .then(function (rows) {
                        rows.forEach(function (row) {
                            var opt = document.createElement('option');
                            opt.value = row.rackId;
                            opt.textContent = row.rackCode + ' — ' + row.rackName;
                            rackSelect.appendChild(opt);
                        });
                    });
            });
            if (binZone.value && rackSelect.options.length <= 1) {
                binZone.dispatchEvent(new Event('change'));
            }
        }
        var filterZone = document.getElementById('filterZoneId');
        var filterRack = document.getElementById('filterRackId');
        if (filterZone && filterRack) {
            filterZone.addEventListener('change', function () {
                var zid = filterZone.value;
                filterRack.innerHTML = '<option value="">Tất cả kệ</option>';
                if (!zid) return;
                fetch(ctx + '/inventory/locations?action=api-racks&zoneId=' + encodeURIComponent(zid))
                    .then(function (r) { return r.json(); })
                    .then(function (rows) {
                        rows.forEach(function (row) {
                            var opt = document.createElement('option');
                            opt.value = row.rackId;
                            opt.textContent = row.rackCode + ' — ' + row.rackName;
                            filterRack.appendChild(opt);
                        });
                    });
            });
        }
        document.querySelectorAll('form[data-disable-on-submit]').forEach(function (form) {
            form.addEventListener('submit', function () {
                form.querySelectorAll('button[type="submit"]').forEach(function (btn) { btn.disabled = true; });
            });
        });
    })();
</script>
</body>
</html>
