package model;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

/**
 * Mirror of `purchase_orders` table. Wrapper types per rule.md §17.
 *
 * Display-only fields (supplierName, createdByName, items) are populated by
 * JOIN / follow-up queries in the DAO for JSP rendering — they are not real
 * columns.
 */
public class PurchaseOrder {

    private Long purchaseOrderId;
    private Long supplierId;
    private Long createdBy;
    private Date orderDate;
    private Date expectedDeliveryDate;
    private String status;          // DRAFT | ORDERED | PARTIALLY_RECEIVED | RECEIVED | CANCELLED
    private String sourceType;      // MANUAL | LOW_STOCK | AI_SUGGESTION
    private BigDecimal totalAmount;
    private String note;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    /* ---- display-only (not DB columns) ---- */
    private String supplierName;
    private String createdByName;
    private List<PurchaseOrderItem> items;

    public Long getPurchaseOrderId() {
        return purchaseOrderId;
    }

    public void setPurchaseOrderId(Long purchaseOrderId) {
        this.purchaseOrderId = purchaseOrderId;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Date getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(Date orderDate) {
        this.orderDate = orderDate;
    }

    public Date getExpectedDeliveryDate() {
        return expectedDeliveryDate;
    }

    public void setExpectedDeliveryDate(Date expectedDeliveryDate) {
        this.expectedDeliveryDate = expectedDeliveryDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
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

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public List<PurchaseOrderItem> getItems() {
        return items;
    }

    public void setItems(List<PurchaseOrderItem> items) {
        this.items = items;
    }

    /* ============ Derived view helpers (used by JSP EL) ============ */
    /**
     * Only DRAFT orders can be edited — enforced again in the servlet, this is
     * just for hiding buttons.
     */
    public boolean isEditable() {
        return "DRAFT".equals(status);
    }

    /**
     * DRAFT and ORDERED (with nothing received) can be cancelled. Servlet still
     * re-checks received_quantity in the DB.
     */
    public boolean isCancellable() {
        return "DRAFT".equals(status) || "ORDERED".equals(status);
    }

    /**
     * Human label for badges — "PARTIALLY_RECEIVED" → "Partially Received".
     */
    public String getStatusLabel() {
        if (status == null) {
            return "";
        }
        String label = status.replace('_', ' ').toLowerCase();
        StringBuilder sb = new StringBuilder(label.length());
        boolean cap = true;
        for (int i = 0; i < label.length(); i++) {
            char ch = label.charAt(i);
            if (cap && Character.isLetter(ch)) {
                sb.append(Character.toUpperCase(ch));
                cap = false;
            } else {
                sb.append(ch);
            }
            if (ch == ' ') {
                cap = true;
            }
        }
        return sb.toString();
    }

    /**
     * CSS modifier for the status badge — "PARTIALLY_RECEIVED" →
     * "po-partially-received". Matches the .po-* rules in main.css.
     */
    public String getStatusCss() {
        if (status == null) {
            return "po-draft";
        }
        return "po-" + status.toLowerCase().replace('_', '-');
    }
}
