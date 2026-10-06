package model;

import java.sql.Timestamp;

/**
 * Mirror of `inventory_adjustments` — one manual correction to one batch's
 * on_hand_quantity. Audit-only: never edited or deleted after creation.
 */
public class InventoryAdjustment {

    private Long adjustmentId;
    private Long batchId;
    private Long performedBy;
    private Integer quantityChange;
    private Integer quantityBefore;
    private Integer quantityAfter;
    private String reason;          // DAMAGED | LOST | EXPIRED | COUNT_CORRECTION | DATA_CORRECTION | OTHER
    private String note;
    private Timestamp createdAt;

    /* Display-only fields from JOINs — not columns of inventory_adjustments. */
    private String productName;
    private String sku;
    private String batchNumber;
    private String performedByName;

    /** Title-case label from the enum, e.g. COUNT_CORRECTION -> "Count Correction". */
    public String getReasonLabel() {
        if (reason == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        boolean cap = true;
        for (char c : reason.toCharArray()) {
            if (c == '_') {
                sb.append(' ');
                cap = true;
            } else if (cap) {
                sb.append(Character.toUpperCase(c));
                cap = false;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }

    /** Signed quantity for display: +5 or -5. */
    public String getQuantityChangeLabel() {
        if (quantityChange == null) {
            return "";
        }
        if (quantityChange > 0) {
            return "+" + quantityChange;
        }
        return String.valueOf(quantityChange);
    }

    public Long getAdjustmentId() {
        return adjustmentId;
    }

    public void setAdjustmentId(Long adjustmentId) {
        this.adjustmentId = adjustmentId;
    }

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }

    public Long getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(Long performedBy) {
        this.performedBy = performedBy;
    }

    public Integer getQuantityChange() {
        return quantityChange;
    }

    public void setQuantityChange(Integer quantityChange) {
        this.quantityChange = quantityChange;
    }

    public Integer getQuantityBefore() {
        return quantityBefore;
    }

    public void setQuantityBefore(Integer quantityBefore) {
        this.quantityBefore = quantityBefore;
    }

    public Integer getQuantityAfter() {
        return quantityAfter;
    }

    public void setQuantityAfter(Integer quantityAfter) {
        this.quantityAfter = quantityAfter;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
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

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public String getPerformedByName() {
        return performedByName;
    }

    public void setPerformedByName(String performedByName) {
        this.performedByName = performedByName;
    }
}
