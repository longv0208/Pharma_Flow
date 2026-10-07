<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="products"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Sản phẩm — Quản trị — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <div class="section-head">
            <div>
                <h2>Quản lý sản phẩm</h2>
                <p class="section-sub">${total} sản phẩm. Sản phẩm ngừng bán vẫn giữ trong lịch sử đơn hàng nhưng bị ẩn khỏi cửa hàng.</p>
            </div>
            <a class="btn btn-primary" href="${ctx}/admin?action=product-new">+ Thêm sản phẩm</a>
        </div>

        <%-- Filter bar: GET keeps filters bookmarkable --%>
        <form class="filter-bar" method="get" action="${ctx}/admin">
            <input type="hidden" name="action" value="products">
            <input type="search" name="q" value="<c:out value='${param.q}'/>"
                   placeholder="Tìm theo tên, SKU, mã vạch…" aria-label="Tìm kiếm sản phẩm">
            <select name="categoryId" aria-label="Danh mục">
                <option value="">Tất cả danh mục</option>
                <c:forEach var="c" items="${categories}">
                    <option value="${c.categoryId}" ${param.categoryId == c.categoryId ? 'selected' : ''}>
                        <c:out value="${c.categoryName}"/>
                    </option>
                </c:forEach>
            </select>
            <select name="type" aria-label="Loại sản phẩm">
                <option value="">Tất cả loại</option>
                <option value="KHONG_KE_DON" ${param.type == 'KHONG_KE_DON' ? 'selected' : ''}>Không kê đơn</option>
                <option value="KE_DON"       ${param.type == 'KE_DON'       ? 'selected' : ''}>Kê đơn</option>
                <option value="HAN_CHE"      ${param.type == 'HAN_CHE'      ? 'selected' : ''}>Hạn chế</option>
            </select>
            <select name="status" aria-label="Trạng thái">
                <option value="">Tất cả trạng thái</option>
                <option value="HOAT_DONG"       ${param.status == 'HOAT_DONG'       ? 'selected' : ''}>Đang bán</option>
                <option value="NGUNG_HOAT_DONG" ${param.status == 'NGUNG_HOAT_DONG' ? 'selected' : ''}>Ngừng bán</option>
            </select>
            <button type="submit" class="btn btn-secondary btn-sm">Lọc</button>
        </form>

        <c:if test="${param.ok == 'created'}">
            <div class="alert alert-success" role="status">Đã tạo sản phẩm.</div>
        </c:if>
        <c:if test="${param.ok == 'updated'}">
            <div class="alert alert-success" role="status">Đã cập nhật sản phẩm.</div>
        </c:if>
        <c:if test="${param.ok == 'deactivated'}">
            <div class="alert alert-success" role="status">Đã ngừng bán sản phẩm.</div>
        </c:if>
        <c:if test="${param.ok == 'activated'}">
            <div class="alert alert-success" role="status">Đã kích hoạt sản phẩm.</div>
        </c:if>
        <c:if test="${param.err == 'notfound'}">
            <div class="alert alert-error" role="alert">Không tìm thấy sản phẩm.</div>
        </c:if>

        <div class="admin-card">
            <c:choose>
                <c:when test="${empty products}">
                    <div class="empty-state"><p>Không có sản phẩm phù hợp.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="admin-table admin-table-fixed table-products">
                        <thead>
                        <tr>
                            <th class="col-id">ID</th>
                            <th>Tên</th>
                            <th class="col-cat">Danh mục</th>
                            <th class="col-sku">SKU</th>
                            <th class="col-type">Loại</th>
                            <th class="col-price">Giá</th>
                            <th class="col-num">Tồn kho</th>
                            <th class="col-num">Online</th>
                            <th class="col-status">Trạng thái</th>
                            <th class="col-act">Thao tác</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="p" items="${products}">
                            <tr>
                                <td><c:out value="${p.productId}"/></td>
                                <td>
                                    <c:out value="${p.productName}"/>
                                    <c:if test="${not empty p.strength}">
                                        <span class="field-hint"> · <c:out value="${p.strength}"/></span>
                                    </c:if>
                                </td>
                                <td class="col-desc"><c:out value="${p.categoryName}"/></td>
                                <td><c:out value="${p.sku}"/></td>
                                <td>
                                    <span class="type-badge type-${p.productType.name().toLowerCase()}">
                                        <c:out value="${p.productType}"/>
                                    </span>
                                </td>
                                <td>
                                    <fmt:formatNumber value="${p.sellingPrice}" type="number" maxFractionDigits="0"/>&#x20AB;
                                    <span class="field-hint">/ <c:out value="${p.sellingUnit}"/></span>
                                </td>
                                <td><c:out value="${p.availableQuantity}"/></td>
                                <td>${p.onlineSaleAllowed ? 'Có' : 'Không'}</td>
                                <td>
                                    <span class="status-badge status-${p.status == 'HOAT_DONG' ? 'active' : 'inactive'}">
                                        <c:out value="${p.status == 'HOAT_DONG' ? 'Đang bán' : 'Ngừng bán'}"/>
                                    </span>
                                </td>
                                <td class="col-actions">
                                    <a class="btn btn-secondary btn-sm"
                                       href="${ctx}/admin?action=product-edit&id=${p.productId}">Sửa</a>
                                    <c:choose>
    <c:when test="${p.status == 'HOAT_DONG'}">
        <form method="post" action="${ctx}/admin?action=product-delete" class="inline-form"
                                              onsubmit="return confirm('Ngừng bán sản phẩm này? Lịch sử đơn hàng và lô hàng được giữ lại.');">
                                            <input type="hidden" name="productId" value="${p.productId}">
                                            <button type="submit" class="btn btn-ghost btn-sm btn-danger">Ngừng bán</button>
                                        </form>
    </c:when>
    <c:otherwise>
        <form method="post" action="${ctx}/admin?action=product-activate" class="inline-form">
            <input type="hidden" name="productId" value="${p.productId}">
            <button type="submit" class="btn btn-ghost btn-sm btn-success">Kích hoạt</button>
        </form>
    </c:otherwise>
</c:choose>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>

                    <%-- Pagination --%>
                    <c:if test="${pages > 1}">
                        <nav class="pager" aria-label="Trang">
                            <c:forEach var="i" begin="1" end="${pages}">
                                <c:url var="pageUrl" value="/admin">
                                    <c:param name="action" value="products"/>
                                    <c:param name="q" value="${param.q}"/>
                                    <c:param name="categoryId" value="${param.categoryId}"/>
                                    <c:param name="type" value="${param.type}"/>
                                    <c:param name="status" value="${param.status}"/>
                                    <c:param name="page" value="${i}"/>
                                </c:url>
                                <a class="pager-num ${i == page ? 'current' : ''}" href="${pageUrl}">${i}</a>
                            </c:forEach>
                        </nav>
                    </c:if>
                </c:otherwise>
            </c:choose>
        </div>
    </main>
</div>

<%@ include file="/WEB-INF/jspf/admin-footer.jspf" %>

</body>
</html>
