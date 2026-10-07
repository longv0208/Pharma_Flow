<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="inventory"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Lô <c:out value="${batch.batchNumber}"/> — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <nav class="crumbs" aria-label="Breadcrumb">
            <a href="${ctx}/inventory">Tồn kho</a>
            <span aria-hidden="true">›</span>
            <a href="${ctx}/inventory?action=product&id=${batch.productId}"><c:out value="${batch.productName}"/></a>
            <span aria-hidden="true">›</span>
            <span>Lô <c:out value="${batch.batchNumber}"/></span>
        </nav>

        <div class="section-head">
            <div>
                <h2>Lô <c:out value="${batch.batchNumber}"/></h2>
                <p class="section-sub">Chi tiết lô — truy xuất và kiểm soát trạng thái.</p>
            </div>
            <a class="btn btn-ghost" href="${ctx}/inventory?action=product&id=${batch.productId}">← Quay lại Sản phẩm</a>
        </div>

        <c:if test="${param.ok == 'blocked'}">
            <div class="alert alert-success" role="status">Đã khóa lô thành công.</div>
        </c:if>
        <c:if test="${param.ok == 'unblocked'}">
            <div class="alert alert-success" role="status">Đã mở khóa lô thành công.</div>
        </c:if>
        <c:if test="${param.err == 'reasonrequired'}">
            <div class="alert alert-error" role="alert">Vui lòng nhập lý do cho thao tác này.</div>
        </c:if>
        <c:if test="${param.err == 'alreadyblocked'}">
            <div class="alert alert-error" role="alert">Lô này đã bị khóa.</div>
        </c:if>
        <c:if test="${param.err == 'notblocked'}">
            <div class="alert alert-error" role="alert">Lô này hiện không bị khóa.</div>
        </c:if>
        <c:if test="${param.err == 'hasreserved'}">
            <div class="alert alert-error" role="alert">Lô này vẫn còn tồn đã đặt — hãy giải phóng các đặt trước khi khóa.</div>
        </c:if>
        <c:if test="${param.err == 'expiredbatch'}">
            <div class="alert alert-error" role="alert">Không thể mở khóa lô đã hết hạn — lô sẽ giữ trạng thái khóa.</div>
        </c:if>
        <c:if test="${param.err == 'expired'}">
            <div class="alert alert-error" role="alert">Không thể khóa lô đã hết hạn — lô đã không còn sử dụng được.</div>
        </c:if>
        <c:if test="${param.err == 'notfound'}">
            <div class="alert alert-error" role="alert">Không tìm thấy lô.</div>
        </c:if>
        <c:if test="${param.err == 'db'}">
            <div class="alert alert-error" role="alert">Lỗi cơ sở dữ liệu — thay đổi trạng thái chưa được lưu.</div>
        </c:if>

        <div class="profile-card">
            <%-- Basic information --%>
            <fieldset class="profile-group">
                <legend>Thông tin cơ bản</legend>
                <div class="profile-grid">
                    <div class="form-field">
                        <label>Tên sản phẩm</label>
                        <span><c:out value="${batch.productName}"/></span>
                    </div>
                    <div class="form-field">
                        <label>SKU</label>
                        <span><c:out value="${batch.sku}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Số lô</label>
                        <span><strong><c:out value="${batch.batchNumber}"/></strong></span>
                    </div>
                    <div class="form-field">
                        <label>Hạn dùng</label>
                        <span>
                            <c:out value="${batch.expiryDate}"/>
                            <c:if test="${expiryWarning == 'EXPIRED'}">
                                <span class="status-badge batch-expired">Đã hết hạn</span>
                            </c:if>
                            <c:if test="${expiryWarning == 'NEAR_EXPIRY'}">
                                <span class="status-badge batch-near-expiry">Sắp hết hạn</span>
                            </c:if>
                        </span>
                    </div>
                    <div class="form-field">
                        <label>Nhà cung cấp</label>
                        <span><c:out value="${batch.supplierName}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Phiếu nhập</label>
                        <span>
                            <c:choose>
                                <c:when test="${empty batch.sourceGoodsReceiptId}">—</c:when>
                                <c:otherwise>#<c:out value="${batch.sourceGoodsReceiptId}"/></c:otherwise>
                            </c:choose>
                        </span>
                    </div>
                    <div class="form-field">
                        <label>Đơn giá vốn</label>
                        <span><fmt:formatNumber value="${batch.costPrice}" type="number" maxFractionDigits="0"/>&#x20AB;</span>
                    </div>
                    <div class="form-field">
                        <label>Vị trí lưu trữ</label>
                        <span><c:out value="${empty batch.storageLocation ? '—' : batch.storageLocation}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Trạng thái lô</label>
                        <span class="status-badge batch-${batch.status.toLowerCase().replace('_','-')}">
                            <c:choose>
                                <c:when test="${batch.status == 'CO_SAN'}">Có sẵn</c:when>
                                <c:when test="${batch.status == 'SAP_HET_HAN'}">Sắp hết hạn</c:when>
                                <c:when test="${batch.status == 'HET_HAN'}">Hết hạn</c:when>
                                <c:when test="${batch.status == 'BI_KHOA'}">Bị khóa</c:when>
                                <c:when test="${batch.status == 'HET_HANG'}">Hết hàng</c:when>
                                <c:otherwise><c:out value="${batch.status}"/></c:otherwise>
                            </c:choose>
                        </span>
                    </div>
                </div>
            </fieldset>

            <%-- Quantity --%>
            <fieldset class="profile-group">
                <legend>Số lượng</legend>
                <div class="profile-grid">
                    <div class="form-field">
                        <label>Tồn thực tế</label>
                        <span><strong><c:out value="${batch.onHandQuantity}"/></strong></span>
                    </div>
                    <div class="form-field">
                        <label>Đã đặt</label>
                        <span><c:out value="${batch.reservedQuantity}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Khả dụng</label>
                        <span><strong><c:out value="${batch.onHandQuantity - batch.reservedQuantity}"/></strong></span>
                    </div>
                    <c:if test="${batch.reservedQuantity > 0}">
                        <div class="form-field">
                            <label>Lưu ý</label>
                            <span class="field-hint">Lô này có <c:out value="${batch.reservedQuantity}"/> đơn vị đã đặt. Hãy giải phóng đặt trước khi khóa lô này.</span>
                        </div>
                    </c:if>
                </div>
            </fieldset>

            <%-- Actions --%>
            <div class="profile-actions">
                <a class="btn btn-secondary" href="${ctx}/inventory?action=history&batchId=${batch.batchId}">
                    Xem Lịch sử Tồn kho
                </a>
                <a class="btn btn-secondary" href="${ctx}/inventory/adjustments?action=new&batchId=${batch.batchId}">
                    Điều chỉnh Tồn kho
                </a>

                <c:if test="${(batch.status == 'CO_SAN' or batch.status == 'SAP_HET_HAN' or batch.status == 'HET_HANG') and batch.reservedQuantity <= 0 and expiryWarning != 'EXPIRED'}">
                    <button type="button" class="btn btn-ghost btn-danger"
                            onclick="document.getElementById('blockForm').hidden = false;">
                        Khóa Lô
                    </button>
                </c:if>
                <c:if test="${batch.status == 'BI_KHOA'}">
                    <button type="button" class="btn btn-primary"
                            onclick="document.getElementById('unblockForm').hidden = false;">
                        Mở khóa Lô
                    </button>
                </c:if>
            </div>

            <%-- Block form (hidden until clicked) --%>
            <c:if test="${(batch.status == 'CO_SAN' or batch.status == 'SAP_HET_HAN' or batch.status == 'HET_HANG') and batch.reservedQuantity <= 0 and expiryWarning != 'EXPIRED'}">
                <form method="post" action="${ctx}/inventory?action=block-batch" id="blockForm" hidden>
                    <input type="hidden" name="batchId" value="${batch.batchId}">
                    <fieldset class="profile-group">
                        <legend>Khóa Lô</legend>
                        <div class="form-field">
                            <label for="reason">Lý do <span class="req">*</span></label>
                            <textarea id="reason" name="reason" rows="3" maxlength="500" required
                                      placeholder="VD: Bao bì hư hỏng, Vấn đề chất lượng, Thu hồi, Vấn đề lưu trữ, Điều tra, Khác"></textarea>
                        </div>
                        <div class="profile-actions">
                            <button type="submit" class="btn btn-danger">Xác nhận Khóa</button>
                            <button type="button" class="btn btn-ghost"
                                    onclick="document.getElementById('blockForm').hidden = true;">Hủy</button>
                        </div>
                    </fieldset>
                </form>
            </c:if>

            <%-- Unblock form (hidden until clicked) --%>
            <c:if test="${batch.status == 'BI_KHOA'}">
                <form method="post" action="${ctx}/inventory?action=unblock-batch" id="unblockForm" hidden>
                    <input type="hidden" name="batchId" value="${batch.batchId}">
                    <fieldset class="profile-group">
                        <legend>Mở khóa Lô</legend>
                        <div class="form-field">
                            <label for="unblockReason">Lý do <span class="req">*</span></label>
                            <textarea id="unblockReason" name="reason" rows="3" maxlength="500" required
                                      placeholder="VD: Đã xử lý vấn đề chất lượng, Đã hoàn tất kiểm tra, Thu hồi đã được xóa, Khóa nhầm"></textarea>
                        </div>
                        <div class="profile-actions">
                            <button type="submit" class="btn btn-primary">Xác nhận Mở khóa</button>
                            <button type="button" class="btn btn-ghost"
                                    onclick="document.getElementById('unblockForm').hidden = true;">Hủy</button>
                        </div>
                    </fieldset>
                </form>
            </c:if>
        </div>
    </main>
</div>

<%@ include file="/WEB-INF/jspf/admin-footer.jspf" %>

</body>
</html>
