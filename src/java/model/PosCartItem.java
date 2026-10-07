package model;

import java.math.BigDecimal;

/**
 * One line of the POS session cart. NOT a database entity — the cart lives only
 * in the HTTP session (a LinkedHashMap of productId -> PosCartItem) so nothing
 * cart-related ever touches the online-order tables. Price/name/sku are cached
 * for display; the real price is re-read from `products` inside the checkout
 * transaction, so a stale display price never reaches sale_items.
 */
public class PosCartItem {

    private Long productId;
    private String productName;
    private String sku;
    private String productType;   // KHONG_KE_DON | KE_DON | HAN_CHE — drives the Rx panel
    private String sellingUnit;
    private BigDecimal unitPrice; // display copy; DB price wins at checkout
    private Integer quantity;
    private Long saleableQuantity; // last known sellable stock, display only

    /** line total for display: unitPrice * quantity. */
    public BigDecimal getSubtotal() {
        if (unitPrice == null || quantity == null) {
            return BigDecimal.ZERO;
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public boolean isRx() {
        return "KE_DON".equals(productType);
    }

    public boolean isRestricted() {
        return "HAN_CHE".equals(productType);
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
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

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public String getSellingUnit() {
        return sellingUnit;
    }

    public void setSellingUnit(String sellingUnit) {
        this.sellingUnit = sellingUnit;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Long getSaleableQuantity() {
        return saleableQuantity;
    }

    public void setSaleableQuantity(Long saleableQuantity) {
        this.saleableQuantity = saleableQuantity;
    }
}
