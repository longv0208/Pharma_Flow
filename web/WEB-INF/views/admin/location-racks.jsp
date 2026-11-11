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
    <title>Vị trí kho — Kệ — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <div class="section-head">
            <div>
                <h2>Vị trí kho — Kệ</h2>
                <p class="section-sub">
                    <c:if test="${not empty filterZone}">
                        Khu vực: <strong><c:out value="${filterZone.zoneCode}"/> — <c:out value="${filterZone.zoneName}"/></strong>
                    </c:if>
                    <c:if test="${empty filterZone}">Danh sách kệ trong kho.</c:if>
                </p>
            </div>
            <c:if test="${!isForm}">
                <c:url var="newRackUrl" value="/inventory/locations">
                    <c:param name="action" value="rack-form"/>
                    <c:if test="${not empty param.zoneId}"><c:param name="zoneId" value="${param.zoneId}"/></c:if>
                </c:url>
                <a class="btn btn-primary" href="${newRackUrl}">+ Thêm kệ</a>
            </c:if>
        </div>

        <nav class="breadcrumb" aria-label="Cấp vị trí">
            <a href="${ctx}/inventory/locations">Khu vực</a>
            <span class="breadcrumb-current">Kệ</span>
            <c:url var="binsNavUrl" value="/inventory/locations">
                <c:param name="action" value="bins"/>
                <c:if test="${not empty param.zoneId}"><c:param name="zoneId" value="${param.zoneId}"/></c:if>
            </c:url>
            <a href="${binsNavUrl}">Ô</a>
        </nav>

        <c:if test="${param.ok == 'created'}">
            <div class="alert alert-success" role="status">Đã tạo kệ.</div>
        </c:if>
        <c:if test="${param.ok == 'updated'}">
            <div class="alert alert-success" role="status">Đã cập nhật kệ.</div>
        </c:if>
        <c:if test="${param.ok == 'deactivated'}">
            <div class="alert alert-success" role="status">Đã ngừng dùng kệ.</div>
        </c:if>
        <c:if test="${param.ok == 'activated'}">
            <div class="alert alert-success" role="status">Đã kích hoạt kệ.</div>
        </c:if>
        <c:if test="${param.err == 'required'}">
            <div class="alert alert-error" role="alert">Vui lòng chọn khu vực và nhập mã, tên kệ.</div>
        </c:if>
        <c:if test="${param.err == 'duplicate'}">
            <div class="alert alert-error" role="alert">Mã kệ đã tồn tại trong khu vực.</div>
        </c:if>
        <c:if test="${param.err == 'hasstock'}">
            <div class="alert alert-error" role="alert">Không thể ngừng dùng — kệ còn tồn kho.</div>
        </c:if>
        <c:if test="${param.err == 'notfound'}">
            <div class="alert alert-error" role="alert">Không tìm thấy kệ.</div>
        </c:if>
        <c:if test="${param.err == 'db'}">
            <div class="alert alert-error" role="alert">Lỗi hệ thống. Thử lại sau.</div>
        </c:if>
        <c:if test="${param.err == 'badstatus'}">
            <div class="alert alert-error" role="alert">Trạng thái không hợp lệ.</div>
        </c:if>

        <c:if test="${isForm}">
            <div class="profile-card">
                <h3>${isEdit ? 'Sửa kệ' : 'Thêm kệ'}</h3>
                <form class="profile-form" method="post" data-disable-on-submit
                      action="${ctx}/inventory/locations?action=${isEdit ? 'rack-update' : 'rack-create'}">
                    <c:if test="${isEdit}">
                        <input type="hidden" name="rackId" value="${rack.rackId}">
                        <input type="hidden" name="zoneId" value="${rack.zoneId}">
                    </c:if>
                    <div class="profile-grid">
                        <c:if test="${!isEdit}">
                            <div class="form-field">
                                <label for="zoneId">Khu vực <span class="req">*</span></label>
                                <select id="zoneId" name="zoneId" required>
                                    <option value="">— Chọn khu vực —</option>
                                    <c:forEach var="z" items="${zones}">
                                        <option value="${z.zoneId}"
                                            ${prefillZoneId == z.zoneId || param.zoneId == z.zoneId ? 'selected' : ''}>
                                            <c:out value="${z.zoneCode}"/> — <c:out value="${z.zoneName}"/>
                                        </option>
                                    </c:forEach>
                                </select>
                            </div>
                        </c:if>
                        <div class="form-field">
                            <label for="rackCode">Mã kệ <span class="req">*</span></label>
                            <input type="text" id="rackCode" name="rackCode" required maxlength="50"
                                   value="<c:out value='${isEdit ? rack.rackCode : ""}'/>">
                        </div>
                        <div class="form-field">
                            <label for="rackName">Tên kệ <span class="req">*</span></label>
                            <input type="text" id="rackName" name="rackName" required maxlength="200"
                                   value="<c:out value='${isEdit ? rack.rackName : ""}'/>">
                        </div>
                    </div>
                    <div class="profile-actions">
                        <button type="submit" class="btn btn-primary">${isEdit ? 'Lưu' : 'Tạo'}</button>
                        <c:url var="cancelRackUrl" value="/inventory/locations">
                            <c:param name="action" value="racks"/>
                            <c:if test="${not empty param.zoneId}"><c:param name="zoneId" value="${param.zoneId}"/></c:if>
                        </c:url>
                        <a class="btn btn-ghost" href="${cancelRackUrl}">Hủy</a>
                    </div>
                </form>
            </div>
        </c:if>

        <form class="filter-bar" method="get" action="${ctx}/inventory/locations">
            <input type="hidden" name="action" value="racks">
            <select name="zoneId" aria-label="Khu vực">
                <option value="">Tất cả khu vực</option>
                <c:forEach var="z" items="${zones}">
                    <option value="${z.zoneId}" ${param.zoneId == z.zoneId ? 'selected' : ''}>
                        <c:out value="${z.zoneCode}"/> — <c:out value="${z.zoneName}"/>
                    </option>
                </c:forEach>
            </select>
            <input type="search" name="q" value="<c:out value='${param.q}'/>"
                   placeholder="Tìm mã kệ…" aria-label="Tìm kiếm">
            <select name="status" aria-label="Trạng thái">
                <option value="">Tất cả trạng thái</option>
                <option value="HOAT_DONG" ${param.status == 'HOAT_DONG' ? 'selected' : ''}>Đang dùng</option>
                <option value="NGUNG_HOAT_DONG" ${param.status == 'NGUNG_HOAT_DONG' ? 'selected' : ''}>Ngừng dùng</option>
            </select>
            <button type="submit" class="btn btn-secondary btn-sm">Lọc</button>
            <a href="${ctx}/inventory/locations?action=racks" class="btn btn-ghost btn-sm">Đặt lại</a>
        </form>

        <div class="admin-card">
            <c:choose>
                <c:when test="${empty racks}">
                    <div class="empty-state"><p>Không có kệ phù hợp.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="admin-table admin-table-fixed">
                        <thead>
                        <tr>
                            <th class="col-id">ID</th>
                            <th>Khu vực</th>
                            <th>Mã kệ</th>
                            <th>Tên kệ</th>
                            <th class="col-num">Số ô</th>
                            <th class="col-num">Tồn thực tế</th>
                            <th class="col-status">Trạng thái</th>
                            <th class="col-act">Thao tác</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="r" items="${racks}">
                            <tr>
                                <td><c:out value="${r.rackId}"/></td>
                                <td><c:out value="${r.zoneCode}"/></td>
                                <td><strong><c:out value="${r.rackCode}"/></strong></td>
                                <td><c:out value="${r.rackName}"/></td>
                                <td><c:out value="${r.binCount != null ? r.binCount : 0}"/></td>
                                <td><c:out value="${r.onHandTotal != null ? r.onHandTotal : 0}"/></td>
                                <td>
                                    <span class="status-badge status-${r.status == 'HOAT_DONG' ? 'active' : 'inactive'}">
                                        <c:out value="${r.statusLabel}"/>
                                    </span>
                                </td>
                                <td class="col-actions">
                                    <a class="btn btn-secondary btn-sm"
                                       href="${ctx}/inventory/locations?action=bins&amp;zoneId=${r.zoneId}&amp;rackId=${r.rackId}">Ô</a>
                                    <a class="btn btn-secondary btn-sm"
                                       href="${ctx}/inventory/locations?action=rack-form&amp;id=${r.rackId}">Sửa</a>
                                    <c:choose>
                                        <c:when test="${r.status == 'HOAT_DONG'}">
                                            <c:choose>
                                                <c:when test="${r.onHandTotal != null && r.onHandTotal > 0}">
                                                    <span class="btn btn-ghost btn-sm" title="Còn tồn kho">Ngừng dùng</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <form method="post" action="${ctx}/inventory/locations?action=rack-deactivate" class="inline-form"
                                                          onsubmit="return confirm('Ngừng dùng kệ này?');">
                                                        <input type="hidden" name="rackId" value="${r.rackId}">
                                                        <input type="hidden" name="zoneId" value="${r.zoneId}">
                                                        <button type="submit" class="btn btn-ghost btn-sm btn-danger">Ngừng dùng</button>
                                                    </form>
                                                </c:otherwise>
                                            </c:choose>
                                        </c:when>
                                        <c:otherwise>
                                            <form method="post" action="${ctx}/inventory/locations?action=rack-activate" class="inline-form">
                                                <input type="hidden" name="rackId" value="${r.rackId}">
                                                <input type="hidden" name="zoneId" value="${r.zoneId}">
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
                        <c:param name="action" value="racks"/>
                        <c:param name="zoneId" value="${param.zoneId}"/>
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
