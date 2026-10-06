package model;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

/**
 * Mirror of `inventory_batches` table. Wrapper types per rule.md §17.
 *
 * One row = one physical batch of one product sitting in the pharmacy. Created
 * (or topped up) only when a Goods Receipt line is ACCEPTED.
 */
public class InventoryBatch {

    private Long batchId;
    private Long productId;
    private Long supplierId;
    private Long sourceGoodsReceiptId;
    private String batchNumber;
    private Date expiryDate;
    private Integer onHandQuantity;
    private Integer reservedQuantity;
    private BigDecimal costPrice;
    private String storageLocation;
    private String status;          // AVAILABLE | NEAR_EXPIRY | EXPIRED | BLOCKED | OUT_OF_STOCK
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public Long getSourceGoodsReceiptId() {
        return sourceGoodsReceiptId;
    }

    public void setSourceGoodsReceiptId(Long sourceGoodsReceiptId) {
        this.sourceGoodsReceiptId = sourceGoodsReceiptId;
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

    public Integer getOnHandQuantity() {
        return onHandQuantity;
    }

    public void setOnHandQuantity(Integer onHandQuantity) {
        this.onHandQuantity = onHandQuantity;
    }

    public Integer getReservedQuantity() {
        return reservedQuantity;
    }

    public void setReservedQuantity(Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public String getStorageLocation() {
        return storageLocation;
    }

    public void setStorageLocation(String storageLocation) {
        this.storageLocation = storageLocation;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
