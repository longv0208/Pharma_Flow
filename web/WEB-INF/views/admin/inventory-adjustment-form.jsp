<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="inventory-adjustments"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Điều chỉnh Mới — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <nav class="crumbs" aria-label="Breadcrumb">
            <a href="${ctx}/inventory">Tồn kho</a>
            <span aria-hidden="true">›</span>
            <a href="${ctx}/inventory?action=batch&id=${batch.batchId}">Lô <c:out value="${batch.batchNumber}"/></a>
            <span aria-hidden="true">›</span>
            <span>Điều chỉnh Mới</span>
        </nav>

        <div class="section-head">
            <div>
                <h2>Điều chỉnh Tồn kho Mới</h2>
                <p class="section-sub">Điều chỉnh thủ công số lượng tồn thực tế của lô này.</p>
            </div>
            <a class="btn btn-ghost" href="${ctx}/inventory?action=batch&id=${batch.batchId}">← Quay lại Chi tiết Lô</a>
        </div>

        <%-- Error messages --%>
        <c:if test="${param.err == 'zerochange'}">
            <div class="alert alert-error" role="alert">Thay đổi số lượng không thể bằng 0.</div>
        </c:if>
        <c:if test="${param.err == 'badquantity'}">
            <div class="alert alert-error" role="alert">Thay đổi số lượng phải là số nguyên.</div>
        </c:if>
        <c:if test="${param.err == 'negativestock'}">
            <div class="alert alert-error" role="alert">Điều chỉnh sẽ làm tồn thực tế xuống dưới 0.</div>
        </c:if>
        <c:if test="${param.err == 'belowreserved'}">
            <div class="alert alert-error" role="alert">Điều chỉnh sẽ làm tồn kho xuống dưới số lượng đã đặt hiện tại.</div>
        </c:if>
        <c:if test="${param.err == 'invalidreason'}">
            <div class="alert alert-error" role="alert">Vui lòng chọn lý do hợp lệ.</div>
        </c:if>
        <c:if test="${param.err == 'noterequired'}">
            <div class="alert alert-error" role="alert">Bắt buộc nhập ghi chú khi lý do là Khác.</div>
        </c:if>
        <c:if test="${param.err == 'notfound'}">
            <div class="alert alert-error" role="alert">Không tìm thấy lô.</div>
        </c:if>
        <c:if test="${param.err == 'db'}">
            <div class="alert alert-error" role="alert">Lỗi cơ sở dữ liệu — điều chỉnh chưa được lưu.</div>
        </c:if>

        <div class="profile-card">
            <%-- Batch information (read-only) --%>
            <fieldset class="profile-group">
                <legend>Thông tin Lô</legend>
                <div class="profile-grid">
                    <div class="form-field">
                        <label>Sản phẩm</label>
                        <span><c:out value="${batch.productName}"/></span>
                    </div>
                    <div class="form-field">
                        <label>SKU</label>
                        <span><c:out value="${batch.sku}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Lô</label>
                        <span><strong><c:out value="${batch.batchNumber}"/></strong></span>
                    </div>
                    <div class="form-field">
                        <label>Hạn dùng</label>
                        <span><c:out value="${batch.expiryDate}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Trạng thái</label>
                        <span class="status-badge batch-${batch.status.toLowerCase().replace('_','-')}">
                            <c:out value="${batch.status}"/>
                        </span>
                    </div>
                </div>
            </fieldset>

            <%-- Current inventory (read-only) --%>
            <fieldset class="profile-group">
                <legend>Tồn kho Hiện tại</legend>
                <div class="profile-grid">
                    <div class="form-field">
                        <label>Tồn thực tế</label>
                        <span><strong id="curOnHand"><c:out value="${batch.onHandQuantity}"/></strong></span>
                    </div>
                    <div class="form-field">
                        <label>Đã đặt</label>
                        <span id="curReserved"><c:out value="${batch.reservedQuantity}"/></span>
                    </div>
                    <div class="form-field">
                        <label>Khả dụng</label>
                        <span><c:out value="${batch.onHandQuantity - batch.reservedQuantity}"/></span>
                    </div>
                </div>
            </fieldset>

            <form method="post" action="${ctx}/inventory/adjustments?action=create" id="adjustForm">
                <input type="hidden" name="batchId" value="${batch.batchId}">

                <fieldset class="profile-group">
                    <legend>Điều chỉnh</legend>
                    <div class="form-field">
                        <label for="quantityChange">Thay đổi Số lượng <span class="req">*</span></label>
                        <input type="number" id="quantityChange" name="quantityChange" required step="1"
                               placeholder="VD: -5 hoặc +3" aria-describedby="qtyHint">
                        <span class="field-hint" id="qtyHint">Số âm trừ tồn, số dương cộng tồn. Không được bằng 0.</span>
                    </div>
                    <div class="form-field">
                        <label for="reason">Lý do <span class="req">*</span></label>
                        <select id="reason" name="reason" required>
                            <option value="">— Chọn lý do —</option>
                            <option value="HU_HONG">Hư hỏng</option>
                            <option value="THAT_LAC">Thất lạc</option>
                            <option value="HET_HAN">Hết hạn</option>
                            <option value="DIEU_CHINH_KIEM_DEM">Điều chỉnh kiểm đếm</option>
                            <option value="DIEU_CHINH_DU_LIEU">Điều chỉnh dữ liệu</option>
                            <option value="KHAC">Khác</option>
                        </select>
                    </div>
                    <div class="form-field">
                        <label for="note">Ghi chú</label>
                        <textarea id="note" name="note" rows="3" maxlength="500"
                                  placeholder="Bắt buộc khi lý do là Khác. VD: Phát hiện hộp hư hỏng khi kiểm tra."></textarea>
                    </div>
                </fieldset>

                <%-- Live preview — UX only, backend recomputes everything --%>
                <fieldset class="profile-group">
                    <legend>Xem trước Điều chỉnh</legend>
                    <div class="profile-grid">
                        <div class="form-field">
                            <label>Tồn thực tế Hiện tại</label>
                            <span><c:out value="${batch.onHandQuantity}"/></span>
                        </div>
                        <div class="form-field">
                            <label>Thay đổi Số lượng</label>
                            <span id="pvChange">—</span>
                        </div>
                        <div class="form-field">
                            <label>Tồn thực tế Mới</label>
                            <span><strong id="pvAfter">—</strong></span>
                        </div>
                        <div class="form-field">
                            <label>Đã đặt</label>
                            <span><c:out value="${batch.reservedQuantity}"/></span>
                        </div>
                        <div class="form-field">
                            <label>Khả dụng Sau điều chỉnh</label>
                            <span><strong id="pvAvail">—</strong></span>
                        </div>
                    </div>
                    <span class="field-hint" id="pvWarn" style="display:none;color:#b91c1c;"></span>
                </fieldset>

                <div class="profile-actions">
                    <a class="btn btn-ghost" href="${ctx}/inventory?action=batch&id=${batch.batchId}">Hủy</a>
                    <button type="submit" class="btn btn-primary">Xác nhận Điều chỉnh</button>
                </div>
            </form>
        </div>
    </main>
