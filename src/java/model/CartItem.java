package model;

import java.math.BigDecimal;

/**
 * Mirror of `cart_items` plus display-only JOIN fields (product name, sku,
 * selling unit/price, live saleable quantity). The price shown here is only a
 * display copy — checkout always re-reads products.selling_price under lock.
 */
public class CartItem {

    private Long cartItemId;
    private Long cartId;
    private Long productId;
    private Integer quantity;

    /* Display-only fields joined from products / inventory_batches. */
    private String productName;
    private String sku;
    private String sellingUnit;
    private BigDecimal sellingPrice;
    private Long saleableQuantity;
    private Boolean sellableOnline;   // status/type/flag re-checked for display

    /** Line total for display: sellingPrice * quantity. */
    public BigDecimal getSubtotal() {
        if (sellingPrice == null || quantity == null) {
            return BigDecimal.ZERO;
        }
        return sellingPrice.multiply(BigDecimal.valueOf(quantity));
    }

    /** True when the requested quantity exceeds what is currently sellable. */
    public boolean isOverStock() {
        if (quantity == null || saleableQuantity == null) {
            return false;
        }
        return quantity > saleableQuantity;
    }

    /** True when the line can no longer be ordered online at all. */
    public boolean isUnavailable() {
        return !Boolean.TRUE.equals(sellableOnline);
    }

    public Long getCartItemId() {
        return cartItemId;
    }

    public void setCartItemId(Long cartItemId) {
        this.cartItemId = cartItemId;
    }

    public Long getCartId() {
        return cartId;
    }

    public void setCartId(Long cartId) {
        this.cartId = cartId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getSellingUnit() {
        return sellingUnit;
    }

    public void setSellingUnit(String sellingUnit) {
        this.sellingUnit = sellingUnit;
    }

    public BigDecimal getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(BigDecimal sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public Long getSaleableQuantity() {
        return saleableQuantity;
    }

    public void setSaleableQuantity(Long saleableQuantity) {
        this.saleableQuantity = saleableQuantity;
    }

    public Boolean getSellableOnline() {
        return sellableOnline;
    }

    public void setSellableOnline(Boolean sellableOnline) {
        this.sellableOnline = sellableOnline;
    }
}
