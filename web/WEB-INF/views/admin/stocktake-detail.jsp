<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="stocktake"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Kiểm kê #<c:out value="${stocktake.stocktakeId}"/> — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <nav class="crumbs" aria-label="Breadcrumb">
            <a href="${ctx}/inventory">Tồn kho</a>
            <span aria-hidden="true">›</span>
            <a href="${ctx}/inventory/stocktakes">Kiểm kê</a>
            <span aria-hidden="true">›</span>
            <span>#<c:out value="${stocktake.stocktakeId}"/></span>
        </nav>

        <div class="section-head">
            <div>
                <h2>Kiểm kê #<c:out value="${stocktake.stocktakeId}"/></h2>
                <p class="section-sub">
                    Trạng thái:
                    <span class="status-badge ${stocktake.statusCss}">
                        <c:out value="${stocktake.statusLabel}"/>
                    </span>
                </p>
            </div>
            <a class="btn btn-ghost" href="${ctx}/inventory/stocktakes">← Quay lại Kiểm kê</a>
        </div>

        <%-- Alerts --%>
        <c:if test="${param.ok == 'created'}">
            <div class="alert alert-success" role="status">Đã tạo kiểm kê.</div>
        </c:if>
        <c:if test="${param.ok == 'started'}">
            <div class="alert alert-success" role="status">Đã bắt đầu kiểm kê — đếm số lượng thực tế của từng lô.</div>
        </c:if>
        <c:if test="${param.ok == 'saved'}">
            <div class="alert alert-success" role="status">Đã lưu số đếm.</div>
        </c:if>
        <c:if test="${param.ok == 'completed'}">
            <div class="alert alert-success" role="status">Kiểm kê đã hoàn thành — tồn kho đã được đối chiếu.</div>
        </c:if>
        <c:if test="${param.err == 'notdraft'}">
            <div class="alert alert-error" role="alert">Kiểm kê này đã được bắt đầu.</div>
        </c:if>
        <c:if test="${param.err == 'notinprogress'}">
            <div class="alert alert-error" role="alert">Kiểm kê này không đang tiến hành — số đếm chỉ để xem.</div>
        </c:if>
        <c:if test="${param.err == 'empty'}">
            <div class="alert alert-error" role="alert">Không có lô tồn kho nào để đếm.</div>
        </c:if>
        <c:if test="${param.err == 'missingcounts'}">
            <div class="alert alert-error" role="alert">Phải đếm tất cả các lô trước khi hoàn thành kiểm kê.</div>
        </c:if>
        <c:if test="${param.err == 'baditem'}">
            <div class="alert alert-error" role="alert">Một số đếm đã gửi không thuộc về kiểm kê này.</div>
        </c:if>
        <c:if test="${param.err == 'badquantity'}">
            <div class="alert alert-error" role="alert">Số lượng thực tế phải là số nguyên lớn hơn hoặc bằng 0.</div>
        </c:if>
        <c:if test="${param.err == 'belowreserved'}">
            <div class="alert alert-error" role="alert"><c:out value="${param.msg}"/></div>
        </c:if>
        <c:if test="${param.err == 'notfound'}">
            <div class="alert alert-error" role="alert">Không tìm thấy kiểm kê.</div>
        </c:if>
        <c:if test="${param.err == 'db'}">
            <div class="alert alert-error" role="alert">Lỗi cơ sở dữ liệu — thay đổi chưa được lưu.</div>
        </c:if>

        <div class="profile-card">
            <%-- Header info --%>
            <fieldset class="profile-group">
                <legend>Thông tin</legend>
                <div class="profile-grid">
                    <div class="form-field">
                        <label>Ngày tạo</label>
                        <span><fmt:formatDate value="${stocktake.createdAt}" pattern="dd/MM/yyyy HH:mm"/></span>
                    </div>
                    <div class="form-field">
                        <label>Người tạo</label>
                        <span><c:out value="${stocktake.createdByName}"/></span>
                    </div>
                    <c:if test="${not empty stocktake.completedAt}">
                        <div class="form-field">
                            <label>Hoàn thành lúc</label>
                            <span><fmt:formatDate value="${stocktake.completedAt}" pattern="dd/MM/yyyy HH:mm"/></span>
                        </div>
                    </c:if>
                    <c:if test="${stocktake.status == 'DANG_KIEM_KE'}">
                        <div class="form-field">
                            <label>Tiến độ</label>
                            <span><strong><c:out value="${stocktake.countedCount}"/> / <c:out value="${stocktake.itemCount}"/></strong> lô đã đếm</span>
                        </div>
                    </c:if>
                </div>
            </fieldset>

            <%-- ==================== BAN_NHAP: start view ==================== --%>
            <c:if test="${stocktake.status == 'BAN_NHAP'}">
                <fieldset class="profile-group">
                    <legend>Bắt đầu Kiểm kê</legend>
                    <p class="field-hint">
                        Kiểm kê này chưa được bắt đầu. Khi bắt đầu, hệ thống sẽ tạo
                        danh sách đếm từ các lô tồn kho hiện tại — bao gồm cả lô bị khóa
                        và đã hết hạn, vì đếm vật lý không phân biệt khả năng bán.
                    </p>
                    <div class="profile-actions">
                        <form method="post" action="${ctx}/inventory/stocktakes"
                              onsubmit="this.querySelector('button').disabled = true; this.querySelector('button').textContent = 'Đang xử lý...';">
                            <input type="hidden" name="action" value="start">
                            <input type="hidden" name="id" value="${stocktake.stocktakeId}">
                            <button type="submit" class="btn btn-primary">Bắt đầu Kiểm kê</button>
                        </form>
                    </div>
                </fieldset>
            </c:if>

            <%-- ==================== DANG_KIEM_KE / HOAN_TAT: items ==================== --%>
            <c:if test="${stocktake.status != 'BAN_NHAP'}">

                <%-- Summary cards on the completed screen --%>
                <c:if test="${stocktake.status == 'HOAN_TAT'}">
                    <fieldset class="profile-group">
                        <legend>Tóm tắt Đối chiếu</legend>
                        <div class="profile-grid">
                            <div class="form-field">
                                <label>Tổng số lô</label>
                                <span><strong><c:out value="${stocktake.itemCount}"/></strong></span>
                            </div>
                            <div class="form-field">
                                <label>Khớp</label>
                                <span><c:out value="${matchedCount}"/></span>
                            </div>
                            <div class="form-field">
                                <label>Chênh lệch</label>
                                <span><c:out value="${stocktake.differenceCount}"/></span>
                            </div>
                            <div class="form-field">
                                <label>Chênh lệch ròng</label>
                                <span><strong><c:out value="${netDifference > 0 ? '+' : ''}${netDifference}"/></strong></span>
                            </div>
                        </div>
                    </fieldset>
                </c:if>

                <form method="get" action="${ctx}/inventory/stocktakes" class="filter-bar">
                    <input type="hidden" name="action" value="detail">
                    <input type="hidden" name="id" value="${stocktake.stocktakeId}">
                    <input type="search" name="q" value="<c:out value='${param.q}'/>"
                           placeholder="Sản phẩm, SKU hoặc lô…" aria-label="Tìm kiếm mục">
                    <button type="submit" class="btn btn-secondary btn-sm">Tìm kiếm</button>
                    <a href="${ctx}/inventory/stocktakes?action=detail&id=${stocktake.stocktakeId}"
                       class="btn btn-ghost btn-sm">Đặt lại</a>
                </form>

                <form method="post" action="${ctx}/inventory/stocktakes"
                      onsubmit="var b = this.querySelector('button[name=submitBtn]'); if (b) { b.disabled = true; b.textContent = 'Đang xử lý...'; }">
                    <input type="hidden" name="id" value="${stocktake.stocktakeId}">

                    <table class="admin-table">
                        <thead>
                        <tr>
                            <th>Sản phẩm</th>
                            <th>SKU</th>
                            <th>Lô</th>
                            <th>Hạn dùng</th>
                            <th>Trạng thái</th>
                            <th class="col-num">SL Hệ thống</th>
                            <th class="col-num">SL Thực tế</th>
                            <th class="col-num">Chênh lệch</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="it" items="${items}">
                            <tr>
                                <td><c:out value="${it.productName}"/></td>
                                <td><c:out value="${it.sku}"/></td>
                                <td>
                                    <a href="${ctx}/inventory?action=batch&id=${it.batchId}">
                                        <c:out value="${it.batchNumber}"/>
                                    </a>
                                </td>
                                <td><c:out value="${it.expiryDate}"/></td>
                                <td>
                                    <span class="status-badge batch-${it.batchStatus.toLowerCase().replace('_','-')}">
                                        <c:choose>
                                            <c:when test="${it.batchStatus == 'CO_SAN'}">Có sẵn</c:when>
                                            <c:when test="${it.batchStatus == 'SAP_HET_HAN'}">Sắp hết hạn</c:when>
                                            <c:when test="${it.batchStatus == 'HET_HAN'}">Hết hạn</c:when>
                                            <c:when test="${it.batchStatus == 'BI_KHOA'}">Bị khóa</c:when>
                                            <c:when test="${it.batchStatus == 'HET_HANG'}">Hết hàng</c:when>
                                            <c:otherwise><c:out value="${it.batchStatus}"/></c:otherwise>
                                        </c:choose>
                                    </span>
                                </td>
                                <td><c:out value="${it.systemQuantity}"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${stocktake.status == 'DANG_KIEM_KE'}">
                                            <input type="number" name="qty_${it.stocktakeItemId}"
                                                   value="${it.actualQuantity}" min="0" step="1"
                                                   class="qty-input" aria-label="Số lượng thực tế">
                                        </c:when>
                                        <c:otherwise>
                                            <c:choose>
                                                <c:when test="${empty it.actualQuantity}">—</c:when>
                                                <c:otherwise><c:out value="${it.actualQuantity}"/></c:otherwise>
                                            </c:choose>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${empty it.differenceQuantity}">—</c:when>
                                        <c:otherwise><strong><c:out value="${it.differenceLabel}"/></strong></c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>

                    <c:if test="${stocktake.status == 'DANG_KIEM_KE'}">
                        <div class="profile-actions">
                            <button type="submit" name="submitBtn" value="save"
                                    class="btn btn-secondary"
                                    formaction="${ctx}/inventory/stocktakes?action=save">
                                Lưu Số đếm
                            </button>
                            <button type="submit" name="submitBtn" value="complete"
                                    class="btn btn-primary"
                                    formaction="${ctx}/inventory/stocktakes?action=complete">
                                Hoàn thành &amp; Đối chiếu
                            </button>
                        </div>
                    </c:if>
                </form>

                <c:if test="${stocktake.status == 'HOAN_TAT'}">
                    <div class="profile-actions">
                        <a class="btn btn-secondary"
                           href="${ctx}/inventory?action=history&type=DIEU_CHINH_KIEM_KE">
                            Xem Lịch sử Tồn kho
                        </a>
                    </div>
                </c:if>
            </c:if>
        </div>
    </main>
</div>

<%@ include file="/WEB-INF/jspf/admin-footer.jspf" %>

</body>
</html>