</div>

<%@ include file="/WEB-INF/jspf/admin-footer.jspf" %>

<script>
(function () {
    var onHand = parseInt(document.getElementById('curOnHand').textContent, 10) || 0;
    var reserved = parseInt(document.getElementById('curReserved').textContent, 10) || 0;
    var qty = document.getElementById('quantityChange');
    var pvChange = document.getElementById('pvChange');
    var pvAfter = document.getElementById('pvAfter');
    var pvAvail = document.getElementById('pvAvail');
    var pvWarn = document.getElementById('pvWarn');

    function update() {
        var v = parseInt(qty.value, 10);
        if (isNaN(v)) {
            pvChange.textContent = '—';
            pvAfter.textContent = '—';
            pvAvail.textContent = '—';
            pvWarn.style.display = 'none';
            return;
        }
        var after = onHand + v;
        var avail = after - reserved;
        pvChange.textContent = (v > 0 ? '+' : '') + v;
        pvAfter.textContent = after;
        pvAvail.textContent = avail;
        var msg = '';
        if (v === 0) {
            msg = 'Thay đổi số lượng không thể bằng 0.';
        } else if (after < 0) {
            msg = 'Điều chỉnh này sẽ làm tồn thực tế xuống dưới 0.';
        } else if (after < reserved) {
            msg = 'Điều chỉnh này sẽ làm tồn kho xuống dưới số lượng đã đặt (' + reserved + ').';
        }
        if (msg) {
            pvWarn.textContent = msg;
            pvWarn.style.display = 'inline';
        } else {
            pvWarn.style.display = 'none';
        }
    }
    qty.addEventListener('input', update);
})();
</script>

</body>
</html>
