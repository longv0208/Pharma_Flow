<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="stocktake"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Kiểm kê — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <div class="section-head">
            <div>
                <h2>Kiểm kê</h2>
                <p class="section-sub">Đếm và đối chiếu tồn kho vật lý định kỳ.</p>
            </div>
        </div>

        <c:if test="${param.ok == 'created'}">
            <div class="alert alert-success" role="status">Đã tạo kiểm kê.</div>
        </c:if>
        <c:if test="${param.err == 'notfound'}">
            <div class="alert alert-error" role="alert">Không tìm thấy kiểm kê.</div>
        </c:if>
        <c:if test="${param.err == 'db'}">
            <div class="alert alert-error" role="alert">Lỗi cơ sở dữ liệu — kiểm kê chưa được lưu. Kiểm tra phạm vi (Zone/Rack/Bin) hợp lệ.</div>
        </c:if>
        <c:if test="${param.err == 'badscope'}">
            <div class="alert alert-error" role="alert">Chọn đúng phạm vi kiểm kê (Zone / Rack / Bin) trước khi tạo.</div>
        </c:if>

        <div class="admin-card" style="margin-bottom:1.25rem;">
            <form method="post" action="${ctx}/inventory/stocktakes" id="createStocktakeForm"
                  onsubmit="this.querySelector('button[type=submit]').disabled = true;">
                <input type="hidden" name="action" value="create">
                <fieldset class="profile-group">
                    <legend>Tạo kiểm kê mới</legend>
                    <div class="profile-grid">
                        <div class="form-field">
                            <label for="scopeType">Phạm vi</label>
                            <select name="scopeType" id="scopeType" required>
                                <option value="TOAN_KHO">Toàn kho</option>
                                <option value="ZONE">Một Zone</option>
                                <option value="RACK">Một Rack (kệ)</option>
                                <option value="BIN">Một Bin (ngăn)</option>
                            </select>
                        </div>
                        <div class="form-field" id="scopeZoneWrap" hidden>
                            <label for="scopeZone">Zone</label>
                            <select id="scopeZone">
                                <option value="">— Chọn Zone —</option>
                                <c:forEach var="z" items="${zones}">
                                    <option value="${z.zoneId}"><c:out value="${z.zoneCode}"/> — <c:out value="${z.zoneName}"/></option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="form-field" id="scopeRackWrap" hidden>
                            <label for="scopeRack">Rack</label>
                            <select id="scopeRack">
                                <option value="">— Chọn Rack —</option>
                                <c:forEach var="r" items="${racks}">
                                    <option value="${r.rackId}" data-zone="${r.zoneId}">
                                        <c:out value="${r.zoneCode}"/> / <c:out value="${r.rackCode}"/> — <c:out value="${r.rackName}"/>
                                    </option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="form-field" id="scopeBinWrap" hidden>
                            <label for="scopeBin">Bin</label>
                            <select id="scopeBin">
                                <option value="">— Chọn Bin —</option>
                                <c:forEach var="b" items="${bins}">
                                    <option value="${b.binId}" data-zone="${b.zoneId}" data-rack="${b.rackId}">
                                        <c:out value="${b.fullCode}"/>
                                    </option>
                                </c:forEach>
                            </select>
                        </div>
                        <input type="hidden" name="scopeId" id="scopeId" value="">
                    </div>
                    <div class="profile-actions">
                        <button type="submit" class="btn btn-primary">Tạo kiểm kê</button>
                    </div>
                </fieldset>
            </form>
        </div>
        <script>
        (function () {
            var typeSel = document.getElementById('scopeType');
            var zoneSel = document.getElementById('scopeZone');
            var rackSel = document.getElementById('scopeRack');
            var binSel = document.getElementById('scopeBin');
            var scopeId = document.getElementById('scopeId');
            // Keep full option lists — filter by showing/hiding, no async fetch.
            var allRackOpts = Array.prototype.slice.call(rackSel.querySelectorAll('option[data-zone]'));
            var allBinOpts = Array.prototype.slice.call(binSel.querySelectorAll('option[data-rack]'));

            function show(el, on) { el.hidden = !on; }

            function rebuildRackOptions(zoneId) {
                var keep = rackSel.querySelector('option[value=""]');
                rackSel.innerHTML = '';
                if (keep) rackSel.appendChild(keep);
                allRackOpts.forEach(function (o) {
                    if (!zoneId || o.getAttribute('data-zone') === String(zoneId)) {
                        rackSel.appendChild(o.cloneNode(true));
                    }
                });
            }

            function rebuildBinOptions(zoneId, rackId) {
                var keep = binSel.querySelector('option[value=""]');
                binSel.innerHTML = '';
                if (keep) binSel.appendChild(keep);
                allBinOpts.forEach(function (o) {
                    var okZone = !zoneId || o.getAttribute('data-zone') === String(zoneId);
                    var okRack = !rackId || o.getAttribute('data-rack') === String(rackId);
                    if (okZone && okRack) {
                        binSel.appendChild(o.cloneNode(true));
                    }
                });
            }

            function syncScopeId() {
                var t = typeSel.value;
                if (t === 'TOAN_KHO') scopeId.value = '';
                else if (t === 'ZONE') scopeId.value = zoneSel.value;
                else if (t === 'RACK') scopeId.value = rackSel.value;
                else if (t === 'BIN') scopeId.value = binSel.value;
            }

            function onType() {
                var t = typeSel.value;
                show(document.getElementById('scopeZoneWrap'), t !== 'TOAN_KHO');
                show(document.getElementById('scopeRackWrap'), t === 'RACK' || t === 'BIN');
                show(document.getElementById('scopeBinWrap'), t === 'BIN');
                if (t === 'RACK' || t === 'BIN') {
                    rebuildRackOptions(zoneSel.value);
                }
                if (t === 'BIN') {
                    rebuildBinOptions(zoneSel.value, rackSel.value);
                }
                syncScopeId();
            }

            typeSel.addEventListener('change', onType);
            zoneSel.addEventListener('change', function () {
                rebuildRackOptions(zoneSel.value);
                rebuildBinOptions(zoneSel.value, '');
                syncScopeId();
            });
            rackSel.addEventListener('change', function () {
                rebuildBinOptions(zoneSel.value, rackSel.value);
                syncScopeId();
            });
            binSel.addEventListener('change', syncScopeId);

            document.getElementById('createStocktakeForm').addEventListener('submit', function (e) {
                syncScopeId();
                var t = typeSel.value;
                if (t !== 'TOAN_KHO' && (!scopeId.value || scopeId.value === '0')) {
                    e.preventDefault();
                    alert('Chọn vị trí thuộc phạm vi kiểm kê.');
                    this.querySelector('button[type=submit]').disabled = false;
                }
            });
            onType();
        })();
        </script>

        <%-- Status filter --%>
        <div class="filter-bar">
            <a class="btn ${empty param.status ? 'btn-secondary' : 'btn-ghost'} btn-sm"
               href="${ctx}/inventory/stocktakes">Tất cả</a>
            <a class="btn ${param.status == 'BAN_NHAP' ? 'btn-secondary' : 'btn-ghost'} btn-sm"
               href="${ctx}/inventory/stocktakes?status=BAN_NHAP">Bản nháp</a>
            <a class="btn ${param.status == 'DANG_KIEM_KE' ? 'btn-secondary' : 'btn-ghost'} btn-sm"
               href="${ctx}/inventory/stocktakes?status=DANG_KIEM_KE">Đang kiểm kê</a>
            <a class="btn ${param.status == 'HOAN_TAT' ? 'btn-secondary' : 'btn-ghost'} btn-sm"
               href="${ctx}/inventory/stocktakes?status=HOAN_TAT">Hoàn tất</a>
        </div>

        <div class="admin-card">
            <c:choose>
                <c:when test="${empty stocktakes}">
                    <div class="empty-state"><p>Không tìm thấy kiểm kê nào.</p></div>
                </c:when>
                <c:otherwise>
                    <table class="admin-table admin-table-fixed">
                        <thead>
                        <tr>
                            <th>Mã Kiểm kê</th>
                            <th>Phạm vi</th>
                            <th>Ngày tạo</th>
                            <th>Người tạo</th>
                            <th>Trạng thái</th>
                            <th class="col-num">Số mục</th>
                            <th class="col-num">Chênh lệch</th>
                            <th>Hoàn thành lúc</th>
                            <th>Thao tác</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="s" items="${stocktakes}">
                            <tr>
                                <td><strong>ST-<c:out value="${s.stocktakeId}"/></strong></td>
                                <td><c:out value="${s.scopeLabel}"/></td>
                                <td><fmt:formatDate value="${s.createdAt}" pattern="dd/MM/yyyy HH:mm"/></td>
                                <td><c:out value="${s.createdByName}"/></td>
                                <td>
                                    <span class="status-badge ${s.statusCss}">
                                        <c:out value="${s.statusLabel}"/>
                                    </span>
                                </td>
                                <td><c:out value="${s.itemCount}"/></td>
                                <td><c:out value="${s.differenceCount}"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${empty s.completedAt}">—</c:when>
                                        <c:otherwise><fmt:formatDate value="${s.completedAt}" pattern="dd/MM/yyyy HH:mm"/></c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <a class="btn btn-ghost btn-sm"
                                       href="${ctx}/inventory/stocktakes?action=detail&id=${s.stocktakeId}">
                                        <c:out value="${s.actionLabel}"/>
                                    </a>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>

                    <%-- Pagination --%>
                    <c:url var="pagerBase" value="/inventory/stocktakes">
                        <c:param name="status" value="${param.status}"/>
                    </c:url>
                    <c:set var="pagerPages" value="${totalPages}"/>
                    <%@ include file="/WEB-INF/jspf/pager.jspf" %>
                </c:otherwise>
            </c:choose>
        </div>
    </main>
</div>

<%@ include file="/WEB-INF/jspf/admin-footer.jspf" %>

</body>
</html>
