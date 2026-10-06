package model;

import java.sql.Date;

/**
 * Mirror of `stocktake_items` — one batch's counting line inside a stocktake.
 * system_quantity is the snapshot taken at Start, refreshed to the locked
 * on_hand at Complete; actual_quantity is NULL until the batch is counted
 * (blank is NOT the same as 0).
 */
public class StocktakeItem {

    private Long stocktakeItemId;
    private Long stocktakeId;
    private Long batchId;
    private Integer systemQuantity;
    private Integer actualQuantity;      // NULL = not counted yet
    private Integer differenceQuantity;

    /* Display-only fields from JOINs — not columns of stocktake_items. */
    private String productName;
    private String sku;
    private String batchNumber;
    private Date expiryDate;
    private String batchStatus;
    private Integer reservedQuantity;

    /** True once staff entered a physical count (0 counts as counted). */
    public boolean isCounted() {
        return actualQuantity != null;
    }

    /** Signed difference for display: +2 / -3 / 0, or "" when not counted. */
    public String getDifferenceLabel() {
        if (differenceQuantity == null) {
            return "";
        }
        if (differenceQuantity > 0) {
            return "+" + differenceQuantity;
        }
        return String.valueOf(differenceQuantity);
    }

    public Long getStocktakeItemId() {
        return stocktakeItemId;
    }

    public void setStocktakeItemId(Long stocktakeItemId) {
        this.stocktakeItemId = stocktakeItemId;
    }

    public Long getStocktakeId() {
        return stocktakeId;
    }

    public void setStocktakeId(Long stocktakeId) {
        this.stocktakeId = stocktakeId;
    }

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }

    public Integer getSystemQuantity() {
        return systemQuantity;
    }

    public void setSystemQuantity(Integer systemQuantity) {
        this.systemQuantity = systemQuantity;
    }

    public Integer getActualQuantity() {
        return actualQuantity;
    }

    public void setActualQuantity(Integer actualQuantity) {
        this.actualQuantity = actualQuantity;
    }

    public Integer getDifferenceQuantity() {
        return differenceQuantity;
    }

    public void setDifferenceQuantity(Integer differenceQuantity) {
        this.differenceQuantity = differenceQuantity;
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

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getBatchStatus() {
        return batchStatus;
    }

    public void setBatchStatus(String batchStatus) {
        this.batchStatus = batchStatus;
    }

    public Integer getReservedQuantity() {
        return reservedQuantity;
    }

    public void setReservedQuantity(Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }
}
