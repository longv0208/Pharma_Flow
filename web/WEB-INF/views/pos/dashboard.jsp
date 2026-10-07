<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="pos"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Bán hàng — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <nav class="crumbs" aria-label="Đường dẫn">
            <span>Bán hàng</span>
            <span aria-hidden="true">›</span>
            <span>Bán tại quầy</span>
        </nav>

        <div class="section-head">
            <div>
                <h2>Bán hàng</h2>
                <p class="section-sub">Bán tại quầy — tìm sản phẩm, thêm vào giỏ, thu tiền.</p>
            </div>
            <a class="btn btn-ghost btn-sm" href="${ctx}/pos?action=history">Lịch sử bán hàng</a>
        </div>

        <c:if test="${not empty param.err}">
            <div class="alert alert-error">
                <c:choose>
                    <c:when test="${param.err == 'EMPTY_CART'}">Giỏ hàng trống — hãy thêm sản phẩm trước.</c:when>
                    <c:when test="${param.err == 'STAFF_PROFILE_MISSING'}">Tài khoản của bạn chưa có hồ sơ nhân viên — yêu cầu quản trị viên liên kết trước khi bán hàng.</c:when>
                    <c:when test="${param.err == 'INVALID_PAYMENT_METHOD'}">Vui lòng chọn phương thức thanh toán hợp lệ.</c:when>
                    <c:when test="${param.err == 'PRODUCT_NOT_FOUND'}">Không tìm thấy sản phẩm.</c:when>
                    <c:when test="${param.err == 'PRODUCT_INACTIVE'}">Sản phẩm này không còn hoạt động.</c:when>
                    <c:when test="${param.err == 'INSUFFICIENT_STOCK'}">Không đủ tồn kho có thể bán.</c:when>
                    <c:when test="${param.err == 'PRESCRIPTION_DETAILS_REQUIRED'}">Đơn hàng này có thuốc Rx — vui lòng nhập bác sĩ kê đơn và cơ sở y tế (tối đa 200 ký tự).</c:when>
                    <c:when test="${param.err == 'PRESCRIPTION_CONFIRMATION_REQUIRED'}">Vui lòng xác nhận bạn đã kiểm tra đơn thuốc hợp lệ trước khi thanh toán.</c:when>
                    <c:when test="${param.err == 'RESTRICTED_NOT_ALLOWED'}">Sản phẩm bị hạn chế không thể bán tại quầy.</c:when>
                    <c:when test="${param.err == 'INVALID_TOKEN'}">Lần thanh toán này đã được gửi — giỏ hàng không thay đổi.</c:when>
                    <c:otherwise>Có lỗi xảy ra — vui lòng thử lại.</c:otherwise>
                </c:choose>
                <c:if test="${not empty param.msg}"> <c:out value="${param.msg}"/></c:if>
            </div>
        </c:if>
        <c:if test="${not empty param.ok}">
            <div class="alert alert-success">
                <c:choose>
                    <c:when test="${param.ok == 'added'}">Đã thêm vào giỏ hàng.</c:when>
                    <c:when test="${param.ok == 'updated'}">Đã cập nhật số lượng.</c:when>
                    <c:when test="${param.ok == 'removed'}">Đã xóa khỏi giỏ hàng.</c:when>
                    <c:when test="${param.ok == 'cleared'}">Đã xóa giỏ hàng.</c:when>
                    <c:otherwise>Hoàn tất.</c:otherwise>
                </c:choose>
            </div>
        </c:if>

        <%-- ==================== product search ==================== --%>
        <div class="admin-card">
            <form class="filter-bar" method="get" action="${ctx}/pos">
                <input type="search" name="q" value="<c:out value='${q}'/>"
                       placeholder="Tên sản phẩm, SKU hoặc mã vạch…" aria-label="Tìm sản phẩm"
                       autofocus>
                <button type="submit" class="btn btn-secondary btn-sm">Tìm kiếm</button>
                <c:if test="${not empty q}">
                    <a href="${ctx}/pos" class="btn btn-ghost btn-sm">Xóa</a>
                </c:if>
            </form>

            <c:if test="${not empty q}">
                <c:choose>
                    <c:when test="${empty results}">
                        <div class="empty-state"><p>Không có sản phẩm hoạt động nào khớp với “<c:out value='${q}'/>”.</p></div>
                    </c:when>
                    <c:otherwise>
                        <table class="admin-table">
                            <thead>
                            <tr>
                                <th>Sản phẩm</th>
                                <th>SKU</th>
                                <th>Loại</th>
                                <th class="col-num">Đơn giá</th>
                                <th class="col-num">Có thể bán</th>
                                <th></th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:forEach var="p" items="${results}">
                                <tr>
                                    <td><c:out value="${p.productName}"/></td>
                                    <td><c:out value="${p.sku}"/></td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${p.productType == 'KE_DON'}">
                                                <span class="type-badge type-rx">Rx</span>
                                            </c:when>
                                            <c:when test="${p.productType == 'HAN_CHE'}">
                                                <span class="type-badge type-restricted">Hạn chế</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="type-badge type-otc">OTC</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td class="col-num"><fmt:formatNumber value="${p.sellingPrice}" type="number" groupingUsed="true"/></td>
                                    <td class="col-num"><c:out value="${p.availableQuantity}"/></td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${p.productType == 'HAN_CHE'}">
                                                <span class="field-hint">Không bán được</span>
                                            </c:when>
                                            <c:when test="${p.availableQuantity <= 0}">
                                                <span class="field-hint">Hết hàng</span>
                                            </c:when>
                                            <c:otherwise>
                                                <form method="post" action="${ctx}/pos?action=add" class="pos-inline-form">
                                                    <input type="hidden" name="productId" value="${p.productId}">
                                                    <input type="hidden" name="q" value="<c:out value='${q}'/>">
                                                    <input type="number" name="qty" value="1" min="1"
                                                           max="${p.availableQuantity}" class="pos-qty-input"
                                                           aria-label="Số lượng">
                                                    <button type="submit" class="btn btn-primary btn-sm">Thêm</button>
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
            </c:if>
        </div>

        <%-- ==================== cart ==================== --%>
        <div class="admin-card">
            <h3 class="pos-card-title">Giỏ hàng</h3>
            <c:choose>
                <c:when test="${empty cartLines}">
                    <div class="empty-state"><p>Giỏ hàng trống — tìm sản phẩm ở trên để bắt đầu bán.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="admin-table">
                        <thead>
                        <tr>
                            <th>Sản phẩm</th>
                            <th>Loại</th>
                            <th class="col-num">Đơn giá</th>
                            <th class="col-num">SL</th>
                            <th class="col-num">Thành tiền</th>
                            <th></th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="line" items="${cartLines}">
                            <tr>
                                <td>
                                    <c:out value="${line.productName}"/>
                                    <div class="field-hint"><c:out value="${line.sku}"/> · <c:out value="${line.sellingUnit}"/></div>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${line.productType == 'KE_DON'}">
                                            <span class="type-badge type-rx">Rx</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="type-badge type-otc">OTC</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="col-num"><fmt:formatNumber value="${line.unitPrice}" type="number" groupingUsed="true"/></td>
                                <td class="col-num">
                                    <form method="post" action="${ctx}/pos?action=update-qty" class="pos-inline-form">
                                        <input type="hidden" name="productId" value="${line.productId}">
                                        <input type="number" name="qty" value="${line.quantity}" min="0"
                                               max="${line.saleableQuantity}" class="pos-qty-input"
                                               aria-label="Số lượng cho ${line.productName}">
                                        <button type="submit" class="btn btn-ghost btn-sm">Đặt</button>
                                    </form>
                                    <c:if test="${line.saleableQuantity != null and line.quantity > line.saleableQuantity}">
                                        <div class="field-hint">Chỉ còn <c:out value="${line.saleableQuantity}"/> có thể bán</div>
                                    </c:if>
                                </td>
                                <td class="col-num"><fmt:formatNumber value="${line.subtotal}" type="number" groupingUsed="true"/></td>
                                <td>
                                    <form method="post" action="${ctx}/pos?action=remove" class="pos-inline-form">
                                        <input type="hidden" name="productId" value="${line.productId}">
                                        <button type="submit" class="btn btn-ghost btn-sm">Xóa</button>
                                    </form>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </c:otherwise>
            </c:choose>
        </div>

        <%-- ==================== checkout ==================== --%>
        <%-- clear uses a separate form referenced by id — HTML forbids nested forms --%>
        <form id="pos-clear-form" method="post" action="${ctx}/pos?action=clear"></form>
        <div class="admin-card">
            <h3 class="pos-card-title">Thanh toán</h3>
            <form id="pos-checkout-form" method="post" action="${ctx}/pos?action=checkout"
                  data-disable-on-submit>
                <input type="hidden" name="checkoutToken" value="<c:out value='${checkoutToken}'/>">
                <c:if test="${cartHasRx}">
                    <fieldset class="profile-group pos-rx-group">
                        <legend>Kiểm tra Đơn thuốc</legend>
                        <p class="field-hint">Giỏ hàng này có thuốc Rx — đơn thuốc được kiểm tra thủ công bởi nhân viên.</p>
                        <div class="profile-grid">
                            <div class="form-field">
                                <label for="prescriber">Bác sĩ kê đơn <span class="req">*</span></label>
                                <input type="text" id="prescriber" name="prescriber"
                                       maxlength="200" required>
                            </div>
                            <div class="form-field">
                                <label for="healthcareFacility">Cơ sở y tế <span class="req">*</span></label>
                                <input type="text" id="healthcareFacility" name="healthcareFacility"
                                       maxlength="200" required>
                            </div>
                        </div>
                        <label class="pos-rx-confirm">
                            <input type="checkbox" name="prescriptionChecked" required>
                            Tôi đã kiểm tra đơn thuốc hợp lệ.
                        </label>
                    </fieldset>
                </c:if>
                <div class="pos-payment-row">
                    <c:forEach var="m" items="${paymentMethods}">
                        <label class="pos-payment-option">
                            <input type="radio" name="paymentMethod" value="${m}"
                                   ${m == 'TIEN_MAT' ? 'checked' : ''}>
                            <c:choose>
                                <c:when test="${m == 'TIEN_MAT'}">Tiền mặt</c:when>
                                <c:when test="${m == 'CHUYEN_KHOAN'}">Chuyển khoản</c:when>
                                <c:otherwise>Thẻ</c:otherwise>
                            </c:choose>
                        </label>
                    </c:forEach>
                </div>
                <div class="pos-total-row">
                    <span>Tổng tiền</span>
                    <strong class="pos-total"><fmt:formatNumber value="${cartTotal}" type="number" groupingUsed="true"/> ₫</strong>
                </div>
                <div class="profile-actions">
                    <button type="submit" class="btn btn-primary"
                            ${empty cartLines ? 'disabled' : ''}>
                        Hoàn tất bán hàng
                    </button>
                    <button type="submit" form="pos-clear-form" class="btn btn-ghost">Xóa giỏ hàng</button>
                </div>
            </form>
        </div>
    </main>
</div>

<script>
    // Prevent duplicate submits — disable the submit button while processing.
    // (The session checkout token is the real guard; this is just the UI half.)
    var form = document.querySelector('form[data-disable-on-submit]');
    if (form) {
        form.addEventListener('submit', function () {
            var btn = form.querySelector('button[type="submit"]');
            if (btn) { btn.disabled = true; btn.textContent = 'Đang xử lý…'; }
        });
    }
</script>

<%@ include file="/WEB-INF/jspf/admin-footer.jspf" %>

</body>
</html>
