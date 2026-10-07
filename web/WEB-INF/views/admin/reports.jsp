<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="reports"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Báo cáo — Quản trị — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <div class="section-head">
            <div>
                <h2>Báo cáo bán hàng</h2>
                <p class="section-sub">
                    Khoảng thời gian:
                    <fmt:formatDate value="${from}" pattern="yyyy-MM-dd"/>
                    →
                    <fmt:formatDate value="${to}" pattern="yyyy-MM-dd"/>
                </p>
            </div>
        </div>

        <%-- Date filter — only input the report accepts --%>
        <form class="filter-bar" method="get" action="${ctx}/reports">
            <input type="date" name="from"
                   value="<fmt:formatDate value='${from}' pattern='yyyy-MM-dd'/>"
                   aria-label="Từ ngày">
            <input type="date" name="to"
                   value="<fmt:formatDate value='${to}' pattern='yyyy-MM-dd'/>"
                   aria-label="Đến ngày">
            <button type="submit" class="btn btn-secondary btn-sm">Xem báo cáo</button>
            <a href="${ctx}/reports" class="btn btn-ghost btn-sm">Tháng này</a>
        </form>

        <%-- ===== Section 1: summary cards ===== --%>
        <div class="stat-grid">
            <div class="stat-card">
                <span class="stat-label">Tổng doanh thu</span>
                <span class="stat-value">
                    <fmt:formatNumber value="${sales.totalRevenue}" type="number" groupingUsed="true"/>&#x20AB;
                </span>
                <span class="stat-sub">POS + trực tuyến, chỉ giao dịch hoàn tất</span>
            </div>
            <div class="stat-card stat-ok">
                <span class="stat-label">Doanh thu tại quầy</span>
                <span class="stat-value">
                    <fmt:formatNumber value="${sales.posRevenue}" type="number" groupingUsed="true"/>&#x20AB;
                </span>
                <span class="stat-sub"><c:out value="${sales.posCount}"/> giao dịch · ${sales.posShare}% tổng</span>
            </div>
            <div class="stat-card">
                <span class="stat-label">Doanh thu trực tuyến</span>
                <span class="stat-value">
                    <fmt:formatNumber value="${sales.onlineRevenue}" type="number" groupingUsed="true"/>&#x20AB;
                </span>
                <span class="stat-sub"><c:out value="${sales.onlineCount}"/> đơn · ${sales.onlineShare}% tổng</span>
            </div>
            <div class="stat-card">
                <span class="stat-label">Tổng giao dịch hoàn tất</span>
                <span class="stat-value"><c:out value="${sales.totalCount}"/></span>
                <span class="stat-sub"><c:out value="${sales.posCount}"/> POS + <c:out value="${sales.onlineCount}"/> đơn online</span>
            </div>
            <div class="stat-card">
                <span class="stat-label">Sản phẩm đã bán</span>
                <span class="stat-value"><c:out value="${sales.totalUnits}"/></span>
                <span class="stat-sub"><c:out value="${sales.posUnits}"/> POS + <c:out value="${sales.onlineUnits}"/> online</span>
            </div>
        </div>

        <%-- Revenue share bar — HTML/CSS only --%>
        <c:if test="${sales.totalRevenue.signum() > 0}">
            <div class="admin-card" style="padding:14px 18px; margin-bottom:18px;">
                <div class="share-bar" role="img"
                     aria-label="POS ${sales.posShare}%, Online ${sales.onlineShare}%">
                    <div class="share-pos" style="width:${sales.posShare}%"></div>
                    <div class="share-online" style="width:${sales.onlineShare}%"></div>
                </div>
                <div class="share-legend">
                    <span><span class="share-dot dot-pos"></span>POS ${sales.posShare}%</span>
                    <span><span class="share-dot dot-online"></span>Trực tuyến ${sales.onlineShare}%</span>
                </div>
            </div>
        </c:if>

        <%-- ===== Section 2: daily revenue ===== --%>
        <div class="admin-card" style="margin-bottom:18px;">
            <h3 style="margin:0 0 12px; font-size:15px;">Doanh thu theo ngày</h3>
            <c:choose>
                <c:when test="${empty daily}">
                    <div class="empty-state"><p>Chưa có dữ liệu trong khoảng thời gian này.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="admin-table admin-table-fixed">
                        <thead>
                        <tr>
                            <th>Ngày</th>
                            <th class="col-num">POS</th>
                            <th class="col-num">Trực tuyến</th>
                            <th class="col-num">Tổng</th>
                            <th class="col-num">GD POS</th>
                            <th class="col-num">Đơn online</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="d" items="${daily}">
                            <tr>
                                <td><fmt:formatDate value="${d.day}" pattern="yyyy-MM-dd"/></td>
                                <td class="col-num"><fmt:formatNumber value="${d.posRevenue}" type="number" groupingUsed="true"/>&#x20AB;</td>
                                <td class="col-num"><fmt:formatNumber value="${d.onlineRevenue}" type="number" groupingUsed="true"/>&#x20AB;</td>
                                <td class="col-num"><fmt:formatNumber value="${d.totalRevenue}" type="number" groupingUsed="true"/>&#x20AB;</td>
                                <td class="col-num"><c:out value="${d.posCount}"/></td>
                                <td class="col-num"><c:out value="${d.onlineCount}"/></td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </c:otherwise>
            </c:choose>
        </div>

        <%-- ===== Section 3: top products ===== --%>
        <div class="admin-card" style="margin-bottom:18px;">
            <h3 style="margin:0 0 12px; font-size:15px;">Sản phẩm bán chạy</h3>
            <c:choose>
                <c:when test="${empty topProducts}">
                    <div class="empty-state"><p>Chưa có dữ liệu trong khoảng thời gian này.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="admin-table admin-table-fixed">
                        <thead>
                        <tr>
                            <th>#</th>
                            <th>Sản phẩm</th>
                            <th>SKU</th>
                            <th class="col-num">SL POS</th>
                            <th class="col-num">SL Online</th>
                            <th class="col-num">Tổng SL</th>
                            <th class="col-num">Doanh thu</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="p" items="${topProducts}" varStatus="st">
                            <tr>
                                <td><c:out value="${st.count}"/></td>
                                <td><c:out value="${p.productName}"/></td>
                                <td><c:out value="${p.sku}"/></td>
                                <td class="col-num"><c:out value="${p.posQty}"/></td>
                                <td class="col-num"><c:out value="${p.onlineQty}"/></td>
                                <td class="col-num"><c:out value="${p.totalQty}"/></td>
                                <td class="col-num"><fmt:formatNumber value="${p.revenue}" type="number" groupingUsed="true"/>&#x20AB;</td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </c:otherwise>
            </c:choose>
        </div>

        <%-- ===== Section 4: online order workflow ===== --%>
        <div class="admin-card" style="margin-bottom:18px;">
            <h3 style="margin:0 0 12px; font-size:15px;">Tình trạng đơn trực tuyến <span class="field-hint">(tất cả thời gian)</span></h3>
            <div class="stat-grid" style="margin-bottom:0;">
                <div class="stat-card"><span class="stat-label">Chờ xử lý</span><span class="stat-value"><c:out value="${orderStatus.choXuLy}"/></span></div>
                <div class="stat-card"><span class="stat-label">Đã xác nhận</span><span class="stat-value"><c:out value="${orderStatus.daXacNhan}"/></span></div>
                <div class="stat-card"><span class="stat-label">Đang chuẩn bị</span><span class="stat-value"><c:out value="${orderStatus.dangChuanBi}"/></span></div>
                <div class="stat-card"><span class="stat-label">Sẵn sàng</span><span class="stat-value"><c:out value="${orderStatus.sanSang}"/></span></div>
                <div class="stat-card"><span class="stat-label">Đang giao</span><span class="stat-value"><c:out value="${orderStatus.dangGiao}"/></span></div>
                <div class="stat-card"><span class="stat-label">Hoàn tất</span><span class="stat-value"><c:out value="${orderStatus.hoanTat}"/></span></div>
                <div class="stat-card"><span class="stat-label">Đã hủy</span><span class="stat-value"><c:out value="${orderStatus.daHuy}"/></span></div>
                <div class="stat-card"><span class="stat-label">Từ chối</span><span class="stat-value"><c:out value="${orderStatus.tuChoi}"/></span></div>
            </div>
        </div>

        <%-- ===== Section 5: inventory snapshot ===== --%>
        <div class="admin-card" style="margin-bottom:18px;">
            <h3 style="margin:0 0 12px; font-size:15px;">Tồn kho hiện tại</h3>
            <div class="stat-grid" style="margin-bottom:0;">
                <div class="stat-card"><span class="stat-label">Tồn thực tế</span><span class="stat-value"><c:out value="${inventory.physicalOnHand}"/></span></div>
                <div class="stat-card"><span class="stat-label">Đã giữ</span><span class="stat-value"><c:out value="${inventory.reserved}"/></span></div>
                <div class="stat-card stat-ok"><span class="stat-label">Có thể bán</span><span class="stat-value"><c:out value="${inventory.saleable}"/></span></div>
                <div class="stat-card"><span class="stat-label">Số lô</span><span class="stat-value"><c:out value="${inventory.batchCount}"/></span></div>
                <div class="stat-card stat-warn"><span class="stat-label">Lô bị khóa</span><span class="stat-value"><c:out value="${inventory.blockedBatchCount}"/></span></div>
                <div class="stat-card stat-danger"><span class="stat-label">Lô hết hạn</span><span class="stat-value"><c:out value="${inventory.expiredBatchCount}"/></span></div>
                <div class="stat-card stat-warn"><span class="stat-label">Lô sắp hết hạn</span><span class="stat-value"><c:out value="${inventory.nearExpiryBatchCount}"/></span></div>
            </div>
        </div>

        <%-- ===== Section 6: alert summary ===== --%>
        <div class="admin-card">
            <h3 style="margin:0 0 12px; font-size:15px;">Cảnh báo tồn kho</h3>
            <div class="stat-grid" style="margin-bottom:0;">
                <a class="stat-card stat-link stat-danger" href="${ctx}/inventory/alerts?type=OUT_OF_STOCK">
                    <span class="stat-label">Hết hàng</span>
                    <span class="stat-value"><c:out value="${alertCounts[0]}"/></span>
                </a>
                <a class="stat-card stat-link stat-warn" href="${ctx}/inventory/alerts?type=LOW_STOCK">
                    <span class="stat-label">Tồn kho thấp</span>
                    <span class="stat-value"><c:out value="${alertCounts[1]}"/></span>
                </a>
                <a class="stat-card stat-link stat-warn" href="${ctx}/inventory/alerts?type=NEAR_EXPIRY">
                    <span class="stat-label">Sắp hết hạn</span>
                    <span class="stat-value"><c:out value="${alertCounts[2]}"/></span>
                </a>
                <a class="stat-card stat-link stat-danger" href="${ctx}/inventory/alerts?type=EXPIRED">
                    <span class="stat-label">Hết hạn</span>
                    <span class="stat-value"><c:out value="${alertCounts[3]}"/></span>
                </a>
            </div>
        </div>
    </main>
</div>

<%@ include file="/WEB-INF/jspf/admin-footer.jspf" %>

</body>
</html>
