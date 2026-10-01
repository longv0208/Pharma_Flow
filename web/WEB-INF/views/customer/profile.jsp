<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>My Profile — PharmaFlow</title>
    <link rel="stylesheet" href="${ctx}/css/main.css">
</head>
<body>

<%@ include file="/WEB-INF/jspf/header.jspf" %>

<section class="section">
    <div class="container profile-wrap">
        <div class="section-head">
            <div>
                <h2>My Profile</h2>
                <p class="section-sub">Manage your personal details and default delivery address.</p>
            </div>
        </div>

        <c:if test="${not empty error}">
            <div class="alert alert-error" role="alert"><c:out value="${error}"/></div>
        </c:if>
        <c:if test="${not empty success}">
            <div class="alert alert-success" role="status"><c:out value="${success}"/></div>
        </c:if>

        <div class="profile-card">
            <form class="profile-form" action="${ctx}/profile" method="post" data-disable-on-submit>

                <fieldset class="profile-group">
                    <legend>Account</legend>

                    <div class="form-field">
                        <label for="email">Email</label>
                        <input type="email" id="email" name="email"
                               value="<c:out value='${sessionScope.currentUser.email}'/>"
                               readonly disabled aria-readonly="true">
                        <span class="field-hint">Email is your sign-in identity and cannot be changed here.</span>
                    </div>

                    <div class="form-field">
                        <label for="fullName">Full Name</label>
                        <input type="text" id="fullName" name="fullName"
                               value="<c:out value='${sessionScope.currentUser.fullName}'/>"
                               autocomplete="name" required>
                        <c:if test="${not empty errors.fullName}">
                            <span class="field-error"><c:out value="${errors.fullName}"/></span>
                        </c:if>
                    </div>

                    <div class="form-field">
                        <label for="phone">Phone Number</label>
                        <input type="tel" id="phone" name="phone"
                               value="<c:out value='${sessionScope.currentUser.phone}'/>"
                               autocomplete="tel" required>
                        <c:if test="${not empty errors.phone}">
                            <span class="field-error"><c:out value="${errors.phone}"/></span>
                        </c:if>
                    </div>
                </fieldset>

                <fieldset class="profile-group">
                    <legend>Delivery Address</legend>

                    <div class="profile-grid">
                        <div class="form-field">
                            <label for="provinceCity">Province / City</label>
                            <input type="text" id="provinceCity" name="provinceCity"
                                   value="<c:out value='${profile.provinceCity}'/>" autocomplete="address-level1">
                        </div>

                        <div class="form-field">
                            <label for="district">District</label>
                            <input type="text" id="district" name="district"
                                   value="<c:out value='${profile.district}'/>" autocomplete="address-level2">
                        </div>

                        <div class="form-field">
                            <label for="ward">Ward</label>
                            <input type="text" id="ward" name="ward"
                                   value="<c:out value='${profile.ward}'/>" autocomplete="address-level3">
                        </div>
                    </div>

                    <div class="form-field">
                        <label for="detailedAddress">Detailed Address</label>
                        <input type="text" id="detailedAddress" name="detailedAddress"
                               value="<c:out value='${profile.detailedAddress}'/>"
                               autocomplete="street-address"
                               placeholder="House number, street…">
                    </div>
                </fieldset>

                <div class="profile-actions">
                    <button type="submit" class="btn btn-primary">Save Changes</button>
                    <a class="btn btn-ghost" href="${ctx}/home">Cancel</a>
                </div>
            </form>
        </div>
    </div>
</section>

<%@ include file="/WEB-INF/jspf/footer.jspf" %>

<script>
    // Prevent duplicate submits (same pattern as register.jsp)
    (function () {
        var form = document.querySelector('form[data-disable-on-submit]');
        if (!form) return;
        form.addEventListener('submit', function () {
            var btn = form.querySelector('button[type="submit"]');
            if (btn) {
                btn.disabled = true;
                btn.textContent = 'Saving…';
            }
        });
    })();
</script>
</body>
</html>
