<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="inventory-history"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Lịch sử Tồn kho — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <nav class="crumbs" aria-label="Breadcrumb">
            <a href="${ctx}/inventory">Tồn kho</a>
            <span aria-hidden="true">›</span>
            <span>Lịch sử</span>
        </nav>

        <div class="section-head">
            <div>
                <h2>Lịch sử Tồn kho</h2>
                <p class="section-sub">Nhật ký kiểm toán đầy đủ theo thời gian của các biến động kho.</p>
            </div>
        </div>

        <%-- Filter bar --%>
        <form class="filter-bar" method="get" action="${ctx}/inventory">
            <input type="hidden" name="action" value="history">
            <input type="search" name="q" value="<c:out value='${param.q}'/>"
                   placeholder="Tên sản phẩm hoặc SKU…" aria-label="Tìm kiếm sản phẩm">
            <input type="search" name="batch" value="<c:out value='${param.batch}'/>"
                   placeholder="Số lô…" aria-label="Tìm kiếm lô">
            <select name="type" aria-label="Loại biến động">
                <option value="">Tất cả loại</option>
                <option value="NHAP_KHO"               ${param.type == 'NHAP_KHO'               ? 'selected' : ''}>Nhập kho</option>
                <option value="BAN_TAI_QUAY"           ${param.type == 'BAN_TAI_QUAY'           ? 'selected' : ''}>Bán tại quầy</option>
                <option value="GIU_HANG_ONLINE"        ${param.type == 'GIU_HANG_ONLINE'        ? 'selected' : ''}>Giữ hàng online</option>
                <option value="GIAI_PHONG_GIU_HANG"    ${param.type == 'GIAI_PHONG_GIU_HANG'    ? 'selected' : ''}>Giải phóng giữ hàng</option>
                <option value="BAN_ONLINE"             ${param.type == 'BAN_ONLINE'             ? 'selected' : ''}>Bán online</option>
                <option value="DIEU_CHINH"             ${param.type == 'DIEU_CHINH'             ? 'selected' : ''}>Điều chỉnh</option>
                <option value="DIEU_CHINH_KIEM_KE"     ${param.type == 'DIEU_CHINH_KIEM_KE'     ? 'selected' : ''}>Điều chỉnh kiểm kê</option>
                <option value="KHOA"                   ${param.type == 'KHOA'                   ? 'selected' : ''}>Khóa</option>
                <option value="MO_KHOA"                ${param.type == 'MO_KHOA'                ? 'selected' : ''}>Mở khóa</option>
            </select>
            <input type="date" name="from" value="<c:out value='${param.from}'/>" aria-label="Từ ngày">
            <input type="date" name="to" value="<c:out value='${param.to}'/>" aria-label="Đến ngày">
            <button type="submit" class="btn btn-secondary btn-sm">Lọc</button>
            <a href="${ctx}/inventory?action=history" class="btn btn-ghost btn-sm">Đặt lại</a>
        </form>

        <div class="admin-card">
            <c:choose>
                <c:when test="${empty movements}">
                    <div class="empty-state"><p>Không tìm thấy biến động nào.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="admin-table admin-table-fixed">
                        <thead>
                        <tr>
                            <th>Ngày / Giờ</th>
                            <th>Sản phẩm</th>
                            <th>Lô</th>
                            <th>Loại biến động</th>
                            <th class="col-num">Thay đổi Tồn thực tế</th>
                            <th class="col-num">Thay đổi Đã đặt</th>
                            <th class="col-num">Tồn thực tế Trước</th>
                            <th class="col-num">Tồn thực tế Sau</th>
                            <th class="col-num">Đã đặt Trước</th>
                            <th class="col-num">Đã đặt Sau</th>
                            <th>Tham chiếu</th>
                            <th>Người thực hiện</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="m" items="${movements}">
                            <tr>
                                <td><fmt:formatDate value="${m.createdAt}" pattern="yyyy-MM-dd HH:mm"/></td>
                                <td><c:out value="${m.productName}"/></td>
                                <td><c:out value="${m.batchNumber}"/></td>
                                <td>
                                    <span class="status-badge ${m.movementCss}">
                                        <c:out value="${m.movementLabel}"/>
                                    </span>
                                </td>
                                <td><c:out value="${m.onHandChangeLabel}"/></td>
                                <td><c:out value="${m.reservedChangeLabel}"/></td>
                                <td><c:out value="${m.onHandBefore}"/></td>
                                <td><c:out value="${m.onHandAfter}"/></td>
                                <td><c:out value="${m.reservedBefore}"/></td>
                                <td><c:out value="${m.reservedAfter}"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${m.referenceType == 'PHIEU_NHAP_KHO'}">
                                            <a href="${ctx}/inventory/receipts?action=detail&id=${m.referenceId}">
                                                Phiếu nhập #<c:out value="${m.referenceId}"/>
                                            </a>
                                        </c:when>
                                        <c:when test="${m.referenceType == 'KIEM_KE'}">
                                            <a href="${ctx}/inventory/stocktakes?action=detail&id=${m.referenceId}">
                                                Kiểm kê #<c:out value="${m.referenceId}"/>
                                            </a>
                                        </c:when>
                                        <c:when test="${m.referenceType == 'BAN_TAI_QUAY'}">
                                            <a href="${ctx}/pos?action=detail&id=${m.referenceId}">
                                                Bán POS #<c:out value="${m.referenceId}"/>
                                            </a>
                                        </c:when>
                                        <c:when test="${m.referenceType == 'DON_HANG_ONLINE'}">
                                            <a href="${ctx}/fulfillment?action=detail&id=${m.referenceId}">
                                                Đơn online #<c:out value="${m.referenceId}"/>
                                            </a>
                                        </c:when>
                                        <c:otherwise>
                                            <c:out value="${m.referenceType}"/> #<c:out value="${m.referenceId}"/>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td><c:out value="${m.performedByName}"/></td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>

                    <%-- Pagination --%>
                    <c:if test="${totalPages > 1}">
                        <nav class="pager" aria-label="Trang">
                            <c:forEach var="i" begin="1" end="${totalPages}">
                                <c:url var="pageUrl" value="/inventory">
                                    <c:param name="action" value="history"/>
                                    <c:param name="q" value="${param.q}"/>
                                    <c:param name="batch" value="${param.batch}"/>
                                    <c:param name="type" value="${param.type}"/>
                                    <c:param name="productId" value="${param.productId}"/>
                                    <c:param name="batchId" value="${param.batchId}"/>
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

