package model;

import java.math.BigDecimal;
import java.sql.Date;

/**
 * Mirror of `goods_receipt_items` table. Wrapper types per rule.md §17.
 *
 * One row = one delivered batch of one PO line. A single PO item can have
 * several rows when the supplier delivers it in multiple batches.
 *
 * productName / sku are display-only JOIN fields for the JSP.
 */
public class GoodsReceiptItem {

    private Long goodsReceiptItemId;
    private Long goodsReceiptId;
    private Long purchaseOrderItemId;
    private Long productId;
    private Long batchId;
    private String batchNumber;
    private Date expiryDate;
    private Integer quantity;
    private BigDecimal costPrice;
    private String inspectionResult;    // CHO_KIEM_TRA | CHAP_NHAN | TU_CHOI
    private String rejectionReason;

    /* ---- display-only (not DB columns) ---- */
    private String productName;
    private String sku;

    /* ---- transient form helpers (used only on the receive form) ----
       Filled from the parent purchase_order_items row so the form can show
       Ordered / Received / Remaining context next to the line. */
    private Integer orderedQuantity;
    private Integer previouslyReceived;

    public Long getGoodsReceiptItemId() {
        return goodsReceiptItemId;
    }

    public void setGoodsReceiptItemId(Long goodsReceiptItemId) {
        this.goodsReceiptItemId = goodsReceiptItemId;
    }

    public Long getGoodsReceiptId() {
        return goodsReceiptId;
    }

    public void setGoodsReceiptId(Long goodsReceiptId) {
        this.goodsReceiptId = goodsReceiptId;
    }

    public Long getPurchaseOrderItemId() {
        return purchaseOrderItemId;
    }

    public void setPurchaseOrderItemId(Long purchaseOrderItemId) {
        this.purchaseOrderItemId = purchaseOrderItemId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
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

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public String getInspectionResult() {
        return inspectionResult;
    }

    public void setInspectionResult(String inspectionResult) {
        this.inspectionResult = inspectionResult;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
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

    public Integer getOrderedQuantity() {
        return orderedQuantity;
    }

    public void setOrderedQuantity(Integer orderedQuantity) {
        this.orderedQuantity = orderedQuantity;
    }

    public Integer getPreviouslyReceived() {
        return previouslyReceived;
    }

    public void setPreviouslyReceived(Integer previouslyReceived) {
        this.previouslyReceived = previouslyReceived;
    }

    /* ============ Derived view helpers ============ */
    /**
     * remaining = ordered - already received. Used on the receive form to cap
     * how much can still be accepted for this PO line.
     */
    public Integer getRemainingQuantity() {
        if (orderedQuantity == null || previouslyReceived == null) {
            return null;
        }
        return orderedQuantity - previouslyReceived;
    }

    /** Vietnamese badge label for each inspection result code. */
    public String getInspectionLabel() {
        if (inspectionResult == null) {
            return "";
        }
        switch (inspectionResult) {
            case "CHO_KIEM_TRA":
                return "Chờ kiểm tra";
            case "CHAP_NHAN":
                return "Chấp nhận";
            case "TU_CHOI":
                return "Từ chối";
            default:
                return inspectionResult;
        }
    }

    /** CSS modifier for the inspection badge — "TU_CHOI" → "insp-tu-choi". */
    public String getInspectionCss() {
        if (inspectionResult == null) {
            return "insp-cho-kiem-tra";
        }
        return "insp-" + inspectionResult.toLowerCase().replace('_', '-');
    }
}
