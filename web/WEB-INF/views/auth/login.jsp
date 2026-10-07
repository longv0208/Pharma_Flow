<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Đăng nhập — PharmaFlow</title>
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
            <h1>Chào mừng trở lại</h1>
            <p class="auth-sub">Đăng nhập để tiếp tục mua sắm hoặc quản lý đơn hàng của bạn.</p>

            <c:if test="${not empty error}">
                <div class="alert alert-error" role="alert"><c:out value="${error}"/></div>
            </c:if>
            <c:if test="${not empty success}">
                <div class="alert alert-success" role="alert"><c:out value="${success}"/></div>
            </c:if>

            <form class="auth-form" action="${ctx}/authen?action=login" method="post" data-disable-on-submit>
                <div class="form-field">
                    <label for="identifier">Email hoặc Tên đăng nhập</label>
                    <input type="text" id="identifier" name="identifier"
                           value="<c:out value='${identifierValue}'/>"
                           autocomplete="username" required>
                </div>

                <div class="form-field">
                    <label for="password">Mật khẩu</label>
                    <input type="password" id="password" name="password"
                           autocomplete="current-password" required>
                </div>

                <p class="auth-alt" style="text-align:right;margin:-4px 0 0">
                    <a href="${ctx}/authen?action=forgot-password">Quên mật khẩu?</a>
                </p>

                <button type="submit" class="btn btn-primary btn-block">Đăng nhập</button>
            </form>

            <p class="auth-alt">
                Chưa có tài khoản?
                <a href="${ctx}/authen?action=register">Tạo tài khoản</a>
            </p>
            <p class="auth-alt">
                <a href="${ctx}/home">← Về trang chủ</a>
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
                        btn.textContent = 'Đang đăng nhập…';
                    }
                });
            })();
        </script>
    </body>
</html>
