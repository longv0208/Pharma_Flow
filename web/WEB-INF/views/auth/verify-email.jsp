<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Xác thực email — PharmaFlow</title>
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
    <h1>Kiểm tra email của bạn</h1>
    <p class="auth-sub">
        Chúng tôi đã gửi mã xác thực 6 số đến
        <b><c:out value="${sessionScope.pendingVerifyEmail}"/></b>.
        Nhập mã bên dưới để kích hoạt tài khoản. Mã có hiệu lực trong 15 phút.
    </p>

    <c:if test="${not empty error}">
        <div class="alert alert-error" role="alert"><c:out value="${error}"/></div>
    </c:if>
    <c:if test="${not empty success}">
        <div class="alert alert-success" role="status"><c:out value="${success}"/></div>
    </c:if>
    <c:if test="${param.sent == '1'}">
        <div class="alert alert-success" role="status">Đã gửi mã xác thực — vui lòng kiểm tra hộp thư.</div>
    </c:if>
    <c:if test="${param.pending == '1'}">
        <div class="alert alert-error" role="alert">Tài khoản của bạn chưa được xác thực. Vui lòng nhập mã chúng tôi đã gửi qua email.</div>
    </c:if>

    <form class="auth-form" action="${ctx}/authen?action=verify-email" method="post" data-disable-on-submit>
        <div class="form-field">
            <label for="code">Mã xác thực</label>
            <input type="text" id="code" name="code" inputmode="numeric"
                   pattern="[0-9]{6}" maxlength="6" required
                   autocomplete="one-time-code" placeholder="Mã 6 số">
        </div>

        <button type="submit" class="btn btn-primary btn-block">Xác thực &amp; Kích hoạt</button>
    </form>

    <form class="auth-form" action="${ctx}/authen?action=resend-code" method="post">
        <button type="submit" class="btn btn-ghost btn-block" id="resendBtn"
                <c:if test="${not empty resendCooldown and resendCooldown > 0}">disabled</c:if>>
            Gửi lại mã<c:if test="${not empty resendCooldown and resendCooldown > 0}"> (<span id="resendTimer"><c:out value="${resendCooldown}"/></span>s)</c:if>
        </button>
    </form>

    <p class="auth-alt">
        <a href="${ctx}/authen?action=login">← Quay lại đăng nhập</a>
    </p>
</div>

<script>
    (function () {
        var form = document.querySelector('form[data-disable-on-submit]');
        if (form) {
            form.addEventListener('submit', function () {
                var btn = form.querySelector('button[type="submit"]');
                if (btn) { btn.disabled = true; btn.textContent = 'Đang xác thực…'; }
            });
        }
        // Resend cooldown countdown — mirrors the 60s server-side limit.
        var btn = document.getElementById('resendBtn');
        var timer = document.getElementById('resendTimer');
        if (btn && timer) {
            var left = parseInt(timer.textContent, 10);
            var iv = setInterval(function () {
                left--;
                if (left <= 0) {
                    clearInterval(iv);
                    btn.disabled = false;
                    btn.textContent = 'Gửi lại mã';
                } else {
                    timer.textContent = left;
                }
            }, 1000);
        }
    })();
</script>
</body>
</html>
