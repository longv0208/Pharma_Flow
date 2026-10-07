<%--
    Product card fragment. Caller sets request-scope `product` before include.
    Fields used: productId, productName, manufacturer, strength, dosageForm,
                 sellingPrice, sellingUnit, productType, displayBadge,
                 inStock, purchasable.
--%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<article class="product-card">
    <div class="product-thumb product-thumb-${fn:toLowerCase(product.productType)}">
        <c:choose>
            <c:when test="${product.productType.name() == 'KE_DON'}">
                <span class="thumb-glyph">Rx</span>
            </c:when>
            <c:when test="${product.productType.name() == 'HAN_CHE'}">
                <span class="thumb-glyph">!</span>
            </c:when>
            <c:otherwise>
                <span class="thumb-glyph">+</span>
            </c:otherwise>
        </c:choose>
        <c:if test="${not empty product.displayBadge}">
            <span class="badge badge-${fn:toLowerCase(fn:replace(product.displayBadge, ' ', '-'))}">
                <c:out value="${product.displayBadge}"/>
            </span>
        </c:if>
    </div>
    <h3 class="product-name">
        <a href="${ctx}/products/${product.productId}">
            <c:out value="${product.productName}"/>
        </a>
    </h3>
    <p class="product-meta">
        <c:out value="${product.activeIngredient}"/>
        <c:if test="${not empty product.strength}"> &middot; <c:out value="${product.strength}"/></c:if>
        <c:if test="${not empty product.dosageForm}"> &middot; <c:out value="${product.dosageForm}"/></c:if>
    </p>
    <div class="price-row">
        <span class="price">
            <fmt:formatNumber value="${product.sellingPrice}" type="number" maxFractionDigits="0"/>&#x20AB;
        </span>
        <c:if test="${not empty product.sellingUnit}">
            <span class="unit">/ <c:out value="${product.sellingUnit}"/></span>
        </c:if>
        <c:if test="${product.inStock and empty product.displayBadge}">
            <span class="badge badge-instock">Còn hàng</span>
        </c:if>
        <a class="card-go" href="${ctx}/products/${product.productId}"
           title="Xem chi tiết" aria-label="Xem chi tiết sản phẩm">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true"><path d="M9 18l6-6-6-6"/></svg>
        </a>
    </div>
    <div class="actions">
        <c:if test="${product.purchasable}">
            <form method="post" action="${ctx}/cart?action=add">
                <input type="hidden" name="productId" value="${product.productId}">
                <input type="hidden" name="quantity" value="1">
                <input type="hidden" name="back" value="products">
                <button type="submit" class="btn btn-primary" title="Thêm vào giỏ">
                    Thêm vào giỏ
                </button>
            </form>
        </c:if>
        <c:if test="${!product.purchasable}">
            <button type="button" class="btn btn-primary" disabled
                    title="Không bán trực tuyến">
                Thêm vào giỏ
            </button>
        </c:if>
    </div>
</article>
