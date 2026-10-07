<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quên mật khẩu — PharmaFlow</title>
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
    <h1>Quên mật khẩu</h1>
    <p class="auth-sub">Nhập email của tài khoản và chúng tôi sẽ gửi mã đặt lại (có hiệu lực trong 15 phút).</p>

    <c:if test="${not empty error}">
        <div class="alert alert-error" role="alert"><c:out value="${error}"/></div>
    </c:if>

    <form class="auth-form" action="${ctx}/authen?action=forgot-password" method="post" data-disable-on-submit>
        <div class="form-field">
            <label for="email">Email</label>
            <input type="email" id="email" name="email" required autocomplete="email"
                   value="<c:out value='${param.email}'/>">
        </div>

        <button type="submit" class="btn btn-primary btn-block">Gửi mã đặt lại</button>
    </form>

    <p class="auth-alt">
        Đã nhớ ra? <a href="${ctx}/authen?action=login">Đăng nhập</a>
    </p>
</div>

<script>
    (function () {
        var form = document.querySelector('form[data-disable-on-submit]');
        if (!form) return;
        form.addEventListener('submit', function () {
            var btn = form.querySelector('button[type="submit"]');
            if (btn) { btn.disabled = true; btn.textContent = 'Đang gửi…'; }
        });
    })();
</script>
</body>
</html>
