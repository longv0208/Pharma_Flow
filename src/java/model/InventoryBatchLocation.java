package model;

import java.sql.Date;
import java.sql.Timestamp;

/**
 * Mirror of `inventory_batch_locations` — qty of one batch at one bin.
 * Dual-write with inventory_batches totals (sum of locations).
 */
public class InventoryBatchLocation {

    private Long batchLocationId;
    private Long batchId;
    private Long binId;
    private Integer onHandQuantity;
    private Integer reservedQuantity;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    /* Display JOINs */
    private String batchNumber;
    private Date expiryDate;
    private String batchStatus;
    private Long productId;
    private String productName;
    private String sku;
    private String zoneCode;
    private String rackCode;
    private String binCode;
    private Long zoneId;
    private Long rackId;

    public Integer getAvailableQuantity() {
        int oh = onHandQuantity != null ? onHandQuantity : 0;
        int rs = reservedQuantity != null ? reservedQuantity : 0;
        return oh - rs;
    }

    public Long getBatchLocationId() { return batchLocationId; }
    public void setBatchLocationId(Long batchLocationId) { this.batchLocationId = batchLocationId; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public Long getBinId() { return binId; }
    public void setBinId(Long binId) { this.binId = binId; }
    public Integer getOnHandQuantity() { return onHandQuantity; }
    public void setOnHandQuantity(Integer onHandQuantity) { this.onHandQuantity = onHandQuantity; }
    public Integer getReservedQuantity() { return reservedQuantity; }
    public void setReservedQuantity(Integer reservedQuantity) { this.reservedQuantity = reservedQuantity; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }
    public Date getExpiryDate() { return expiryDate; }
    public void setExpiryDate(Date expiryDate) { this.expiryDate = expiryDate; }
    public String getBatchStatus() { return batchStatus; }
    public void setBatchStatus(String batchStatus) { this.batchStatus = batchStatus; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getZoneCode() { return zoneCode; }
    public void setZoneCode(String zoneCode) { this.zoneCode = zoneCode; }
    public String getRackCode() { return rackCode; }
    public void setRackCode(String rackCode) { this.rackCode = rackCode; }
    public String getBinCode() { return binCode; }
    public void setBinCode(String binCode) { this.binCode = binCode; }
    public Long getZoneId() { return zoneId; }
    public void setZoneId(Long zoneId) { this.zoneId = zoneId; }
    public Long getRackId() { return rackId; }
    public void setRackId(Long rackId) { this.rackId = rackId; }

    public String getLocationLabel() {
        return (zoneCode != null ? zoneCode : "?")
                + " / " + (rackCode != null ? rackCode : "?")
                + " / " + (binCode != null ? binCode : "?");
    }
}
