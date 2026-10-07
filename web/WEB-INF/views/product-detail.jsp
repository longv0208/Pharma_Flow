<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="p" value="${product}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><c:out value="${p.productName}"/> — PharmaFlow</title>
    <link rel="stylesheet" href="${ctx}/css/main.css">
</head>
<body>

<%@ include file="/WEB-INF/jspf/header.jspf" %>

<section class="section">
    <div class="container">

        <%-- Breadcrumb --%>
        <nav class="crumbs" aria-label="Đường dẫn">
            <a href="${ctx}/home">Trang chủ</a> /
            <a href="${ctx}/products">Sản phẩm</a>
            <c:if test="${not empty p.categoryName}">
                / <a href="${ctx}/products?category=${p.categoryId}"><c:out value="${p.categoryName}"/></a>
            </c:if>
            / <span><c:out value="${p.productName}"/></span>
        </nav>

        <div class="pd-grid">
            <%-- Gallery side --%>
            <div class="pd-thumb product-thumb product-thumb-${fn:toLowerCase(p.productType)}">
                <c:choose>
                    <c:when test="${p.productType.name() == 'KE_DON'}">
                        <span class="thumb-glyph">Rx</span>
                    </c:when>
                    <c:when test="${p.productType.name() == 'HAN_CHE'}">
                        <span class="thumb-glyph">!</span>
                    </c:when>
                    <c:otherwise>
                        <span class="thumb-glyph">+</span>
                    </c:otherwise>
                </c:choose>
                <c:if test="${not empty p.displayBadge}">
                    <span class="badge badge-${fn:toLowerCase(fn:replace(p.displayBadge, ' ', '-'))}">
                        <c:out value="${p.displayBadge}"/>
                    </span>
                </c:if>
            </div>

            <%-- Info side --%>
            <div class="pd-info">
                <div class="pd-head">
                    <div class="pd-badges">
                        <span class="type-badge type-${p.productType.name().toLowerCase()}">
                            <c:out value="${p.productType}"/>
                        </span>
                        <c:choose>
                            <c:when test="${p.productType.name() == 'KHONG_KE_DON'}">
                                <span class="rx-badge rx-otc">Thuốc không kê đơn</span>
                            </c:when>
                            <c:when test="${p.productType.name() == 'KE_DON'}">
                                <span class="rx-badge rx-required">Cần đơn thuốc</span>
                            </c:when>
                            <c:otherwise>
                                <span class="rx-badge rx-restricted">Hạn chế — không bán trực tuyến</span>
                            </c:otherwise>
                        </c:choose>
                    </div>
                    <h1 class="pd-title"><c:out value="${p.productName}"/></h1>
                    <p class="pd-meta">
                        <c:if test="${not empty p.activeIngredient}">
                            <span><c:out value="${p.activeIngredient}"/></span>
                        </c:if>
                        <c:if test="${not empty p.strength}"> · <c:out value="${p.strength}"/></c:if>
                        <c:if test="${not empty p.dosageForm}"> · <c:out value="${p.dosageForm}"/></c:if>
                    </p>
                    <c:if test="${not empty p.shortDescription}">
                        <p class="pd-short"><c:out value="${p.shortDescription}"/></p>
                    </c:if>
                </div>

                <dl class="pd-facts">
                    <c:if test="${not empty p.manufacturer}">
                        <div><dt>Nhà sản xuất</dt><dd><c:out value="${p.manufacturer}"/></dd></div>
                    </c:if>
                    <c:if test="${not empty p.registrationNumber}">
                        <div><dt>Số đăng ký</dt>
                            <dd>
                                <c:out value="${p.registrationNumber}"/>
                                <a class="reg-lookup" href="https://www.pharmacity.vn/cach-tra-cuu-thong-tin-dang-ky-thuoc.htm" target="_blank" rel="noopener noreferrer">Tra cứu đăng ký thuốc</a>
                            </dd>
                        </div>
                    </c:if>
                    <c:if test="${not empty p.categoryName}">
                        <div><dt>Danh mục</dt>
                            <dd><a href="${ctx}/products?category=${p.categoryId}"><c:out value="${p.categoryName}"/></a></dd>
                        </div>
                    </c:if>
                    <div><dt>SKU</dt><dd><c:out value="${p.sku}"/></dd></div>
                    <c:if test="${not empty p.barcode}">
                        <div><dt>Mã vạch</dt><dd><c:out value="${p.barcode}"/></dd></div>
                    </c:if>
                    <div><dt>Tồn kho</dt>
                        <dd>
                            <c:choose>
                                <c:when test="${p.inStock}"><c:out value="${p.availableQuantity}"/> <c:out value="${p.sellingUnit}"/> có sẵn</c:when>
                                <c:otherwise>Hết hàng</c:otherwise>
                            </c:choose>
                        </dd>
                    </div>
                </dl>

                <div class="pd-buy">
                    <div class="pd-price">
                        <fmt:formatNumber value="${p.sellingPrice}" type="number" maxFractionDigits="0"/>&#x20AB;
                        <span class="unit">/ <c:out value="${p.sellingUnit}"/></span>
                    </div>
                    <div class="pd-actions">
                        <c:if test="${p.purchasable}">
                            <form class="pd-add-cart" method="post" action="${ctx}/cart?action=add">
                                <input type="hidden" name="productId" value="${p.productId}">
                                <input type="hidden" name="back" value="detail">
                                <input class="pos-qty-input" type="number" name="quantity"
                                       min="1" step="1" value="1" max="${p.availableQuantity}">
                                <button type="submit" class="btn btn-primary btn-lg" title="Thêm vào giỏ">
                                    Thêm vào giỏ
                                </button>
                            </form>
                        </c:if>
                        <c:if test="${!p.purchasable}">
                            <button type="button" class="btn btn-primary btn-lg" disabled
                                    title="Không thể mua trực tuyến">
                                Thêm vào giỏ
                            </button>
                            <p class="field-hint">
                                <c:choose>
                                    <c:when test="${p.productType.name() == 'KE_DON'}">Cần dược sĩ xem xét — vui lòng đến cửa hàng.</c:when>
                                    <c:when test="${p.productType.name() == 'HAN_CHE'}">Mặt hàng hạn chế — không bán trực tuyến.</c:when>
                                    <c:otherwise>Hiện đang hết hàng.</c:otherwise>
                                </c:choose>
                            </p>
                        </c:if>
                    </div>
                </div>
            </div>
        </div>

        <%-- Medicine information — rendered only when at least one field has content --%>
        <c:if test="${not empty p.indication or not empty p.usageInstruction or not empty p.warnings or not empty p.contraindications}">
            <section class="med-info" aria-label="Thông tin thuốc">
                <h2 class="med-title">Thông tin thuốc</h2>

                <c:if test="${not empty p.indication}">
                    <div class="med-block">
                        <h3 class="med-label">Chỉ định</h3>
                        <p class="med-text"><c:out value="${p.indication}"/></p>
                    </div>
                </c:if>

                <c:if test="${not empty p.usageInstruction}">
                    <div class="med-block">
                        <h3 class="med-label">Cách dùng</h3>
                        <p class="med-text"><c:out value="${p.usageInstruction}"/></p>
                    </div>
                </c:if>

                <c:if test="${not empty p.warnings}">
                    <div class="med-block">
                        <h3 class="med-label">Cảnh báo &amp; thận trọng</h3>
                        <p class="med-text"><c:out value="${p.warnings}"/></p>
                    </div>
                </c:if>

                <c:if test="${not empty p.contraindications}">
                    <div class="med-block">
                        <h3 class="med-label">Chống chỉ định</h3>
                        <p class="med-text"><c:out value="${p.contraindications}"/></p>
                    </div>
                </c:if>
            </section>
        </c:if>
    </div>
</section>

<%@ include file="/WEB-INF/jspf/footer.jspf" %>

</body>
</html>
