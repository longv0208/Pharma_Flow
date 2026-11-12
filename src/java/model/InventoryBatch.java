package model;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Mirror of `inventory_batches` table. Wrapper types per rule.md §17.
 *
 * One row = one physical batch of one product sitting in the pharmacy. Created
 * (or topped up) only when a Goods Receipt line is ACCEPTED.
 */
public class InventoryBatch {

    /** Near-expiry window in days — batches inside it display as SAP_HET_HAN. */
    public static final int NEAR_EXPIRY_DAYS = 90;

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
    private String status;          // CO_SAN | SAP_HET_HAN | HET_HAN | BI_KHOA | HET_HANG
    private Timestamp createdAt;
    private Timestamp updatedAt;

    /* Display-only fields from JOINs — not columns of inventory_batches. */
    private String productName;
    private String sku;
    private String supplierName;

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

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    /**
     * Status from the expiry date alone. HET_HAN when the calendar day is
     * today or earlier, SAP_HET_HAN within {@link #NEAR_EXPIRY_DAYS}, otherwise
     * CO_SAN. A null date is CO_SAN. Callers still apply zero-stock and BI_KHOA.
     * Compares calendar dates, so the time of day on {@code today} does not
     * move a batch across the boundary.
     */
    public static String statusForExpiry(Date expiryDate, Date today) {
        if (expiryDate == null || today == null) {
            return "CO_SAN";
        }
        LocalDate expiry = expiryDate.toLocalDate();
        LocalDate day = today.toLocalDate();
        if (!expiry.isAfter(day)) {
            return "HET_HAN";
        }
        long diffDays = ChronoUnit.DAYS.between(day, expiry);
        if (diffDays <= NEAR_EXPIRY_DAYS) {
            return "SAP_HET_HAN";
        }
        return "CO_SAN";
    }

    /**
     * Label to show. The stored status is a cache and is not rewritten on read.
     * A past expiry wins over CO_SAN / SAP_HET_HAN / HET_HANG. BI_KHOA stays
     * blocked. Zero stock that is still in date is HET_HANG.
     */
    public String getEffectiveStatus() {
        if ("BI_KHOA".equals(status)) {
            return "BI_KHOA";
        }
        String fromExpiry = statusForExpiry(expiryDate, today());
        if ("HET_HAN".equals(fromExpiry)) {
            return "HET_HAN";
        }
        int onHand = onHandQuantity == null ? 0 : onHandQuantity;
        if (onHand <= 0) {
            return "HET_HANG";
        }
        return fromExpiry;
    }

    /**
     * Units that can still be sold from this batch. Matches
     * InventoryDAO.ALLOCATABLE: cached status CO_SAN or SAP_HET_HAN, and the
     * expiry date is still after today. Expired, blocked, and empty batches
     * contribute 0 even when the status cache has not flipped.
     */
    public int getSaleableQuantity() {
        if (!"CO_SAN".equals(status) && !"SAP_HET_HAN".equals(status)) {
            return 0;
        }
        if (expiryDate == null || "HET_HAN".equals(statusForExpiry(expiryDate, today()))) {
            return 0;
        }
        int onHand = onHandQuantity == null ? 0 : onHandQuantity;
        int reserved = reservedQuantity == null ? 0 : reservedQuantity;
        int available = onHand - reserved;
        if (available < 0) {
            return 0;
        }
        return available;
    }

    /**
     * Date badge next to the expiry field. EXPIRED / NEAR_EXPIRY from the
     * calendar date, independent of the cached status and of BI_KHOA.
     */
    public String getExpiryWarning() {
        String fromExpiry = statusForExpiry(expiryDate, today());
        if ("HET_HAN".equals(fromExpiry)) {
            return "EXPIRED";
        }
        if ("SAP_HET_HAN".equals(fromExpiry)) {
            return "NEAR_EXPIRY";
        }
        return null;
    }

    private static Date today() {
        return Date.valueOf(LocalDate.now());
    }
}
