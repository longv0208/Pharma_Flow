<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="stocktake"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Kiểm kê — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <div class="section-head">
            <div>
                <h2>Kiểm kê</h2>
                <p class="section-sub">Đếm và đối chiếu tồn kho vật lý định kỳ.</p>
            </div>
            <form method="post" action="${ctx}/inventory/stocktakes"
                  onsubmit="this.querySelector('button').disabled = true;">
                <input type="hidden" name="action" value="create">
                <button type="submit" class="btn btn-primary">Kiểm kê Mới</button>
            </form>
        </div>

        <c:if test="${param.ok == 'created'}">
            <div class="alert alert-success" role="status">Đã tạo kiểm kê.</div>
        </c:if>
        <c:if test="${param.err == 'notfound'}">
            <div class="alert alert-error" role="alert">Không tìm thấy kiểm kê.</div>
        </c:if>
        <c:if test="${param.err == 'db'}">
            <div class="alert alert-error" role="alert">Lỗi cơ sở dữ liệu — kiểm kê chưa được lưu.</div>
        </c:if>

        <%-- Status filter --%>
        <div class="filter-bar">
            <a class="btn ${empty param.status ? 'btn-secondary' : 'btn-ghost'} btn-sm"
               href="${ctx}/inventory/stocktakes">Tất cả</a>
            <a class="btn ${param.status == 'BAN_NHAP' ? 'btn-secondary' : 'btn-ghost'} btn-sm"
               href="${ctx}/inventory/stocktakes?status=BAN_NHAP">Bản nháp</a>
            <a class="btn ${param.status == 'DANG_KIEM_KE' ? 'btn-secondary' : 'btn-ghost'} btn-sm"
               href="${ctx}/inventory/stocktakes?status=DANG_KIEM_KE">Đang kiểm kê</a>
            <a class="btn ${param.status == 'HOAN_TAT' ? 'btn-secondary' : 'btn-ghost'} btn-sm"
               href="${ctx}/inventory/stocktakes?status=HOAN_TAT">Hoàn tất</a>
        </div>

        <div class="admin-card">
            <c:choose>
                <c:when test="${empty stocktakes}">
                    <div class="empty-state"><p>Không tìm thấy kiểm kê nào.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="admin-table admin-table-fixed">
                        <thead>
                        <tr>
                            <th>Mã Kiểm kê</th>
                            <th>Ngày tạo</th>
                            <th>Người tạo</th>
                            <th>Trạng thái</th>
                            <th class="col-num">Số mục</th>
                            <th class="col-num">Chênh lệch</th>
                            <th>Hoàn thành lúc</th>
                            <th>Thao tác</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="s" items="${stocktakes}">
                            <tr>
                                <td><strong>ST-<c:out value="${s.stocktakeId}"/></strong></td>
                                <td><fmt:formatDate value="${s.createdAt}" pattern="dd/MM/yyyy HH:mm"/></td>
                                <td><c:out value="${s.createdByName}"/></td>
                                <td>
                                    <span class="status-badge ${s.statusCss}">
                                        <c:out value="${s.statusLabel}"/>
                                    </span>
                                </td>
                                <td><c:out value="${s.itemCount}"/></td>
                                <td><c:out value="${s.differenceCount}"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${empty s.completedAt}">—</c:when>
                                        <c:otherwise><fmt:formatDate value="${s.completedAt}" pattern="dd/MM/yyyy HH:mm"/></c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <a class="btn btn-ghost btn-sm"
                                       href="${ctx}/inventory/stocktakes?action=detail&id=${s.stocktakeId}">
                                        <c:out value="${s.actionLabel}"/>
                                    </a>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>

                    <%-- Pagination --%>
                    <c:if test="${totalPages > 1}">
                        <nav class="pager" aria-label="Trang">
                            <c:forEach var="i" begin="1" end="${totalPages}">
                                <c:url var="pageUrl" value="/inventory/stocktakes">
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
