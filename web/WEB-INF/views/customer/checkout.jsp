<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đặt hàng — PharmaFlow</title>
    <link rel="stylesheet" href="${ctx}/css/main.css">
</head>
<body>

<%@ include file="/WEB-INF/jspf/header.jspf" %>

<section class="section">
    <div class="container">
        <div class="section-head">
            <div>
                <h2>Xác nhận đặt hàng</h2>
                <p class="section-sub">Kiểm tra đơn hàng và nhập thông tin giao hàng.</p>
            </div>
        </div>

        <c:if test="${not empty param.err}">
            <div class="alert alert-error" role="alert">
                <c:choose>
                    <c:when test="${param.err == 'name'}">Vui lòng nhập họ tên người nhận (tối đa 150 ký tự).</c:when>
                    <c:when test="${param.err == 'phone'}">Vui lòng nhập số điện thoại (tối đa 30 ký tự).</c:when>
                    <c:when test="${param.err == 'province'}">Vui lòng nhập Tỉnh / Thành phố (tối đa 100 ký tự).</c:when>
                    <c:when test="${param.err == 'district'}">Vui lòng nhập Quận / Huyện (tối đa 100 ký tự).</c:when>
                    <c:when test="${param.err == 'ward'}">Vui lòng nhập Phường / Xã (tối đa 100 ký tự).</c:when>
                    <c:when test="${param.err == 'address'}">Vui lòng nhập địa chỉ chi tiết (tối đa 255 ký tự).</c:when>
                    <c:otherwise>Thông tin giao hàng chưa đầy đủ, vui lòng kiểm tra lại.</c:otherwise>
                </c:choose>
            </div>
        </c:if>

        <div class="profile-card">
            <table class="admin-table">
                <thead>
                <tr>
                    <th>Sản phẩm</th>
                    <th>Đơn vị</th>
                    <th class="col-num">Đơn giá</th>
                    <th class="col-num">Số lượng</th>
                    <th class="col-num">Thành tiền</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="item" items="${items}">
                    <tr>
                        <td><c:out value="${item.productName}"/></td>
                        <td><c:out value="${item.sellingUnit}"/></td>
                        <td class="col-num">
                            <fmt:formatNumber value="${item.sellingPrice}" type="number" groupingUsed="true"/>
                        </td>
                        <td class="col-num"><c:out value="${item.quantity}"/></td>
                        <td class="col-num">
                            <fmt:formatNumber value="${item.subtotal}" type="number" groupingUsed="true"/>&#x20AB;
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>

            <div class="pos-total-row">
                <span>Tổng cộng</span>
                <span class="pos-total">
                    <fmt:formatNumber value="${cartTotal}" type="number" groupingUsed="true"/>&#x20AB;
                </span>
            </div>
        </div>

        <div class="profile-card" style="margin-top:24px">
            <form class="profile-form" method="post" action="${ctx}/orders?action=place"
                  data-disable-on-submit>
                <input type="hidden" name="checkoutToken" value="${checkoutToken}">

                <fieldset class="profile-group">
                    <legend>Thông tin giao hàng</legend>

                    <div class="profile-grid">
                        <div class="form-field">
                            <label for="customerName">Họ tên người nhận</label>
                            <input type="text" id="customerName" name="customerName"
                                   value="<c:out value='${sessionScope.currentUser.fullName}'/>"
                                   maxlength="150" autocomplete="name" required>
                        </div>
                        <div class="form-field">
                            <label for="customerPhone">Số điện thoại</label>
                            <input type="tel" id="customerPhone" name="customerPhone"
                                   value="<c:out value='${sessionScope.currentUser.phone}'/>"
                                   maxlength="30" autocomplete="tel" required>
                        </div>
                    </div>

                    <div class="profile-grid">
                        <div class="form-field">
                            <label for="provinceCity">Tỉnh / Thành phố</label>
                            <input type="text" id="provinceCity" name="provinceCity"
                                   value="<c:out value='${profile.provinceCity}'/>"
                                   maxlength="100" autocomplete="address-level1" required>
                        </div>
                        <div class="form-field">
                            <label for="district">Quận / Huyện</label>
                            <input type="text" id="district" name="district"
                                   value="<c:out value='${profile.district}'/>"
                                   maxlength="100" autocomplete="address-level2" required>
                        </div>
                        <div class="form-field">
                            <label for="ward">Phường / Xã</label>
                            <input type="text" id="ward" name="ward"
                                   value="<c:out value='${profile.ward}'/>"
                                   maxlength="100" autocomplete="address-level3" required>
                        </div>
                    </div>

                    <div class="form-field">
                        <label for="detailedAddress">Địa chỉ chi tiết</label>
                        <input type="text" id="detailedAddress" name="detailedAddress"
                               value="<c:out value='${profile.detailedAddress}'/>"
                               maxlength="255" autocomplete="street-address"
                               placeholder="Số nhà, đường…" required>
                    </div>
                </fieldset>

                <fieldset class="profile-group">
                    <legend>Thanh toán</legend>
                    <p class="field-hint">Thanh toán khi nhận hàng (COD). Bạn sẽ thanh toán
                        trực tiếp cho nhân viên giao hàng khi nhận được sản phẩm.</p>
                </fieldset>

                <div class="profile-actions">
                    <button type="submit" class="btn btn-primary btn-lg">Đặt hàng</button>
                    <a class="btn btn-ghost" href="${ctx}/cart">Quay lại giỏ hàng</a>
                </div>
            </form>
        </div>
    </div>
</section>

<%@ include file="/WEB-INF/jspf/footer.jspf" %>

<script>
    // Prevent duplicate submits (same pattern as profile.jsp)
    (function () {
        var form = document.querySelector('form[data-disable-on-submit]');
        if (!form) return;
        form.addEventListener('submit', function () {
            var btn = form.querySelector('button[type="submit"]');
            if (btn) {
                btn.disabled = true;
                btn.textContent = 'Đang đặt hàng…';
            }
        });
    })();
</script>
</body>
</html>
