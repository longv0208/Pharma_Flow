<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="categories"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Danh mục — Quản trị — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <div class="section-head">
            <div>
                <h2>Quản lý danh mục</h2>
                <p class="section-sub">Tổ chức danh mục sản phẩm. Danh mục ngừng bán bị ẩn khỏi cửa hàng nhưng vẫn giữ sản phẩm.</p>
            </div>
            <a class="btn btn-primary" href="${ctx}/admin?action=category-new">+ Thêm danh mục</a>
        </div>

        <c:if test="${param.ok == 'created'}">
            <div class="alert alert-success" role="status">Đã tạo danh mục.</div>
        </c:if>
        <c:if test="${param.ok == 'updated'}">
            <div class="alert alert-success" role="status">Đã cập nhật danh mục.</div>
        </c:if>
        <c:if test="${param.ok == 'deactivated'}">
            <div class="alert alert-success" role="status">Đã ngừng bán danh mục.</div>
        </c:if>
        <c:if test="${param.ok == 'activated'}">
            <div class="alert alert-success" role="status">Đã kích hoạt danh mục.</div>
        </c:if>
        <c:if test="${param.err == 'notfound'}">
            <div class="alert alert-error" role="alert">Không tìm thấy danh mục.</div>
        </c:if>

        <div class="admin-card">
            <c:choose>
                <c:when test="${empty categories}">
                    <div class="empty-state"><p>Chưa có danh mục nào.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="admin-table admin-table-fixed table-categories">
                        <thead>
                        <tr>
                            <th class="col-id">ID</th>
                            <th class="col-name">Tên</th>
                            <th>Mô tả</th>
                            <th class="col-status">Trạng thái</th>
                            <th class="col-act">Thao tác</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="c" items="${categories}">
                            <tr>
                                <td><c:out value="${c.categoryId}"/></td>
                                <td><c:out value="${c.categoryName}"/></td>
                                <td class="col-desc"><c:out value="${c.description}"/></td>
                                <td>
                                    <span class="status-badge status-${c.status == 'HOAT_DONG' ? 'active' : 'inactive'}">
                                        <c:out value="${c.status == 'HOAT_DONG' ? 'Đang bán' : 'Ngừng bán'}"/>
                                    </span>
                                </td>
                                <td class="col-actions">
                                    <a class="btn btn-secondary btn-sm"
                                       href="${ctx}/admin?action=category-edit&id=${c.categoryId}">Sửa</a>
                                    <c:choose>
                                        <c:when test="${c.status == 'HOAT_DONG'}">
                                            <form method="post" action="${ctx}/admin?action=category-delete" class="inline-form"
                                                  onsubmit="return confirm('Ngừng bán danh mục này? Sản phẩm bên trong được giữ lại nhưng bị ẩn.');">
                                                <input type="hidden" name="categoryId" value="${c.categoryId}">
                                                <button type="submit" class="btn btn-ghost btn-sm btn-danger">Ngừng bán</button>
                                            </form>
                                        </c:when>
                                        <c:otherwise>
                                            <form method="post" action="${ctx}/admin?action=category-activate" class="inline-form">
                                                <input type="hidden" name="categoryId" value="${c.categoryId}">
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
