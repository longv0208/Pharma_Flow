package model;

import java.math.BigDecimal;

/**
 * Mirror of `products` table. `availableQuantity` is derived from
 * `inventory_batches` (not a real column). Wrapper types per rule.md §17 — no
 * primitives in entities.
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
    private String shortDescription;
    private String indication;
    private String usageInstruction;
    private String warnings;
    private String contraindications;
    private ProductType productType;
    private String sellingUnit;
    private BigDecimal sellingPrice;
    private Boolean onlineSaleAllowed;
    private String status;
    private Long availableQuantity;
    private String categoryName;   // populated via JOIN when admin list needs it

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
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

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getActiveIngredient() {
        return activeIngredient;
    }

    public void setActiveIngredient(String activeIngredient) {
        this.activeIngredient = activeIngredient;
    }

    public String getStrength() {
        return strength;
    }

    public void setStrength(String strength) {
        this.strength = strength;
    }

    public String getDosageForm() {
        return dosageForm;
    }

    public void setDosageForm(String dosageForm) {
        this.dosageForm = dosageForm;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getIndication() {
        return indication;
    }

    public void setIndication(String indication) {
        this.indication = indication;
    }

    public String getUsageInstruction() {
        return usageInstruction;
    }

    public void setUsageInstruction(String usageInstruction) {
        this.usageInstruction = usageInstruction;
    }

    public String getWarnings() {
        return warnings;
    }

    public void setWarnings(String warnings) {
        this.warnings = warnings;
    }

    public String getContraindications() {
        return contraindications;
    }

    public void setContraindications(String contraindications) {
        this.contraindications = contraindications;
    }

    public ProductType getProductType() {
        return productType;
    }

    public void setProductType(ProductType productType) {
        this.productType = productType;
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

    public Boolean getOnlineSaleAllowed() {
        return onlineSaleAllowed;
    }

    public void setOnlineSaleAllowed(Boolean onlineSaleAllowed) {
        this.onlineSaleAllowed = onlineSaleAllowed;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Long availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    /* ============ Derived view helpers (used by JSP EL) ============ */
    public boolean isInStock() {
        return availableQuantity != null && availableQuantity > 0;
    }

    /**
     * Sale rule per SRS: only OTC + online_sale_allowed + in stock can be added
     * to cart.
     */
    public boolean isPurchasable() {
        return Boolean.TRUE.equals(onlineSaleAllowed)
                && productType == ProductType.OTC
                && isInStock();
    }

    /**
     * Short badge text shown on the product card, or null when none applies.
     */
    public String getDisplayBadge() {
        if (productType == ProductType.RX) {
            return "Rx";
        }
        if (productType == ProductType.RESTRICTED) {
            return "Restricted";
        }
        if (!isInStock()) {
            return "Out of Stock";
        }
        return null;
    }
}
