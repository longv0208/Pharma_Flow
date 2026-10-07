<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="suppliers"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Nhà cung cấp — Admin — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <div class="section-head">
            <div>
                <h2>Quản lý Nhà cung cấp</h2>
                <p class="section-sub">Đối tác cung cấp sản phẩm cho nhà thuốc. Nhà cung cấp ngừng hoạt động vẫn được giữ lại lịch sử.</p>
            </div>
            <a class="btn btn-primary" href="${ctx}/admin?action=supplier-new">+ Thêm Nhà cung cấp</a>
        </div>

        <c:if test="${param.ok == 'created'}">
            <div class="alert alert-success" role="status">Đã tạo nhà cung cấp.</div>
        </c:if>
        <c:if test="${param.ok == 'updated'}">
            <div class="alert alert-success" role="status">Đã cập nhật nhà cung cấp.</div>
        </c:if>
        <c:if test="${param.ok == 'deactivated'}">
            <div class="alert alert-success" role="status">Đã ngừng hoạt động nhà cung cấp.</div>
        </c:if>
        <c:if test="${param.ok == 'activated'}">
            <div class="alert alert-success" role="status">Đã kích hoạt nhà cung cấp.</div>
        </c:if>
        <c:if test="${param.err == 'notfound'}">
            <div class="alert alert-error" role="alert">Không tìm thấy nhà cung cấp.</div>
        </c:if>

        <div class="admin-card">
            <c:choose>
                <c:when test="${empty suppliers}">
                    <div class="empty-state"><p>Chưa có nhà cung cấp nào.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="admin-table admin-table-fixed table-suppliers">
                        <thead>
                        <tr>
                            <th class="col-id">ID</th>
                            <th>Tên nhà cung cấp</th>
                            <th>Người liên hệ</th>
                            <th>Số điện thoại</th>
                            <th>Email</th>
                            <th class="col-status">Trạng thái</th>
                            <th class="col-act">Thao tác</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="s" items="${suppliers}">
                            <tr>
                                <td><c:out value="${s.supplierId}"/></td>
                                <td><c:out value="${s.supplierName}"/></td>
                                <td><c:out value="${s.contactPerson}"/></td>
                                <td><c:out value="${s.phone}"/></td>
                                <td class="col-desc"><c:out value="${s.email}"/></td>
                                <td>
                                    <span class="status-badge status-${s.status == 'HOAT_DONG' ? 'active' : 'inactive'}">
                                        <c:out value="${s.status == 'HOAT_DONG' ? 'Hoạt động' : 'Ngừng hoạt động'}"/>
                                    </span>
                                </td>
                                <td class="col-actions">
                                    <a class="btn btn-secondary btn-sm"
                                       href="${ctx}/admin?action=supplier-edit&id=${s.supplierId}">Sửa</a>
                                    <c:choose>
    <c:when test="${s.status == 'HOAT_DONG'}">
        <form method="post" action="${ctx}/admin?action=supplier-delete" class="inline-form"
                                              onsubmit="return confirm('Ngừng hoạt động nhà cung cấp này? Lịch sử mua hàng vẫn được giữ lại.');">
                                            <input type="hidden" name="supplierId" value="${s.supplierId}">
                                            <button type="submit" class="btn btn-ghost btn-sm btn-danger">Ngừng hoạt động</button>
                                        </form>
    </c:when>
    <c:otherwise>
        <form method="post" action="${ctx}/admin?action=supplier-activate" class="inline-form">
            <input type="hidden" name="supplierId" value="${s.supplierId}">
            <button type="submit" class="btn btn-ghost btn-sm btn-success">Kích hoạt</button>
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
        </div>
    </main>
</div>

<%@ include file="/WEB-INF/jspf/admin-footer.jspf" %>

</body>
</html>
