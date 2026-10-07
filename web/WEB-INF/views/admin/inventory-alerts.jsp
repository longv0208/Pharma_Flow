<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="inventory-alerts"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Cảnh báo Tồn kho — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <nav class="crumbs" aria-label="Breadcrumb">
            <a href="${ctx}/inventory">Tồn kho</a>
            <span aria-hidden="true">›</span>
            <span>Cảnh báo</span>
        </nav>

        <div class="section-head">
            <div>
                <h2>Cảnh báo Tồn kho</h2>
                <p class="section-sub">Theo dõi các thuốc cần chú ý về tồn kho.</p>
            </div>
            <c:if test="${ownerAdmin}">
                <a class="btn btn-secondary" href="${ctx}/inventory/alerts?action=settings">Cài đặt Cảnh báo</a>
            </c:if>
        </div>

        <%-- Summary cards — same rules as the table, recalculated per request --%>
        <div class="dash-grid">
            <div class="dash-card">
                <span class="dash-name">Hết hàng</span>
                <span class="dash-desc">Tồn có thể bán = 0</span>
                <strong class="alert-count">${summary[0]}</strong>
            </div>
            <div class="dash-card">
                <span class="dash-name">Sắp hết hàng</span>
                <span class="dash-desc">Có thể bán ≤ ${settings.minimumStockLevel}</span>
                <strong class="alert-count">${summary[1]}</strong>
            </div>
            <div class="dash-card">
                <span class="dash-name">Sắp hết hạn</span>
                <span class="dash-desc">Trong vòng ${settings.nearExpiryWarningDays} ngày</span>
                <strong class="alert-count">${summary[2]}</strong>
            </div>
            <div class="dash-card">
                <span class="dash-name">Đã hết hạn</span>
                <span class="dash-desc">Quá hạn, còn tồn kho</span>
                <strong class="alert-count">${summary[3]}</strong>
            </div>
        </div>

        <%-- Filters — GET keeps them bookmarkable --%>
        <form class="filter-bar" method="get" action="${ctx}/inventory/alerts">
            <select name="type" aria-label="Loại cảnh báo">
                <option value="">Tất cả cảnh báo</option>
                <option value="OUT_OF_STOCK" ${param.type == 'OUT_OF_STOCK' ? 'selected' : ''}>Hết hàng</option>
                <option value="LOW_STOCK"    ${param.type == 'LOW_STOCK'    ? 'selected' : ''}>Sắp hết hàng</option>
                <option value="NEAR_EXPIRY"  ${param.type == 'NEAR_EXPIRY'  ? 'selected' : ''}>Sắp hết hạn</option>
                <option value="EXPIRED"      ${param.type == 'EXPIRED'      ? 'selected' : ''}>Đã hết hạn</option>
            </select>
            <input type="search" name="q" value="<c:out value='${param.q}'/>"
                   placeholder="Tên sản phẩm / SKU / lô…" aria-label="Tìm kiếm cảnh báo">
            <button type="submit" class="btn btn-secondary btn-sm">Lọc</button>
            <a href="${ctx}/inventory/alerts" class="btn btn-ghost btn-sm">Đặt lại</a>
        </form>

        <div class="admin-card">
            <c:choose>
                <c:when test="${empty alerts}">
                    <div class="empty-state"><p>Không có cảnh báo nào — tồn kho ở trạng thái tốt.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="admin-table admin-table-fixed">
                        <thead>
                        <tr>
                            <th>Cảnh báo</th>
                            <th>Sản phẩm</th>
                            <th>SKU</th>
                            <th>Lô</th>
                            <th class="col-num">Số lượng</th>
                            <th>Ngưỡng / Hạn dùng</th>
                            <th>Chi tiết</th>
                            <th>Thao tác</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="a" items="${alerts}">
                            <tr>
                                <td>
                                    <span class="status-badge ${a.alertCss}">
                                        <c:out value="${a.alertTypeLabel}"/>
                                    </span>
                                </td>
                                <td><c:out value="${a.productName}"/></td>
                                <td><c:out value="${a.sku}"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${empty a.batchNumber}">—</c:when>
                                        <c:otherwise><c:out value="${a.batchNumber}"/></c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${a.batchLevel}">
                                            <c:out value="${a.quantity}"/> tồn thực tế
                                        </c:when>
                                        <c:otherwise>
                                            <c:out value="${a.quantity}"/> khả dụng
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${a.batchLevel}">
                                            <c:choose>
                                                <c:when test="${a.alertType == 'EXPIRED'}">
                                                    Hết hạn: <fmt:formatDate value="${a.expiryDate}" pattern="dd/MM/yyyy"/>
                                                </c:when>
                                                <c:otherwise>
                                                    Hạn dùng: <fmt:formatDate value="${a.expiryDate}" pattern="dd/MM/yyyy"/>
                                                </c:otherwise>
                                            </c:choose>
                                        </c:when>
                                        <c:otherwise>
                                            Tối thiểu: <c:out value="${a.threshold}"/>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${a.batchLevel}">
                                            <c:out value="${a.expiryDetail}"/>
                                        </c:when>
                                        <c:otherwise>
                                            <c:out value="${a.message}"/>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${a.batchLevel}">
                                            <a class="btn btn-secondary btn-sm"
                                               href="${ctx}/inventory?action=batch&id=${a.batchId}">Xem Lô</a>
                                        </c:when>
                                        <c:otherwise>
                                            <a class="btn btn-secondary btn-sm"
                                               href="${ctx}/inventory?action=product&id=${a.productId}">Xem Tồn kho</a>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>

                    <%-- Pagination — preserves type + q --%>
                    <c:if test="${totalPages > 1}">
                        <nav class="pager" aria-label="Trang">
                            <c:forEach var="i" begin="1" end="${totalPages}">
                                <c:url var="pageUrl" value="/inventory/alerts">
                                    <c:param name="type" value="${param.type}"/>
                                    <c:param name="q" value="${param.q}"/>
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
