<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>New Password — PharmaFlow</title>
    <link rel="stylesheet" href="${ctx}/css/main.css">
</head>
<body class="auth-page">

<div class="auth-card">
    <a class="auth-brand" href="${ctx}/home">
        <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
            <rect x="3" y="8" width="18" height="13" rx="2"/>
            <path d="M8 8V5a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v3"/>
            <path d="M12 12v6M9 15h6"/>
        </svg>
        <span>PharmaFlow</span>
    </a>
    <h1>Choose a new password</h1>
    <p class="auth-sub">For <b><c:out value="${sessionScope.pendingResetEmail}"/></b></p>

    <c:if test="${not empty error}">
        <div class="alert alert-error" role="alert"><c:out value="${error}"/></div>
    </c:if>

    <form class="auth-form" action="${ctx}/authen?action=reset-password" method="post" data-disable-on-submit>
        <div class="form-field">
            <label for="password">New Password</label>
            <input type="password" id="password" name="password"
                   autocomplete="new-password" required minlength="6">
            <c:if test="${not empty errors.password}">
                <span class="field-error"><c:out value="${errors.password}"/></span>
            </c:if>
        </div>

        <div class="form-field">
            <label for="confirmPassword">Confirm New Password</label>
            <input type="password" id="confirmPassword" name="confirmPassword"
                   autocomplete="new-password" required minlength="6">
            <c:if test="${not empty errors.confirmPassword}">
                <span class="field-error"><c:out value="${errors.confirmPassword}"/></span>
            </c:if>
        </div>

        <button type="submit" class="btn btn-primary btn-block">Update Password</button>
    </form>

    <p class="auth-alt">
        <a href="${ctx}/authen?action=login">← Back to sign in</a>
    </p>
</div>

<script>
    (function () {
        var form = document.querySelector('form[data-disable-on-submit]');
        if (!form) return;
        form.addEventListener('submit', function () {
            var btn = form.querySelector('button[type="submit"]');
            if (btn) { btn.disabled = true; btn.textContent = 'Updating…'; }
        });
    })();
</script>
</body>
</html>
