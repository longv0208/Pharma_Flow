<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="categories"/>
<c:set var="isEdit" value="${mode == 'edit'}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>${isEdit ? "Sửa" : "Thêm"} danh mục — Quản trị — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <div class="section-head">
            <div>
                <h2>${isEdit ? 'Sửa danh mục' : 'Thêm danh mục'}</h2>
                <p class="section-sub">
                    ${isEdit ? 'Cập nhật tên, mô tả và hiển thị.' : 'Tạo nhóm danh mục hiển thị trên cửa hàng.'}
                </p>
            </div>
        </div>

        <div class="profile-card">
            <form class="profile-form" method="post" data-disable-on-submit
                  action="${ctx}/admin?action=${isEdit ? 'category-update' : 'category-create'}">

                <c:if test="${isEdit}">
                    <input type="hidden" name="categoryId" value="${category.categoryId}">
                </c:if>

                <div class="form-field">
                    <label for="categoryName">Tên danh mục <span class="req">*</span></label>
                    <input type="text" id="categoryName" name="categoryName" required maxlength="150"
                           value="<c:out value='${isEdit ? category.categoryName : categoryNameValue}'/>">
                    <c:if test="${not empty errors.categoryName}">
                        <span class="field-error"><c:out value="${errors.categoryName}"/></span>
                    </c:if>
                </div>

                <div class="form-field">
                    <label for="description">Mô tả</label>
                    <input type="text" id="description" name="description" maxlength="500"
                           value="<c:out value='${isEdit ? category.description : descriptionValue}'/>"
                           placeholder="Ghi chú ngắn chỉ hiển thị cho quản trị viên">
                </div>

                <c:if test="${isEdit}">
                    <div class="form-field">
                        <label for="status">Trạng thái</label>
                        <select id="status" name="status">
                            <option value="ACTIVE"   ${category.status == 'ACTIVE'   ? 'selected' : ''}>ĐANG BÁN — hiển thị trên cửa hàng</option>
                            <option value="INACTIVE" ${category.status == 'INACTIVE' ? 'selected' : ''}>NGỪNG BÁN — ẩn</option>
                        </select>
                    </div>
                </c:if>

                <div class="profile-actions">
                    <button type="submit" class="btn btn-primary">${isEdit ? 'Lưu thay đổi' : 'Tạo danh mục'}</button>
                    <a class="btn btn-ghost" href="${ctx}/admin?action=categories">Hủy</a>
                </div>
            </form>
        </div>
    </main>
</div>

<%@ include file="/WEB-INF/jspf/admin-footer.jspf" %>

<script>
    // Prevent duplicate submits (same pattern as register.jsp / profile.jsp)
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
