<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="adminNav" value="stocktake"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/jspf/admin-head.jspf" %>
    <title>Stocktake #<c:out value="${stocktake.stocktakeId}"/> — PharmaFlow</title>
</head>
<body class="admin-layout">

<%@ include file="/WEB-INF/jspf/admin-nav.jspf" %>

<div class="admin-shell">
    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>

    <main class="admin-main">
        <nav class="crumbs" aria-label="Breadcrumb">
            <a href="${ctx}/inventory">Inventory</a>
            <span aria-hidden="true">›</span>
            <a href="${ctx}/inventory/stocktakes">Stocktake</a>
            <span aria-hidden="true">›</span>
            <span>#<c:out value="${stocktake.stocktakeId}"/></span>
        </nav>

        <div class="section-head">
            <div>
                <h2>Stocktake #<c:out value="${stocktake.stocktakeId}"/></h2>
                <p class="section-sub">
                    Status:
                    <span class="status-badge ${stocktake.statusCss}">
                        <c:out value="${stocktake.statusLabel}"/>
                    </span>
                </p>
            </div>
            <a class="btn btn-ghost" href="${ctx}/inventory/stocktakes">← Back to Stocktakes</a>
        </div>

        <%-- Alerts --%>
        <c:if test="${param.ok == 'created'}">
            <div class="alert alert-success" role="status">Stocktake created.</div>
        </c:if>
        <c:if test="${param.ok == 'started'}">
            <div class="alert alert-success" role="status">Stocktake started — count the physical quantity of each batch.</div>
        </c:if>
        <c:if test="${param.ok == 'saved'}">
            <div class="alert alert-success" role="status">Counts saved.</div>
        </c:if>
        <c:if test="${param.ok == 'completed'}">
            <div class="alert alert-success" role="status">Stocktake completed — inventory reconciled.</div>
        </c:if>
        <c:if test="${param.err == 'notdraft'}">
            <div class="alert alert-error" role="alert">This stocktake has already been started.</div>
        </c:if>
        <c:if test="${param.err == 'notinprogress'}">
            <div class="alert alert-error" role="alert">This stocktake is not in progress — its counts are read-only.</div>
        </c:if>
        <c:if test="${param.err == 'empty'}">
            <div class="alert alert-error" role="alert">No inventory batches are available to count.</div>
        </c:if>
        <c:if test="${param.err == 'missingcounts'}">
            <div class="alert alert-error" role="alert">All batches must be counted before the stocktake can be completed.</div>
        </c:if>
        <c:if test="${param.err == 'baditem'}">
            <div class="alert alert-error" role="alert">A submitted count does not belong to this stocktake.</div>
        </c:if>
        <c:if test="${param.err == 'badquantity'}">
            <div class="alert alert-error" role="alert">Actual quantity must be a whole number of zero or more.</div>
        </c:if>
        <c:if test="${param.err == 'belowreserved'}">
            <div class="alert alert-error" role="alert"><c:out value="${param.msg}"/></div>
        </c:if>
        <c:if test="${param.err == 'notfound'}">
            <div class="alert alert-error" role="alert">Stocktake not found.</div>
        </c:if>
        <c:if test="${param.err == 'db'}">
            <div class="alert alert-error" role="alert">Database error — the change was not saved.</div>
        </c:if>

        <div class="profile-card">
            <%-- Header info --%>
            <fieldset class="profile-group">
                <legend>Information</legend>
                <div class="profile-grid">
                    <div class="form-field">
                        <label>Created</label>
                        <span><fmt:formatDate value="${stocktake.createdAt}" pattern="dd/MM/yyyy HH:mm"/></span>
                    </div>
                    <div class="form-field">
                        <label>Created By</label>
                        <span><c:out value="${stocktake.createdByName}"/></span>
                    </div>
                    <c:if test="${not empty stocktake.completedAt}">
                        <div class="form-field">
                            <label>Completed At</label>
                            <span><fmt:formatDate value="${stocktake.completedAt}" pattern="dd/MM/yyyy HH:mm"/></span>
                        </div>
                    </c:if>
                    <c:if test="${stocktake.status == 'IN_PROGRESS'}">
                        <div class="form-field">
                            <label>Progress</label>
                            <span><strong><c:out value="${stocktake.countedCount}"/> / <c:out value="${stocktake.itemCount}"/></strong> batches counted</span>
                        </div>
                    </c:if>
                </div>
            </fieldset>

            <%-- ==================== DRAFT: start view ==================== --%>
            <c:if test="${stocktake.status == 'DRAFT'}">
                <fieldset class="profile-group">
                    <legend>Start Stocktake</legend>
                    <p class="field-hint">
                        This stocktake has not started yet. Starting will create a counting
                        list from the current inventory batches — including blocked and
                        expired stock, since physical counting ignores saleability.
                    </p>
                    <div class="profile-actions">
                        <form method="post" action="${ctx}/inventory/stocktakes"
                              onsubmit="this.querySelector('button').disabled = true; this.querySelector('button').textContent = 'Processing...';">
                            <input type="hidden" name="action" value="start">
                            <input type="hidden" name="id" value="${stocktake.stocktakeId}">
                            <button type="submit" class="btn btn-primary">Start Stocktake</button>
                        </form>
                    </div>
                </fieldset>
            </c:if>

            <%-- ==================== IN_PROGRESS / COMPLETED: items ==================== --%>
            <c:if test="${stocktake.status != 'DRAFT'}">

                <%-- Summary cards on the completed screen --%>
                <c:if test="${stocktake.status == 'COMPLETED'}">
                    <fieldset class="profile-group">
                        <legend>Reconciliation Summary</legend>
                        <div class="profile-grid">
                            <div class="form-field">
                                <label>Total Batches</label>
                                <span><strong><c:out value="${stocktake.itemCount}"/></strong></span>
                            </div>
                            <div class="form-field">
                                <label>Matched</label>
                                <span><c:out value="${matchedCount}"/></span>
                            </div>
                            <div class="form-field">
                                <label>Differences</label>
                                <span><c:out value="${stocktake.differenceCount}"/></span>
                            </div>
                            <div class="form-field">
                                <label>Net Difference</label>
                                <span><strong><c:out value="${netDifference > 0 ? '+' : ''}${netDifference}"/></strong></span>
                            </div>
                        </div>
                    </fieldset>
                </c:if>

                <form method="get" action="${ctx}/inventory/stocktakes" class="filter-bar">
                    <input type="hidden" name="action" value="detail">
                    <input type="hidden" name="id" value="${stocktake.stocktakeId}">
                    <input type="search" name="q" value="<c:out value='${param.q}'/>"
                           placeholder="Product, SKU or batch…" aria-label="Search item">
                    <button type="submit" class="btn btn-secondary btn-sm">Search</button>
                    <a href="${ctx}/inventory/stocktakes?action=detail&id=${stocktake.stocktakeId}"
                       class="btn btn-ghost btn-sm">Reset</a>
                </form>

                <form method="post" action="${ctx}/inventory/stocktakes"
                      onsubmit="var b = this.querySelector('button[name=submitBtn]'); if (b) { b.disabled = true; b.textContent = 'Processing...'; }">
                    <input type="hidden" name="id" value="${stocktake.stocktakeId}">

                    <table class="admin-table">
                        <thead>
                        <tr>
                            <th>Product</th>
                            <th>SKU</th>
                            <th>Batch</th>
                            <th>Expiry</th>
                            <th>Status</th>
                            <th class="col-num">System Qty</th>
                            <th class="col-num">Actual Qty</th>
                            <th class="col-num">Difference</th>
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
                                        <c:out value="${it.batchStatus}"/>
                                    </span>
                                </td>
                                <td><c:out value="${it.systemQuantity}"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${stocktake.status == 'IN_PROGRESS'}">
                                            <input type="number" name="qty_${it.stocktakeItemId}"
                                                   value="${it.actualQuantity}" min="0" step="1"
                                                   class="qty-input" aria-label="Actual quantity">
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

                    <c:if test="${stocktake.status == 'IN_PROGRESS'}">
                        <div class="profile-actions">
                            <button type="submit" name="submitBtn" value="save"
                                    class="btn btn-secondary"
                                    formaction="${ctx}/inventory/stocktakes?action=save">
                                Save Counts
                            </button>
                            <button type="submit" name="submitBtn" value="complete"
                                    class="btn btn-primary"
                                    formaction="${ctx}/inventory/stocktakes?action=complete">
                                Complete &amp; Reconcile
                            </button>
                        </div>
                    </c:if>
                </form>

                <c:if test="${stocktake.status == 'COMPLETED'}">
                    <div class="profile-actions">
                        <a class="btn btn-secondary"
                           href="${ctx}/inventory?action=history&type=STOCKTAKE_ADJUSTMENT">
                            View Inventory History
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
