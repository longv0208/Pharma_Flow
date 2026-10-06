package model;

import java.math.BigDecimal;

/**
 * Mirror of `sale_items` — one product line of a completed sale. unit_price is
 * the price snapshot taken from products.selling_price at checkout time; it is
 * never re-read from the product afterwards (rule.md — backend is authoritative
 * for money).
 */
public class SaleItem {

    private Long saleItemId;
    private Long saleTransactionId;
    private Long productId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;

    /* Display-only fields from JOINs — not columns of sale_items. */
    private String productName;
    private String sku;
    private String productType;
    private String sellingUnit;

    public Long getSaleItemId() {
        return saleItemId;
    }

    public void setSaleItemId(Long saleItemId) {
        this.saleItemId = saleItemId;
    }

    public Long getSaleTransactionId() {
        return saleTransactionId;
    }

    public void setSaleTransactionId(Long saleTransactionId) {
        this.saleTransactionId = saleTransactionId;
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
}
