<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="products"/>
<c:set var="isEdit" value="${mode == 'edit'}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>${isEdit ? "Sửa" : "Thêm"} sản phẩm — Quản trị — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <div class="section-head">
            <div>
                <h2>${isEdit ? 'Sửa sản phẩm' : 'Thêm sản phẩm'}</h2>
                <p class="section-sub">
                    ${isEdit ? 'Cập nhật thông tin danh mục, giá bán và hiển thị.' : 'Thêm sản phẩm vào danh mục. Tồn kho được quản lý qua đơn nhập hàng / lô hàng.'}
                </p>
            </div>
        </div>

        <div class="profile-card">
            <form class="profile-form" method="post" data-disable-on-submit
                  action="${ctx}/admin?action=${isEdit ? 'product-update' : 'product-create'}">

                <c:if test="${isEdit}">
                    <input type="hidden" name="productId" value="${product.productId}">
                </c:if>

                <fieldset class="profile-group">
                    <legend>Thông tin định danh</legend>

                    <div class="form-field">
                        <label for="productName">Tên sản phẩm <span class="req">*</span></label>
                        <input type="text" id="productName" name="productName" required maxlength="200"
                               value="<c:out value='${product.productName}'/>">
                        <c:if test="${not empty errors.productName}">
                            <span class="field-error"><c:out value="${errors.productName}"/></span>
                        </c:if>
                    </div>

                    <div class="profile-grid">
                        <div class="form-field">
                            <label for="sku">Mã SKU <span class="req">*</span></label>
                            <input type="text" id="sku" name="sku" required maxlength="100"
                                   value="<c:out value='${product.sku}'/>">
                            <c:if test="${not empty errors.sku}">
                                <span class="field-error"><c:out value="${errors.sku}"/></span>
                            </c:if>
                        </div>

                        <div class="form-field">
                            <label for="barcode">Mã vạch</label>
                            <input type="text" id="barcode" name="barcode" maxlength="100"
                                   value="<c:out value='${product.barcode}'/>">
                            <c:if test="${not empty errors.barcode}">
                                <span class="field-error"><c:out value="${errors.barcode}"/></span>
                            </c:if>
                        </div>

                        <div class="form-field">
                            <label for="categoryId">Danh mục <span class="req">*</span></label>
                            <select id="categoryId" name="categoryId" required>
                                <option value="">— Chọn —</option>
                                <c:forEach var="c" items="${categories}">
                                    <option value="${c.categoryId}"
                                            ${product.categoryId == c.categoryId ? 'selected' : ''}>
                                        <c:out value="${c.categoryName}"/>
                                    </option>
                                </c:forEach>
                            </select>
                            <c:if test="${not empty errors.categoryId}">
                                <span class="field-error"><c:out value="${errors.categoryId}"/></span>
                            </c:if>
                        </div>
                    </div>
                </fieldset>

                <fieldset class="profile-group">
                    <legend>Thông tin y tế</legend>

                    <div class="profile-grid">
                        <div class="form-field">
                            <label for="activeIngredient">Hoạt chất</label>
                            <input type="text" id="activeIngredient" name="activeIngredient" maxlength="255"
                                   value="<c:out value='${product.activeIngredient}'/>">
                        </div>
                        <div class="form-field">
                            <label for="strength">Hàm lượng</label>
                            <input type="text" id="strength" name="strength" maxlength="100"
                                   value="<c:out value='${product.strength}'/>" placeholder="500mg, 10%…">
                        </div>
                        <div class="form-field">
                            <label for="dosageForm">Dạng bào chế</label>
                            <input type="text" id="dosageForm" name="dosageForm" maxlength="100"
                                   value="<c:out value='${product.dosageForm}'/>" placeholder="Viên nén, siro…">
                        </div>
                    </div>

                    <div class="profile-grid">
                        <div class="form-field">
                            <label for="manufacturer">Nhà sản xuất</label>
                            <input type="text" id="manufacturer" name="manufacturer" maxlength="200"
                                   value="<c:out value='${product.manufacturer}'/>">
                        </div>
                        <div class="form-field">
                            <label for="registrationNumber">Số đăng ký</label>
                            <input type="text" id="registrationNumber" name="registrationNumber" maxlength="100"
                                   value="<c:out value='${product.registrationNumber}'/>">
                        </div>
                        <div class="form-field">
                            <label for="productType">Loại sản phẩm <span class="req">*</span></label>
                            <select id="productType" name="productType" required>
                                <option value="OTC"        ${product.productType.name() == 'OTC'        ? 'selected' : ''}>OTC — không kê đơn</option>
                                <option value="RX"         ${product.productType.name() == 'RX'         ? 'selected' : ''}>RX — cần đơn thuốc</option>
                                <option value="RESTRICTED" ${product.productType.name() == 'RESTRICTED' ? 'selected' : ''}>HẠN CHẾ</option>
                            </select>
                        </div>
                    </div>
                </fieldset>

                <fieldset class="profile-group">
                    <legend>Thông tin thuốc</legend>

                    <div class="form-field">
                        <label for="shortDescription">Mô tả ngắn</label>
                        <textarea id="shortDescription" name="shortDescription" rows="2" maxlength="500"
                                  placeholder="Tổng quan ngắn hiển thị dưới tên sản phẩm"><c:out value="${product.shortDescription}"/></textarea>
                        <c:if test="${not empty errors.shortDescription}">
                            <span class="field-error"><c:out value="${errors.shortDescription}"/></span>
                        </c:if>
                        <span class="field-hint">Hiển thị dưới tên sản phẩm trên trang chi tiết. Tối đa 500 ký tự.</span>
                    </div>

                    <div class="form-field">
                        <label for="indication">Chỉ định</label>
                        <textarea id="indication" name="indication" rows="4"
                                  placeholder="Thuốc được dùng để điều trị gì"><c:out value="${product.indication}"/></textarea>
                    </div>

                    <div class="form-field">
                        <label for="usageInstruction">Cách dùng</label>
                        <textarea id="usageInstruction" name="usageInstruction" rows="4"
                                  placeholder="Liều dùng, thời điểm, cách sử dụng"><c:out value="${product.usageInstruction}"/></textarea>
                    </div>

                    <div class="form-field">
                        <label for="warnings">Cảnh báo &amp; Thận trọng</label>
                        <textarea id="warnings" name="warnings" rows="4"
                                  placeholder="Các lưu ý quan trọng"><c:out value="${product.warnings}"/></textarea>
                    </div>

                    <div class="form-field">
                        <label for="contraindications">Chống chỉ định</label>
                        <textarea id="contraindications" name="contraindications" rows="4"
                                  placeholder="Các trường hợp không được dùng thuốc"><c:out value="${product.contraindications}"/></textarea>
                    </div>
                </fieldset>

                <fieldset class="profile-group">
                    <legend>Bán hàng</legend>

                    <div class="profile-grid">
                        <div class="form-field">
                            <label for="sellingPrice">Giá bán <span class="req">*</span></label>
                            <input type="number" id="sellingPrice" name="sellingPrice" required
                                   min="0" step="1"
                                   value="<c:out value='${product.sellingPrice}'/>">
                            <c:if test="${not empty errors.sellingPrice}">
                                <span class="field-error"><c:out value="${errors.sellingPrice}"/></span>
                            </c:if>
                        </div>
                        <div class="form-field">
                            <label for="sellingUnit">Đơn vị bán <span class="req">*</span></label>
                            <input type="text" id="sellingUnit" name="sellingUnit" required maxlength="100"
                                   value="<c:out value='${product.sellingUnit}'/>" placeholder="hộp, chai, vỉ…">
                            <c:if test="${not empty errors.sellingUnit}">
                                <span class="field-error"><c:out value="${errors.sellingUnit}"/></span>
                            </c:if>
                        </div>
                        <div class="form-field">
                            <label class="check-label" for="onlineSaleAllowed">
                                <input type="checkbox" id="onlineSaleAllowed" name="onlineSaleAllowed" value="1"
                                       ${product.onlineSaleAllowed ? 'checked' : ''}>
                                <span>Cho phép bán online</span>
                            </label>
                            <span class="field-hint">Chỉ các sản phẩm OTC + cho phép bán online + còn hàng mới hiển thị có thể mua trên cửa hàng.</span>
                        </div>
                    </div>
                </fieldset>

                <c:if test="${isEdit}">
                    <div class="form-field">
                        <label for="status">Trạng thái</label>
                        <select id="status" name="status">
                            <option value="ACTIVE"   ${product.status == 'ACTIVE'   ? 'selected' : ''}>ĐANG BÁN — hiển thị trên cửa hàng</option>
                            <option value="INACTIVE" ${product.status == 'INACTIVE' ? 'selected' : ''}>NGỪNG BÁN — ẩn</option>
                        </select>
                    </div>
                    <c:if test="${not empty product.availableQuantity}">
                        <p class="field-hint">Tồn kho có thể bán: <c:out value="${product.availableQuantity}"/> (quản lý qua lô hàng, không chỉnh sửa ở đây).</p>
                    </c:if>
                </c:if>

                <div class="profile-actions">
                    <button type="submit" class="btn btn-primary">${isEdit ? 'Lưu thay đổi' : 'Tạo sản phẩm'}</button>
                    <a class="btn btn-ghost" href="${ctx}/admin?action=products">Hủy</a>
                </div>
            </form>
        </div>
    </main>
</div>

<%@ include file="/WEB-INF/jspf/admin-footer.jspf" %>

<script>
    // Prevent duplicate submits (same pattern as other admin forms)
    (function () {
        var form = document.querySelector('form[data-disable-on-submit]');
        if (!form) return;
        form.addEventListener('submit', function () {
            var btn = form.querySelector('button[type="submit"]');
            if (btn) { btn.disabled = true; btn.textContent = 'Đang lưu…'; }
        });
    })();
</script>
</body>
</html>
