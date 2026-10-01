<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="categories"/>
<c:set var="isEdit" value="${mode == 'edit'}"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>${isEdit ? "Edit" : "New"} Category — Admin — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <div class="section-head">
            <div>
                <h2>${isEdit ? 'Edit Category' : 'New Category'}</h2>
                <p class="section-sub">
                    ${isEdit ? 'Update name, description and visibility.' : 'Create a catalog group shown on the storefront.'}
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
                    <label for="categoryName">Category Name <span class="req">*</span></label>
                    <input type="text" id="categoryName" name="categoryName" required maxlength="150"
                           value="<c:out value='${isEdit ? category.categoryName : categoryNameValue}'/>">
                    <c:if test="${not empty errors.categoryName}">
                        <span class="field-error"><c:out value="${errors.categoryName}"/></span>
                    </c:if>
                </div>

                <div class="form-field">
                    <label for="description">Description</label>
                    <input type="text" id="description" name="description" maxlength="500"
                           value="<c:out value='${isEdit ? category.description : descriptionValue}'/>"
                           placeholder="Short note shown to admins only">
                </div>

                <c:if test="${isEdit}">
                    <div class="form-field">
                        <label for="status">Status</label>
                        <select id="status" name="status">
                            <option value="ACTIVE"   ${category.status == 'ACTIVE'   ? 'selected' : ''}>ACTIVE — visible on storefront</option>
                            <option value="INACTIVE" ${category.status == 'INACTIVE' ? 'selected' : ''}>INACTIVE — hidden</option>
                        </select>
                    </div>
                </c:if>

                <div class="profile-actions">
                    <button type="submit" class="btn btn-primary">${isEdit ? 'Save Changes' : 'Create Category'}</button>
                    <a class="btn btn-ghost" href="${ctx}/admin?action=categories">Cancel</a>
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
            if (btn) { btn.disabled = true; btn.textContent = 'Saving…'; }
        });
    })();
</script>
</body>
</html>
