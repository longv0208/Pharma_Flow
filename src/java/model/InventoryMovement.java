package model;

import java.sql.Timestamp;

/**
 * Mirror of `inventory_movements` table. Wrapper types per rule.md §17.
 *
 * One row = one audit event on a batch: stock in, stock out, reservation,
 * adjustment, block, unblock. Quantity fields are always non-null in DB.
 *
 * Display-only fields (productName, sku, batchNumber, performedByName) are
 * populated by JOINs in the DAO for JSP rendering.
 */
public class InventoryMovement {

    private Long movementId;
    private Long batchId;
    private Long performedBy;
    private String movementType;    // NHAP_KHO | BAN_TAI_QUAY | GIU_HANG_ONLINE | ...
    private Integer onHandChange;
    private Integer reservedChange;
    private Integer onHandBefore;
    private Integer onHandAfter;
    private Integer reservedBefore;
    private Integer reservedAfter;
    private String referenceType;
    private Long referenceId;
    private String reason;
    private Timestamp createdAt;

    /* ---- display-only (not DB columns) ---- */
    private String productName;
    private String sku;
    private String batchNumber;
    private String performedByName;

    public Long getMovementId() {
        return movementId;
    }

    public void setMovementId(Long movementId) {
        this.movementId = movementId;
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

    public String getMovementType() {
        return movementType;
    }

    public void setMovementType(String movementType) {
        this.movementType = movementType;
    }

    public Integer getOnHandChange() {
        return onHandChange;
    }

    public void setOnHandChange(Integer onHandChange) {
        this.onHandChange = onHandChange;
    }

    public Integer getReservedChange() {
        return reservedChange;
    }

    public void setReservedChange(Integer reservedChange) {
        this.reservedChange = reservedChange;
    }

    public Integer getOnHandBefore() {
        return onHandBefore;
    }

    public void setOnHandBefore(Integer onHandBefore) {
        this.onHandBefore = onHandBefore;
    }

    public Integer getOnHandAfter() {
        return onHandAfter;
    }

    public void setOnHandAfter(Integer onHandAfter) {
        this.onHandAfter = onHandAfter;
    }

    public Integer getReservedBefore() {
        return reservedBefore;
    }

    public void setReservedBefore(Integer reservedBefore) {
        this.reservedBefore = reservedBefore;
    }

    public Integer getReservedAfter() {
        return reservedAfter;
    }

    public void setReservedAfter(Integer reservedAfter) {
        this.reservedAfter = reservedAfter;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Long referenceId) {
        this.referenceId = referenceId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
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

    /* ============ Derived view helpers ============ */
    /**
     * Human label — explicit Vietnamese text for each movement_type code.
     */
    public String getMovementLabel() {
        if (movementType == null) {
            return "";
        }
        switch (movementType) {
            case "NHAP_KHO":
                return "Nhập kho";
            case "BAN_TAI_QUAY":
                return "Bán tại quầy";
            case "GIU_HANG_ONLINE":
                return "Giữ hàng online";
            case "GIAI_PHONG_GIU_HANG":
                return "Giải phóng giữ hàng";
            case "BAN_ONLINE":
                return "Bán online";
            case "DIEU_CHINH":
                return "Điều chỉnh";
            case "DIEU_CHINH_KIEM_KE":
                return "Điều chỉnh kiểm kê";
            case "KHOA":
                return "Khóa";
            case "MO_KHOA":
                return "Mở khóa";
            default:
                return movementType;
        }
    }

    /**
     * CSS modifier — "NHAP_KHO" → "mv-nhap-kho".
     */
    public String getMovementCss() {
        if (movementType == null) {
            return "mv-other";
        }
        return "mv-" + movementType.toLowerCase().replace('_', '-');
    }

    /**
     * Short sign for quantity change display — "+50" or "-3" or "0".
     */
    public String getOnHandChangeLabel() {
        if (onHandChange == null) {
            return "0";
        }
        if (onHandChange > 0) {
            return "+" + onHandChange;
        }
        return String.valueOf(onHandChange);
    }

    public String getReservedChangeLabel() {
        if (reservedChange == null) {
            return "0";
        }
        if (reservedChange > 0) {
            return "+" + reservedChange;
        }
        return String.valueOf(reservedChange);
    }
}
