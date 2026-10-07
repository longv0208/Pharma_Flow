package model;

import java.sql.Date;

/**
 * One calculated inventory alert — display/read model only.
 * Nothing is persisted: alerts are recomputed from live inventory on every
 * GET /inventory/alerts (see InventoryAlertDAO).
 *
 * Stock alerts (OUT_OF_STOCK, LOW_STOCK) are product-level → batchId null.
 * Expiry alerts (NEAR_EXPIRY, EXPIRED) are batch-level → productId set too.
 */
public class InventoryAlert {

    private String alertType;      // OUT_OF_STOCK | LOW_STOCK | NEAR_EXPIRY | EXPIRED
    private String severity;       // Critical | High | Warning
    private Long productId;
    private String productName;
    private String sku;
    private Long batchId;
    private String batchNumber;
    private Integer quantity;      // product: saleable available; batch: on_hand
    private Integer threshold;     // product alerts only: minimum_stock_level
    private Date expiryDate;       // batch alerts only
    private Integer daysToExpiry;  // batch alerts only — negative when expired
    private String message;

    /** Vietnamese label for each alert type code (internal, not a DB enum). */
    public String getAlertTypeLabel() {
        if (alertType == null) {
            return "";
        }
        switch (alertType) {
            case "OUT_OF_STOCK":
                return "Hết hàng";
            case "LOW_STOCK":
                return "Tồn kho thấp";
            case "NEAR_EXPIRY":
                return "Sắp hết hạn";
            case "EXPIRED":
                return "Hết hạn";
            default:
                return alertType;
        }
    }

    /** CSS modifier, e.g. OUT_OF_STOCK -> "al-out-of-stock". */
    public String getAlertCss() {
        if (alertType == null) {
            return "al-other";
        }
        return "al-" + alertType.toLowerCase().replace('_', '-');
    }

    /** Severity badge CSS, e.g. Critical -> "al-sev-critical". */
    public String getSeverityCss() {
        if (severity == null) {
            return "al-sev-warning";
        }
        return "al-sev-" + severity.toLowerCase();
    }

    /** True for batch-level alerts (View Batch), false for product-level. */
    public boolean isBatchLevel() {
        return batchId != null;
    }

    /** Human-readable expiry detail, e.g. "Còn 57 ngày" / "Đã hết hạn 5 ngày trước". */
    public String getExpiryDetail() {
        if (daysToExpiry == null) {
            return "";
        }
        if (daysToExpiry < 0) {
            return "Đã hết hạn " + (-daysToExpiry) + " ngày trước";
        }
        return "Còn " + daysToExpiry + " ngày";
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
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

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getThreshold() {
        return threshold;
    }

    public void setThreshold(Integer threshold) {
        this.threshold = threshold;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Integer getDaysToExpiry() {
        return daysToExpiry;
    }

    public void setDaysToExpiry(Integer daysToExpiry) {
        this.daysToExpiry = daysToExpiry;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
