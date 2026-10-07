<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="inventory-adjustments"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Điều chỉnh Tồn kho — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <div class="section-head">
            <div>
                <h2>Điều chỉnh Tồn kho</h2>
                <p class="section-sub">Điều chỉnh thủ công tồn thực tế của lô — nhật ký kiểm toán.</p>
            </div>
        </div>

        <c:if test="${param.ok == 'created'}">
            <div class="alert alert-success" role="status">Đã ghi nhận điều chỉnh.</div>
        </c:if>
        <c:if test="${param.err == 'notfound'}">
            <div class="alert alert-error" role="alert">Không tìm thấy lô.</div>
        </c:if>

        <%-- Filter bar: GET keeps filters bookmarkable --%>
        <form class="filter-bar" method="get" action="${ctx}/inventory/adjustments">
            <input type="search" name="q" value="<c:out value='${param.q}'/>"
                   placeholder="Tên sản phẩm hoặc SKU…" aria-label="Tìm kiếm sản phẩm">
            <input type="search" name="batch" value="<c:out value='${param.batch}'/>"
                   placeholder="Số lô…" aria-label="Tìm kiếm lô">
            <select name="reason" aria-label="Lý do">
                <option value="">Tất cả lý do</option>
                <option value="HU_HONG"              ${param.reason == 'HU_HONG'              ? 'selected' : ''}>Hư hỏng</option>
                <option value="THAT_LAC"             ${param.reason == 'THAT_LAC'             ? 'selected' : ''}>Thất lạc</option>
                <option value="HET_HAN"              ${param.reason == 'HET_HAN'              ? 'selected' : ''}>Hết hạn</option>
                <option value="DIEU_CHINH_KIEM_DEM"  ${param.reason == 'DIEU_CHINH_KIEM_DEM'  ? 'selected' : ''}>Điều chỉnh kiểm đếm</option>
                <option value="DIEU_CHINH_DU_LIEU"   ${param.reason == 'DIEU_CHINH_DU_LIEU'   ? 'selected' : ''}>Điều chỉnh dữ liệu</option>
                <option value="KHAC"                 ${param.reason == 'KHAC'                 ? 'selected' : ''}>Khác</option>
            </select>
            <input type="date" name="from" value="<c:out value='${param.from}'/>" aria-label="Từ ngày">
            <input type="date" name="to" value="<c:out value='${param.to}'/>" aria-label="Đến ngày">
            <button type="submit" class="btn btn-secondary btn-sm">Lọc</button>
            <a href="${ctx}/inventory/adjustments" class="btn btn-ghost btn-sm">Đặt lại</a>
        </form>

        <div class="admin-card">
            <c:choose>
                <c:when test="${empty adjustments}">
                    <div class="empty-state"><p>Không tìm thấy điều chỉnh nào.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="admin-table admin-table-fixed">
                        <thead>
                        <tr>
                            <th>Ngày / Giờ</th>
                            <th>Sản phẩm</th>
                            <th>Lô</th>
                            <th>Lý do</th>
                            <th class="col-num">Thay đổi SL</th>
                            <th class="col-num">Trước</th>
                            <th class="col-num">Sau</th>
                            <th>Người thực hiện</th>
                            <th>Ghi chú</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="a" items="${adjustments}">
                            <tr>
                                <td><fmt:formatDate value="${a.createdAt}" pattern="dd/MM/yyyy HH:mm"/></td>
                                <td><c:out value="${a.productName}"/></td>
                                <td>
                                    <a href="${ctx}/inventory?action=batch&id=${a.batchId}">
                                        <c:out value="${a.batchNumber}"/>
                                    </a>
                                </td>
                                <td><span class="status-badge mv-adjustment"><c:out value="${a.reasonLabel}"/></span></td>
                                <td><strong><c:out value="${a.quantityChangeLabel}"/></strong></td>
                                <td><c:out value="${a.quantityBefore}"/></td>
                                <td><c:out value="${a.quantityAfter}"/></td>
                                <td><c:out value="${a.performedByName}"/></td>
                                <td><c:out value="${empty a.note ? '—' : a.note}"/></td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>

                    <%-- Pagination --%>
                    <c:if test="${totalPages > 1}">
                        <nav class="pager" aria-label="Trang">
                            <c:forEach var="i" begin="1" end="${totalPages}">
                                <c:url var="pageUrl" value="/inventory/adjustments">
                                    <c:param name="q" value="${param.q}"/>
                                    <c:param name="batch" value="${param.batch}"/>
                                    <c:param name="reason" value="${param.reason}"/>
                                    <c:param name="from" value="${param.from}"/>
                                    <c:param name="to" value="${param.to}"/>
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
