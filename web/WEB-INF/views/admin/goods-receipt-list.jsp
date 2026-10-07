<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="stock-receiving"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Nhập kho — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <div class="section-head">
            <div>
                <h2>Nhập kho</h2>
                <p class="section-sub">Phiếu nhập theo đơn đặt hàng. Chỉ phiếu đã xác nhận mới cập nhật vào kho.</p>
            </div>
            <a class="btn btn-primary" href="${ctx}/inventory/receipts?action=new">+ Nhập thuốc</a>
        </div>

        <%-- Filter bar: GET keeps filters bookmarkable --%>
        <form class="filter-bar" method="get" action="${ctx}/inventory/receipts">
            <input type="search" name="q" value="<c:out value='${param.q}'/>"
                   placeholder="Tìm theo mã phiếu hoặc tên nhà cung cấp…" aria-label="Tìm kiếm phiếu nhập">
            <select name="status" aria-label="Trạng thái">
                <option value="">Tất cả trạng thái</option>
                <option value="BAN_NHAP"            ${param.status == 'BAN_NHAP'            ? 'selected' : ''}>Bản nháp</option>
                <option value="DA_XAC_NHAN"         ${param.status == 'DA_XAC_NHAN'         ? 'selected' : ''}>Đã xác nhận</option>
                <option value="CHAP_NHAN_MOT_PHAN"  ${param.status == 'CHAP_NHAN_MOT_PHAN'  ? 'selected' : ''}>Chấp nhận một phần</option>
                <option value="DA_HUY"              ${param.status == 'DA_HUY'              ? 'selected' : ''}>Đã hủy</option>
            </select>
            <button type="submit" class="btn btn-secondary btn-sm">Lọc</button>
        </form>

        <c:if test="${param.ok == 'created'}">
            <div class="alert alert-success" role="status">Đã lưu nháp phiếu nhập — chưa có gì vào kho.</div>
        </c:if>
        <c:if test="${param.ok == 'updated'}">
            <div class="alert alert-success" role="status">Đã cập nhật phiếu nhập nháp.</div>
        </c:if>
        <c:if test="${param.ok == 'cancelled'}">
            <div class="alert alert-success" role="status">Đã hủy phiếu nhập nháp.</div>
        </c:if>
        <c:if test="${param.err == 'notfound'}">
            <div class="alert alert-error" role="alert">Không tìm thấy phiếu nhập.</div>
        </c:if>
        <c:if test="${param.err == 'noteditable'}">
            <div class="alert alert-error" role="alert">Chỉ có phiếu nháp mới có thể sửa.</div>
        </c:if>
        <c:if test="${param.err == 'notcancellable'}">
            <div class="alert alert-error" role="alert">Phiếu nhập này không thể hủy.</div>
        </c:if>

        <div class="admin-card">
            <c:choose>
                <c:when test="${empty receipts}">
                    <div class="empty-state"><p>Chưa có phiếu nhập nào.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="admin-table admin-table-fixed table-goods-receipts">
                        <thead>
                        <tr>
                            <th class="col-id">Phiếu nhập</th>
                            <th class="col-po">Đơn hàng</th>
                            <th>Nhà cung cấp</th>
                            <th class="col-date">Ngày nhận</th>
                            <th class="col-invoice">Hóa đơn</th>
                            <th class="col-status">Trạng thái</th>
                            <th>Người nhận</th>
                            <th class="col-act">Thao tác</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="r" items="${receipts}">
                            <tr>
                                <td>#<c:out value="${r.goodsReceiptId}"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${empty r.purchaseOrderId}">—</c:when>
                                        <c:otherwise>#<c:out value="${r.purchaseOrderId}"/></c:otherwise>
                                    </c:choose>
                                </td>
                                <td><c:out value="${r.supplierName}"/></td>
                                <td><c:out value="${r.receiptDate}"/></td>
                                <td><c:out value="${empty r.invoiceNumber ? '—' : r.invoiceNumber}"/></td>
                                <td>
                                    <span class="status-badge ${r.statusCss}">
                                        <c:out value="${r.statusLabel}"/>
                                    </span>
                                </td>
                                <td><c:out value="${r.receivedByName}"/></td>
                                <td class="col-actions">
                                    <a class="btn btn-secondary btn-sm"
                                       href="${ctx}/inventory/receipts?action=detail&id=${r.goodsReceiptId}">Xem</a>
                                    <c:if test="${r.editable}">
                                        <a class="btn btn-ghost btn-sm"
                                           href="${ctx}/inventory/receipts?action=edit&id=${r.goodsReceiptId}">Sửa</a>
                                    </c:if>
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
