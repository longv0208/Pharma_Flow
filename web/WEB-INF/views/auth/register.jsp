<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Create Account — PharmaFlow</title>
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
            <h1>Create your account</h1>
            <p class="auth-sub">Join PharmaFlow to order medicines online and track your orders.</p>

            <c:if test="${not empty error}">
                <div class="alert alert-error" role="alert"><c:out value="${error}"/></div>
            </c:if>

            <form class="auth-form" action="${ctx}/authen?action=register" method="post" data-disable-on-submit>
                <div class="form-field">
                    <label for="fullName">Full Name</label>
                    <input type="text" id="fullName" name="fullName"
                           value="<c:out value='${fullNameValue}'/>" autocomplete="name" required>
                    <c:if test="${not empty errors.fullName}">
                        <span class="field-error"><c:out value="${errors.fullName}"/></span>
                    </c:if>
                </div>

                <div class="form-field">
                    <label for="email">Email</label>
                    <input type="email" id="email" name="email"
                           value="<c:out value='${emailValue}'/>" autocomplete="email" required>
                    <c:if test="${not empty errors.email}">
                        <span class="field-error"><c:out value="${errors.email}"/></span>
                    </c:if>
                </div>

                <div class="form-field">
                    <label for="phone">Phone Number</label>
                    <input type="tel" id="phone" name="phone"
                           value="<c:out value='${phoneValue}'/>" autocomplete="tel" required>
                    <c:if test="${not empty errors.phone}">
                        <span class="field-error"><c:out value="${errors.phone}"/></span>
                    </c:if>
                </div>

                <div class="form-field">
                    <label for="username">Username</label>
                    <input type="text" id="username" name="username"
                           value="<c:out value='${usernameValue}'/>" autocomplete="username" required>
                    <c:if test="${not empty errors.username}">
                        <span class="field-error"><c:out value="${errors.username}"/></span>
                    </c:if>
                </div>

                <div class="form-field">
                    <label for="password">Password</label>
                    <input type="password" id="password" name="password"
                           autocomplete="new-password" required minlength="6">
                    <c:if test="${not empty errors.password}">
                        <span class="field-error"><c:out value="${errors.password}"/></span>
                    </c:if>
                </div>

                <div class="form-field">
                    <label for="confirmPassword">Confirm Password</label>
                    <input type="password" id="confirmPassword" name="confirmPassword"
                           autocomplete="new-password" required minlength="6">
                    <c:if test="${not empty errors.confirmPassword}">
                        <span class="field-error"><c:out value="${errors.confirmPassword}"/></span>
                    </c:if>
                </div>

                <button type="submit" class="btn btn-primary btn-block">Create Account</button>
            </form>

            <p class="auth-alt">
                Already have an account?
                <a href="${ctx}/authen?action=login">Sign in</a>
            </p>
            <p class="auth-alt">
                <a href="${ctx}/home">← Back to home</a>
            </p>
        </div>

        <script>
            // Prevent duplicate submits — disable button while processing (rule.md §52)
            (function () {
                var form = document.querySelector('form[data-disable-on-submit]');
                if (!form)
                    return;
                form.addEventListener('submit', function () {
                    var btn = form.querySelector('button[type="submit"]');
                    if (btn) {
                        btn.disabled = true;
                        btn.textContent = 'Creating account…';
                    }
                });
            })();
        </script>
    </body>
</html>
