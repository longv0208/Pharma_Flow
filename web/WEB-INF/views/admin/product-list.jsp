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

        <div class="admin-card catalog-card">
            <form class="filter-bar catalog-filters" method="get" action="${ctx}/admin">
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

            <c:choose>
                <c:when test="${empty products}">
                    <div class="empty-state"><p>Không có sản phẩm phù hợp.</p></div>
                </c:when>
                <c:otherwise>
                    <div class="catalog-scroll">
                    <table class="admin-table product-catalog">
                        <thead>
                        <tr>
                            <th>Sản phẩm</th>
                            <th>Danh mục</th>
                            <th>Loại</th>
                            <th class="num">Giá</th>
                            <th class="num">Tồn</th>
                            <th>Online</th>
                            <th>Trạng thái</th>
                            <th class="act">Thao tác</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="p" items="${products}">
                            <tr>
                                <td>
                                    <div class="pc-name">
                                        <span class="pc-title"><c:out value="${p.productName}"/></span>
                                        <c:if test="${not empty p.strength}">
                                            <span class="pc-strength"><c:out value="${p.strength}"/></span>
                                        </c:if>
                                    </div>
                                    <c:set var="pcBarcode" value="${empty p.barcode ? '—' : p.barcode}"/>
                                    <div class="pc-meta" title="<c:out value='SKU ${p.sku} · ${pcBarcode}'/>">
                                        <span>#<c:out value="${p.productId}"/></span>
                                        <span><c:out value="${p.sku}"/></span>
                                        <span><c:out value="${empty p.barcode ? '—' : p.barcode}"/></span>
                                    </div>
                                </td>
                                <td class="pc-cat"><c:out value="${p.categoryName}"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${p.productType == 'KE_DON'}">
                                            <span class="type-badge type-rx"><c:out value="${p.typeLabel}"/></span>
                                        </c:when>
                                        <c:when test="${p.productType == 'HAN_CHE'}">
                                            <span class="type-badge type-restricted"><c:out value="${p.typeLabel}"/></span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="type-badge type-otc"><c:out value="${p.typeLabel}"/></span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="num">
                                    <fmt:formatNumber value="${p.sellingPrice}" type="number" maxFractionDigits="0"/>&#x20AB;
                                    <span class="pc-unit">/ <c:out value="${p.sellingUnit}"/></span>
                                </td>
                                <td class="num"><c:out value="${p.availableQuantity}"/></td>
                                <td>
                                    <span class="flag-pill ${p.onlineSaleAllowed ? 'flag-yes' : 'flag-no'}">
                                        ${p.onlineSaleAllowed ? 'Có' : 'Không'}
                                    </span>
                                </td>
                                <td>
                                    <span class="status-badge status-${p.status == 'HOAT_DONG' ? 'active' : 'inactive'}">
                                        <c:out value="${p.status == 'HOAT_DONG' ? 'Đang bán' : 'Ngừng bán'}"/>
                                    </span>
                                </td>
                                <td class="pc-actions">
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
                    </div>

                    <c:url var="pagerBase" value="/admin">
                        <c:param name="action" value="products"/>
                        <c:param name="q" value="${param.q}"/>
                        <c:param name="categoryId" value="${param.categoryId}"/>
                        <c:param name="type" value="${param.type}"/>
                        <c:param name="status" value="${param.status}"/>
                    </c:url>
                    <c:set var="pagerPages" value="${pages}"/>
                    <%@ include file="/WEB-INF/jspf/pager.jspf" %>
                </c:otherwise>
            </c:choose>
        </div>
    </main>
</div>

<%@ include file="/WEB-INF/jspf/admin-footer.jspf" %>

</body>
</html>
