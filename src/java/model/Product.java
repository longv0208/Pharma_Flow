package model;

import java.math.BigDecimal;

/**
 * Mirror of `products` table.
 * `availableQuantity` is derived from `inventory_batches` (not a real column).
 * Wrapper types per rule.md §17 — no primitives in entities.
 *
 * Derived helpers (isInStock / isPurchasable / getDisplayBadge) are view-only
 * conveniences used by JSP EL — they don't mutate state.
 */
public class Product {
    private Long productId;
    private Long categoryId;
    private String productName;
    private String sku;
    private String barcode;
    private String activeIngredient;
    private String strength;
    private String dosageForm;
    private String manufacturer;
    private String registrationNumber;
    private ProductType productType;
    private String sellingUnit;
    private BigDecimal sellingPrice;
    private Boolean onlineSaleAllowed;
    private String status;
    private Long availableQuantity;

    public Long getProductId() { return productId; }
    public void setProductId(Long v) { this.productId = v; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long v) { this.categoryId = v; }

    public String getProductName() { return productName; }
    public void setProductName(String v) { this.productName = v; }

    public String getSku() { return sku; }
    public void setSku(String v) { this.sku = v; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String v) { this.barcode = v; }

    public String getActiveIngredient() { return activeIngredient; }
    public void setActiveIngredient(String v) { this.activeIngredient = v; }

    public String getStrength() { return strength; }
    public void setStrength(String v) { this.strength = v; }

    public String getDosageForm() { return dosageForm; }
    public void setDosageForm(String v) { this.dosageForm = v; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String v) { this.manufacturer = v; }

    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String v) { this.registrationNumber = v; }

    public ProductType getProductType() { return productType; }
    public void setProductType(ProductType v) { this.productType = v; }

    public String getSellingUnit() { return sellingUnit; }
    public void setSellingUnit(String v) { this.sellingUnit = v; }

    public BigDecimal getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(BigDecimal v) { this.sellingPrice = v; }

    public Boolean getOnlineSaleAllowed() { return onlineSaleAllowed; }
    public void setOnlineSaleAllowed(Boolean v) { this.onlineSaleAllowed = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    public Long getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(Long v) { this.availableQuantity = v; }

    /* ============ Derived view helpers (used by JSP EL) ============ */

    public boolean isInStock() {
        return availableQuantity != null && availableQuantity > 0;
    }

    /** Sale rule per SRS: only OTC + online_sale_allowed + in stock can be added to cart. */
    public boolean isPurchasable() {
        return Boolean.TRUE.equals(onlineSaleAllowed)
                && productType == ProductType.OTC
                && isInStock();
    }

    /** Short badge text shown on the product card, or null when none applies. */
    public String getDisplayBadge() {
        if (productType == ProductType.RX)         return "Rx";
        if (productType == ProductType.RESTRICTED) return "Restricted";
        if (!isInStock())                          return "Out of Stock";
        return null;
    }
}
