<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>PharmaFlow — Health Care for Every Home</title>
    <link rel="stylesheet" href="${ctx}/css/main.css">
</head>
<body>

<%@ include file="/WEB-INF/jspf/header.jspf" %>

<c:if test="${not empty errorMessage}">
    <div class="alert alert-error" role="alert">
        <div class="container"><c:out value="${errorMessage}"/></div>
    </div>
</c:if>

<!-- Hero -->
<section class="hero">
    <div class="container hero-grid">
        <div class="hero-copy">
            <p class="hero-eyebrow">Seasonal Health Promotion</p>
            <h1>Health Care for Every Home</h1>
            <p class="hero-sub">Up to 50% off selected wellness products and daily OTC medicines. Shop certified pharmaceuticals and personal essentials with verified depot storage.</p>
            <div class="hero-cta">
                <a class="btn btn-primary btn-lg" href="${ctx}/products">Shop Now</a>
                <a class="btn btn-ghost btn-lg" href="${ctx}/products?promo=1">View All Offers</a>
            </div>
        </div>
        <aside class="hero-offer" aria-label="Special offer">
            <div class="offer-head">
                <span class="offer-label">Special Offer</span>
                <span class="offer-off">Save 50%</span>
            </div>
            <ul class="offer-items">
                <li><span class="offer-ic">V</span><div><b>Vitamin C 500</b><span class="offer-price">$7.90</span></div></li>
                <li><span class="offer-ic">F</span><div><b>Fish Oil 1000</b><span class="offer-price">$15.00</span></div></li>
                <li><span class="offer-ic">A</span><div><b>First Aid Paste</b><span class="offer-price">$9.30</span></div></li>
            </ul>
            <p class="offer-note">* Valid for online customers until end of month</p>
        </aside>
    </div>
</section>

<!-- Search -->
<section class="search-band">
    <div class="container">
        <form class="search-bar" action="${ctx}/products" method="get" role="search">
            <input type="search" name="q" placeholder="Search by product name, active ingredient, or symptom…" aria-label="Search products">
            <button class="btn btn-primary" type="submit">Search</button>
        </form>
        <div class="search-meta">
            <span class="quick-search">
                <span class="quick-label">Quick search:</span>
                <a href="${ctx}/products?q=fever">fever</a>
                <a href="${ctx}/products?q=vitamins">vitamins</a>
                <a href="${ctx}/products?q=cough">cough</a>
                <a href="${ctx}/products?q=skincare">skincare</a>
                <a href="${ctx}/products?q=babycare">baby care</a>
            </span>
            <span class="help-links">
                <a href="#"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M9.1 9a3 3 0 0 1 5.8 1c0 2-3 3-3 3"/><path d="M12 17h.01"/></svg> Contact Pharmacist</a>
                <a href="#"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 6-9 12-9 12s-9-6-9-12a9 9 0 1 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg> Find Store</a>
            </span>
        </div>
    </div>
</section>

<!-- Categories -->
<section class="section">
    <div class="container">
        <div class="section-head">
            <h2>Browse Categories</h2>
            <a class="view-all" href="${ctx}/products">View All &rarr;</a>
        </div>
        <c:choose>
            <c:when test="${empty categories}">
                <div class="empty-state"><p>No categories available yet.</p></div>
            </c:when>
            <c:otherwise>
                <ul class="category-grid">
                    <c:forEach var="c" items="${categories}">
                        <li>
                            <a class="category-tile" href="${ctx}/products?category=${c.categoryId}">
                                <span class="cat-icon" aria-hidden="true">
                                    <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                                        <rect x="4" y="4" width="16" height="16" rx="3"/>
                                        <path d="M12 8v8M8 12h8"/>
                                    </svg>
                                </span>
                                <span class="cat-name"><c:out value="${c.categoryName}"/></span>
                            </a>
                        </li>
                    </c:forEach>
                </ul>
            </c:otherwise>
        </c:choose>
    </div>
</section>

<!-- Promo banners (static display, matching design) -->
<section class="section section-promos">
    <div class="container promo-grid">
        <div class="promo promo-a">
            <div>
                <span class="promo-tag">Campaign</span>
                <h3>Exclusive September Offers</h3>
                <p>Save up to 30% on vitamins and dietary supplements</p>
                <a class="btn btn-primary btn-sm" href="${ctx}/products?promo=1">View Offers</a>
            </div>
            <span class="promo-art" aria-hidden="true">
                <svg width="56" height="56" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M20 7h-9M14 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18 8l-7 7-3-3"/></svg>
            </span>
        </div>
        <div class="promo promo-b">
            <div>
                <span class="promo-tag">Payment Partner</span>
                <h3>Pay Later with E-Wallet</h3>
                <p>Buy now &amp; get $50 voucher on orders above $50 using online checkout</p>
                <a class="btn btn-primary btn-sm" href="${ctx}/products">View Offer</a>
            </div>
            <span class="promo-art" aria-hidden="true">
                <svg width="56" height="56" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="2" y="6" width="20" height="14" rx="2"/><path d="M2 10h20M6 16h4"/></svg>
            </span>
        </div>
    </div>
</section>

<!-- Featured Products -->
<section class="section">
    <div class="container">
        <div class="section-head">
            <div>
                <h2>Featured Products</h2>
                <p class="section-sub">Curated everyday OTC medicines and healthcare essentials</p>
            </div>
            <a class="view-all" href="${ctx}/products">View All &rarr;</a>
        </div>
        <c:choose>
            <c:when test="${empty featuredProducts}">
                <div class="empty-state"><p>No products available yet.</p></div>
            </c:when>
            <c:otherwise>
                <div class="product-grid">
                    <c:forEach var="p" items="${featuredProducts}">
                        <c:set var="product" value="${p}" scope="request"/>
                        <jsp:include page="/WEB-INF/jspf/product-card.jspf"/>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</section>

<!-- Health Care Products -->
<section class="section">
    <div class="container">
        <div class="section-head">
            <div>
                <h2>Health Care Products</h2>
                <p class="section-sub">Popular personal wellness, first aid, and diagnostic devices</p>
            </div>
            <a class="view-all" href="${ctx}/products">View All &rarr;</a>
        </div>
        <c:choose>
            <c:when test="${empty healthCareProducts}">
                <div class="empty-state"><p>No products available yet.</p></div>
            </c:when>
            <c:otherwise>
                <div class="product-grid">
                    <c:forEach var="p" items="${healthCareProducts}">
                        <c:set var="product" value="${p}" scope="request"/>
                        <jsp:include page="/WEB-INF/jspf/product-card.jspf"/>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</section>

<%@ include file="/WEB-INF/jspf/footer.jspf" %>

</body>
</html>
