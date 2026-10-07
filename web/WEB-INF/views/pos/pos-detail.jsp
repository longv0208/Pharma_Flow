<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="pos"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Đơn bán #${sale.saleTransactionId} — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <nav class="crumbs" aria-label="Đường dẫn">
            <span>Bán hàng</span>
            <span aria-hidden="true">›</span>
            <a href="${ctx}/pos?action=history">Lịch sử</a>
            <span aria-hidden="true">›</span>
            <span>Đơn bán #<c:out value="${sale.saleTransactionId}"/></span>
        </nav>

        <div class="section-head">
            <div>
                <h2>Đơn bán #<c:out value="${sale.saleTransactionId}"/></h2>
                <p class="section-sub">Hóa đơn — đơn bán đã hoàn tất là dữ liệu kiểm toán chỉ đọc.</p>
            </div>
            <a class="btn btn-primary btn-sm" href="${ctx}/pos">Đơn bán mới</a>
        </div>

        <c:if test="${param.ok == 'completed'}">
            <div class="alert alert-success">Bán hàng hoàn tất — tồn kho đã được trừ theo phân bổ FEFO.</div>
        </c:if>

        <%-- ==================== header ==================== --%>
        <div class="admin-card">
            <div class="profile-grid">
                <div class="profile-group">
                    <span class="field-hint">Ngày / Giờ</span>
                    <strong><fmt:formatDate value="${sale.saleDatetime}" pattern="yyyy-MM-dd HH:mm"/></strong>
                </div>
                <div class="profile-group">
                    <span class="field-hint">Nhân viên</span>
                    <strong><c:out value="${sale.staffName}"/></strong>
                </div>
                <div class="profile-group">
                    <span class="field-hint">Thanh toán</span>
                    <strong><c:out value="${sale.paymentLabel}"/></strong>
                </div>
                <div class="profile-group">
                    <span class="field-hint">Trạng thái</span>
                    <strong><span class="status-badge ${sale.statusCss}"><c:out value="${sale.statusLabel}"/></span></strong>
                </div>
            </div>
        </div>

        <%-- ==================== prescription verification ==================== --%>
        <c:if test="${prescription != null}">
            <div class="admin-card">
                <h3 class="pos-card-title">Xác nhận Đơn thuốc</h3>
                <div class="profile-grid">
                    <div class="profile-group">
                        <span class="field-hint">Đơn thuốc</span>
                        <strong>Đã kiểm tra</strong>
                    </div>
                    <div class="profile-group">
                        <span class="field-hint">Bác sĩ kê đơn</span>
                        <strong><c:out value="${prescription.prescriber}"/></strong>
                    </div>
                    <div class="profile-group">
                        <span class="field-hint">Cơ sở y tế</span>
                        <strong><c:out value="${prescription.healthcareFacility}"/></strong>
                    </div>
                    <div class="profile-group">
                        <span class="field-hint">Người kiểm tra</span>
                        <strong><c:out value="${sale.staffName}"/></strong>
                    </div>
                    <div class="profile-group">
                        <span class="field-hint">Thời điểm kiểm tra</span>
                        <strong><fmt:formatDate value="${prescription.validatedAt}" pattern="yyyy-MM-dd HH:mm"/></strong>
                    </div>
                </div>
            </div>
        </c:if>

        <%-- ==================== lines ==================== --%>
        <div class="admin-card">
            <h3 class="pos-card-title">Sản phẩm</h3>
            <table class="admin-table">
                <thead>
                <tr>
                    <th>Sản phẩm</th>
                    <th>Loại</th>
                    <th class="col-num">Đơn giá</th>
                    <th class="col-num">SL</th>
                    <th class="col-num">Thành tiền</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="item" items="${items}">
                    <tr>
                        <td>
                            <c:out value="${item.productName}"/>
                            <div class="field-hint"><c:out value="${item.sku}"/> · <c:out value="${item.sellingUnit}"/></div>
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${item.productType == 'RX'}">
                                    <span class="type-badge type-rx">Rx</span>
                                </c:when>
                                <c:when test="${item.productType == 'RESTRICTED'}">
                                    <span class="type-badge type-restricted">Hạn chế</span>
                                </c:when>
                                <c:otherwise>
                                    <span class="type-badge type-otc">OTC</span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td class="col-num"><fmt:formatNumber value="${item.unitPrice}" type="number" groupingUsed="true"/></td>
                        <td class="col-num"><c:out value="${item.quantity}"/></td>
                        <td class="col-num"><fmt:formatNumber value="${item.subtotal}" type="number" groupingUsed="true"/></td>
                    </tr>
                </c:forEach>
                </tbody>
                <tfoot>
                <tr>
                    <td colspan="4" class="col-num"><strong>Tổng tiền</strong></td>
                    <td class="col-num"><strong><fmt:formatNumber value="${sale.totalAmount}" type="number" groupingUsed="true"/> ₫</strong></td>
                </tr>
                </tfoot>
            </table>
        </div>

        <%-- ==================== batch traceability ==================== --%>
        <div class="admin-card">
            <h3 class="pos-card-title">Phân bổ lô hàng</h3>
            <p class="field-hint">Các lô hàng mà sản phẩm được lấy ra — FEFO (hết hạn sớm nhất trước).</p>
            <c:choose>
                <c:when test="${empty allocations}">
                    <div class="empty-state"><p>Chưa có phân bổ nào được ghi nhận.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="admin-table">
                        <thead>
                        <tr>
                            <th>Sản phẩm</th>
                            <th>Lô hàng</th>
                            <th>Hạn dùng</th>
                            <th class="col-num">SL</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="a" items="${allocations}">
                            <tr>
                                <td><c:out value="${a.productName}"/></td>
                                <td>
                                    <a href="${ctx}/inventory?action=batch&id=${a.batchId}">
                                        <c:out value="${a.batchNumber}"/>
                                    </a>
                                </td>
                                <td><fmt:formatDate value="${a.expiryDate}" pattern="yyyy-MM-dd"/></td>
                                <td class="col-num"><c:out value="${a.quantity}"/></td>
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
