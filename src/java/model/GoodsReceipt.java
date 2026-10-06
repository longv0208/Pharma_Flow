package model;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

/**
 * Mirror of `goods_receipts` table. Wrapper types per rule.md §17.
 *
 * A goods receipt records what the supplier actually delivered and what the
 * pharmacy inspected — it is NOT the same as the purchase order (what was
 * ordered) or the inventory batch (what entered stock).
 *
 * Display-only fields (supplierName, receivedByName, items) are populated by
 * JOIN / follow-up queries in the DAO for JSP rendering.
 */
public class GoodsReceipt {

    private Long goodsReceiptId;
    private Long supplierId;
    private Long purchaseOrderId;
    private Long receivedBy;
    private Date receiptDate;
    private String invoiceNumber;
    private String note;
    private String status;          // DRAFT | CONFIRMED | PARTIALLY_ACCEPTED | CANCELLED
    private Timestamp createdAt;
    private Timestamp updatedAt;

    /* ---- display-only (not DB columns) ---- */
    private String supplierName;
    private String receivedByName;
    private List<GoodsReceiptItem> items;

    public Long getGoodsReceiptId() {
        return goodsReceiptId;
    }

    public void setGoodsReceiptId(Long goodsReceiptId) {
        this.goodsReceiptId = goodsReceiptId;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public Long getPurchaseOrderId() {
        return purchaseOrderId;
    }

    public void setPurchaseOrderId(Long purchaseOrderId) {
        this.purchaseOrderId = purchaseOrderId;
    }

    public Long getReceivedBy() {
        return receivedBy;
    }

    public void setReceivedBy(Long receivedBy) {
        this.receivedBy = receivedBy;
    }

    public Date getReceiptDate() {
        return receiptDate;
    }

    public void setReceiptDate(Date receiptDate) {
        this.receiptDate = receiptDate;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
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

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getReceivedByName() {
        return receivedByName;
    }

    public void setReceivedByName(String receivedByName) {
        this.receivedByName = receivedByName;
    }

    public List<GoodsReceiptItem> getItems() {
        return items;
    }

    public void setItems(List<GoodsReceiptItem> items) {
        this.items = items;
    }

    /* ============ Derived view helpers (used by JSP EL) ============ */
    /**
     * Only DRAFT receipts can be edited — enforced again in the servlet, this
     * is just for hiding buttons.
     */
    public boolean isEditable() {
        return "DRAFT".equals(status);
    }

    /**
     * Only a DRAFT receipt may be cancelled — confirmed receipts already moved
     * inventory and need a different (adjustment) workflow.
     */
    public boolean isCancellable() {
        return "DRAFT".equals(status);
    }

    /**
     * A receipt is confirmable while still a draft and holding at least one
     * item — the servlet re-checks everything against the DB anyway.
     */
    public boolean isConfirmable() {
        return "DRAFT".equals(status);
    }

    /**
     * Human label for badges — "PARTIALLY_ACCEPTED" → "Partially Accepted".
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
     * CSS modifier for the status badge — "PARTIALLY_ACCEPTED" →
     * "gr-partially-accepted". Matches the .gr-* rules in main.css.
     */
    public String getStatusCss() {
        if (status == null) {
            return "gr-draft";
        }
        return "gr-" + status.toLowerCase().replace('_', '-');
    }
}
