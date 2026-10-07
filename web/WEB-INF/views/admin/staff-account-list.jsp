<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="staff-accounts"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Tài khoản nhân viên — Quản trị — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <div class="section-head">
            <div>
                <h2>Tài khoản nhân viên</h2>
                <p class="section-sub">${total} tài khoản. Nhân viên ngừng hoạt động vẫn giữ lại lịch sử giao dịch.</p>
            </div>
            <a class="btn btn-primary" href="${ctx}/admin/staff-accounts?action=new">+ Thêm nhân viên</a>
        </div>

        <%-- Filter bar: GET keeps filters bookmarkable --%>
        <form class="filter-bar" method="get" action="${ctx}/admin/staff-accounts">
            <input type="hidden" name="action" value="list">
            <input type="search" name="q" value="<c:out value='${param.q}'/>"
                   placeholder="Tìm theo tên, email, tên đăng nhập, mã nhân viên…"
                   aria-label="Tìm kiếm nhân viên">
            <select name="role" aria-label="Vai trò">
                <option value="">Tất cả vai trò</option>
                <option value="NHAN_VIEN"          ${param.role == 'NHAN_VIEN'          ? 'selected' : ''}>Nhân viên</option>
                <option value="NHAN_VIEN_GIAO_HANG" ${param.role == 'NHAN_VIEN_GIAO_HANG' ? 'selected' : ''}>Nhân viên giao hàng</option>
            </select>
            <select name="status" aria-label="Trạng thái">
                <option value="">Tất cả trạng thái</option>
                <option value="HOAT_DONG"       ${param.status == 'HOAT_DONG'       ? 'selected' : ''}>Hoạt động</option>
                <option value="NGUNG_HOAT_DONG" ${param.status == 'NGUNG_HOAT_DONG' ? 'selected' : ''}>Ngừng hoạt động</option>
            </select>
            <button type="submit" class="btn btn-secondary btn-sm">Lọc</button>
        </form>

        <c:if test="${param.ok == 'created'}">
            <div class="alert alert-success" role="status">Đã tạo tài khoản nhân viên.</div>
        </c:if>
        <c:if test="${param.ok == 'updated'}">
            <div class="alert alert-success" role="status">Đã cập nhật thông tin nhân viên.</div>
        </c:if>
        <c:if test="${param.ok == 'activated'}">
            <div class="alert alert-success" role="status">Đã kích hoạt tài khoản.</div>
        </c:if>
        <c:if test="${param.ok == 'deactivated'}">
            <div class="alert alert-success" role="status">Đã ngừng hoạt động tài khoản.</div>
        </c:if>
        <c:if test="${param.ok == 'password-reset'}">
            <div class="alert alert-success" role="status">Đã đặt lại mật khẩu.</div>
        </c:if>
        <c:if test="${param.err == 'notfound'}">
            <div class="alert alert-error" role="alert">Không tìm thấy tài khoản nhân viên.</div>
        </c:if>

        <div class="admin-card">
            <c:choose>
                <c:when test="${empty rows}">
                    <div class="empty-state"><p>Không có tài khoản nhân viên phù hợp.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="admin-table admin-table-fixed">
                        <thead>
                        <tr>
                            <th>Mã NV</th>
                            <th>Họ tên</th>
                            <th>Tên đăng nhập</th>
                            <th>Email</th>
                            <th>Số điện thoại</th>
                            <th>Vai trò</th>
                            <th class="col-status">Trạng thái</th>
                            <th>Ngày tạo</th>
                            <th class="col-act">Thao tác</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="r" items="${rows}">
                            <tr>
                                <td>
                                    <c:choose>
                                        <c:when test="${not empty r.employeeCode}">
                                            <c:out value="${r.employeeCode}"/>
                                        </c:when>
                                        <c:otherwise>—</c:otherwise>
                                    </c:choose>
                                </td>
                                <td><c:out value="${r.fullName}"/></td>
                                <td><c:out value="${r.username}"/></td>
                                <td class="col-desc"><c:out value="${r.email}"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${not empty r.phone}">
                                            <c:out value="${r.phone}"/>
                                        </c:when>
                                        <c:otherwise>—</c:otherwise>
                                    </c:choose>
                                </td>
                                <td><c:out value="${r.roleLabel}"/></td>
                                <td>
                                    <span class="status-badge status-${r.status == 'HOAT_DONG' ? 'active' : 'inactive'}">
                                        <c:out value="${r.statusLabel}"/>
                                    </span>
                                </td>
                                <td><fmt:formatDate value="${r.createdAt}" pattern="yyyy-MM-dd"/></td>
                                <td class="col-actions">
                                    <a class="btn btn-secondary btn-sm"
                                       href="${ctx}/admin/staff-accounts?action=edit&id=${r.userId}">Sửa</a>
                                    <a class="btn btn-ghost btn-sm"
                                       href="${ctx}/admin/staff-accounts?action=password&id=${r.userId}">Mật khẩu</a>
                                    <c:choose>
    <c:when test="${r.status == 'HOAT_DONG'}">
        <form method="post" action="${ctx}/admin/staff-accounts?action=deactivate" class="inline-form"
                                              onsubmit="return confirm('Ngừng hoạt động tài khoản này? Nhân viên sẽ không đăng nhập được nhưng lịch sử vẫn được giữ lại.');">
                                            <input type="hidden" name="userId" value="${r.userId}">
                                            <button type="submit" class="btn btn-ghost btn-sm btn-danger">Ngừng</button>
                                        </form>
    </c:when>
    <c:otherwise>
        <form method="post" action="${ctx}/admin/staff-accounts?action=activate" class="inline-form">
            <input type="hidden" name="userId" value="${r.userId}">
            <button type="submit" class="btn btn-ghost btn-sm btn-success">Kích hoạt</button>
        </form>
    </c:otherwise>
</c:choose>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>

                    <%-- Pagination --%>
                    <c:if test="${pages > 1}">
                        <nav class="pager" aria-label="Trang">
                            <c:forEach var="i" begin="1" end="${pages}">
                                <c:url var="pageUrl" value="/admin/staff-accounts">
                                    <c:param name="action" value="list"/>
                                    <c:param name="q" value="${param.q}"/>
                                    <c:param name="role" value="${param.role}"/>
                                    <c:param name="status" value="${param.status}"/>
                                    <c:param name="page" value="${i}"/>
                                </c:url>
                                <a class="pager-num ${i == page ? 'current' : ''}" href="${pageUrl}">${i}</a>
                            </c:forEach>
                        </nav>
                    </c:if>
                </c:otherwise>
            </c:choose>
        </div>
    </main>
</div>

<%@ include file="/WEB-INF/jspf/admin-footer.jspf" %>

</body>
</html>
