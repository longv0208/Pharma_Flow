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
    <title>Vị trí kho — Khu vực — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <div class="section-head">
            <div>
                <h2>Vị trí kho — Khu vực</h2>
                <p class="section-sub">Quản lý khu vực (zone), kệ (rack) và ô (bin). Ngừng dùng khi không còn tồn tại vị trí.</p>
            </div>
            <c:if test="${!isForm}">
                <a class="btn btn-primary" href="${ctx}/inventory/locations?action=zone-form">+ Thêm khu vực</a>
            </c:if>
        </div>

        <nav class="breadcrumb" aria-label="Cấp vị trí">
            <span class="breadcrumb-current">Khu vực</span>
            <a href="${ctx}/inventory/locations?action=racks">Kệ</a>
            <a href="${ctx}/inventory/locations?action=bins">Ô</a>
        </nav>

        <c:if test="${param.ok == 'created'}">
            <div class="alert alert-success" role="status">Đã tạo khu vực.</div>
        </c:if>
        <c:if test="${param.ok == 'updated'}">
            <div class="alert alert-success" role="status">Đã cập nhật khu vực.</div>
        </c:if>
        <c:if test="${param.ok == 'deactivated'}">
            <div class="alert alert-success" role="status">Đã ngừng dùng khu vực.</div>
        </c:if>
        <c:if test="${param.ok == 'activated'}">
            <div class="alert alert-success" role="status">Đã kích hoạt khu vực.</div>
        </c:if>
        <c:if test="${param.err == 'required'}">
            <div class="alert alert-error" role="alert">Vui lòng nhập mã và tên khu vực.</div>
        </c:if>
        <c:if test="${param.err == 'duplicate'}">
            <div class="alert alert-error" role="alert">Mã khu vực đã tồn tại.</div>
        </c:if>
        <c:if test="${param.err == 'hasstock'}">
            <div class="alert alert-error" role="alert">Không thể ngừng dùng — vị trí còn tồn kho.</div>
        </c:if>
        <c:if test="${param.err == 'notfound'}">
            <div class="alert alert-error" role="alert">Không tìm thấy khu vực.</div>
        </c:if>
        <c:if test="${param.err == 'db'}">
            <div class="alert alert-error" role="alert">Lỗi hệ thống. Thử lại sau.</div>
        </c:if>
        <c:if test="${param.err == 'badstatus'}">
            <div class="alert alert-error" role="alert">Trạng thái không hợp lệ.</div>
        </c:if>

        <c:if test="${isForm}">
            <div class="profile-card">
                <h3>${isEdit ? 'Sửa khu vực' : 'Thêm khu vực'}</h3>
                <form class="profile-form" method="post" data-disable-on-submit
                      action="${ctx}/inventory/locations?action=${isEdit ? 'zone-update' : 'zone-create'}">
                    <c:if test="${isEdit}">
                        <input type="hidden" name="zoneId" value="${zone.zoneId}">
                    </c:if>
                    <div class="profile-grid">
                        <div class="form-field">
                            <label for="zoneCode">Mã khu vực <span class="req">*</span></label>
                            <input type="text" id="zoneCode" name="zoneCode" required maxlength="50"
                                   value="<c:out value='${isEdit ? zone.zoneCode : ""}'/>"
                                   placeholder="VD: A, LNH">
                        </div>
                        <div class="form-field">
                            <label for="zoneName">Tên khu vực <span class="req">*</span></label>
                            <input type="text" id="zoneName" name="zoneName" required maxlength="200"
                                   value="<c:out value='${isEdit ? zone.zoneName : ""}'/>">
                        </div>
                    </div>
                    <div class="profile-actions">
                        <button type="submit" class="btn btn-primary">${isEdit ? 'Lưu' : 'Tạo'}</button>
                        <a class="btn btn-ghost" href="${ctx}/inventory/locations">Hủy</a>
                    </div>
                </form>
            </div>
        </c:if>

        <form class="filter-bar" method="get" action="${ctx}/inventory/locations">
            <input type="search" name="q" value="<c:out value='${param.q}'/>"
                   placeholder="Tìm mã hoặc tên khu vực…" aria-label="Tìm kiếm">
            <select name="status" aria-label="Trạng thái">
                <option value="">Tất cả trạng thái</option>
                <option value="HOAT_DONG" ${param.status == 'HOAT_DONG' ? 'selected' : ''}>Đang dùng</option>
                <option value="NGUNG_HOAT_DONG" ${param.status == 'NGUNG_HOAT_DONG' ? 'selected' : ''}>Ngừng dùng</option>
            </select>
            <button type="submit" class="btn btn-secondary btn-sm">Lọc</button>
            <a href="${ctx}/inventory/locations" class="btn btn-ghost btn-sm">Đặt lại</a>
        </form>

        <div class="admin-card">
            <c:choose>
                <c:when test="${empty zones}">
                    <div class="empty-state"><p>Chưa có khu vực nào.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="admin-table admin-table-fixed">
                        <thead>
                        <tr>
                            <th class="col-id">ID</th>
                            <th>Mã</th>
                            <th>Tên</th>
                            <th class="col-num">Số kệ</th>
                            <th class="col-num">Tồn thực tế</th>
                            <th class="col-status">Trạng thái</th>
                            <th class="col-act">Thao tác</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="z" items="${zones}">
                            <tr>
                                <td><c:out value="${z.zoneId}"/></td>
                                <td><strong><c:out value="${z.zoneCode}"/></strong></td>
                                <td><c:out value="${z.zoneName}"/></td>
                                <td><c:out value="${z.rackCount != null ? z.rackCount : 0}"/></td>
                                <td><c:out value="${z.onHandTotal != null ? z.onHandTotal : 0}"/></td>
                                <td>
                                    <span class="status-badge status-${z.status == 'HOAT_DONG' ? 'active' : 'inactive'}">
                                        <c:out value="${z.statusLabel}"/>
                                    </span>
                                </td>
                                <td class="col-actions">
                                    <a class="btn btn-secondary btn-sm"
                                       href="${ctx}/inventory/locations?action=racks&amp;zoneId=${z.zoneId}">Kệ</a>
                                    <a class="btn btn-secondary btn-sm"
                                       href="${ctx}/inventory/locations?action=zone-form&amp;id=${z.zoneId}">Sửa</a>
                                    <c:choose>
                                        <c:when test="${z.status == 'HOAT_DONG'}">
                                            <c:choose>
                                                <c:when test="${z.onHandTotal != null && z.onHandTotal > 0}">
                                                    <span class="btn btn-ghost btn-sm" title="Còn tồn kho — không thể ngừng dùng">Ngừng dùng</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <form method="post" action="${ctx}/inventory/locations?action=zone-deactivate" class="inline-form"
                                                          onsubmit="return confirm('Ngừng dùng khu vực này? Kệ và ô bên trong vẫn giữ nguyên.');">
                                                        <input type="hidden" name="zoneId" value="${z.zoneId}">
                                                        <button type="submit" class="btn btn-ghost btn-sm btn-danger">Ngừng dùng</button>
                                                    </form>
                                                </c:otherwise>
                                            </c:choose>
                                        </c:when>
                                        <c:otherwise>
                                            <form method="post" action="${ctx}/inventory/locations?action=zone-activate" class="inline-form">
                                                <input type="hidden" name="zoneId" value="${z.zoneId}">
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
    document.querySelectorAll('form[data-disable-on-submit]').forEach(function (form) {
        form.addEventListener('submit', function () {
            form.querySelectorAll('button[type="submit"]').forEach(function (btn) { btn.disabled = true; });
        });
    });
</script>
</body>
</html>
