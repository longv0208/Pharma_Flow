package model;

import java.math.BigDecimal;

/**
 * Mirror of `online_order_items` — one purchased line. unit_price / subtotal /
 * selling_unit are frozen copies of products at order time. productName/sku are
 * display-only JOIN fields for the order detail page.
 */
public class OnlineOrderItem {

    private Long onlineOrderItemId;
    private Long onlineOrderId;
    private Long productId;
    private Integer quantity;
    private String sellingUnit;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;

    /* Display-only fields joined from products. */
    private String productName;
    private String sku;

    public Long getOnlineOrderItemId() {
        return onlineOrderItemId;
    }

    public void setOnlineOrderItemId(Long onlineOrderItemId) {
        this.onlineOrderItemId = onlineOrderItemId;
    }

    public Long getOnlineOrderId() {
        return onlineOrderId;
    }

    public void setOnlineOrderId(Long onlineOrderId) {
        this.onlineOrderId = onlineOrderId;
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

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
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
}
