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
    private String status;          // BAN_NHAP | DA_XAC_NHAN | CHAP_NHAN_MOT_PHAN | DA_HUY
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
     * Only BAN_NHAP receipts can be edited — enforced again in the servlet,
     * this is just for hiding buttons.
     */
    public boolean isEditable() {
        return "BAN_NHAP".equals(status);
    }

    /**
     * Only a BAN_NHAP receipt may be cancelled — confirmed receipts already
     * moved inventory and need a different (adjustment) workflow.
     */
    public boolean isCancellable() {
        return "BAN_NHAP".equals(status);
    }

    /**
     * A receipt is confirmable while still a draft and holding at least one
     * item — the servlet re-checks everything against the DB anyway.
     */
    public boolean isConfirmable() {
        return "BAN_NHAP".equals(status);
    }

    /**
     * Vietnamese status label for badges.
     */
    public String getStatusLabel() {
        if (status == null) {
            return "";
        }
        switch (status) {
            case "BAN_NHAP":
                return "Bản nháp";
            case "DA_XAC_NHAN":
                return "Đã xác nhận";
            case "CHAP_NHAN_MOT_PHAN":
                return "Chấp nhận một phần";
            case "DA_HUY":
                return "Đã hủy";
            default:
                return status;
        }
    }

    /**
     * CSS modifier for the status badge — "CHAP_NHAN_MOT_PHAN" →
     * "gr-chap-nhan-mot-phan". Matches the .gr-* rules in main.css.
     */
    public String getStatusCss() {
        if (status == null) {
            return "gr-ban-nhap";
        }
        return "gr-" + status.toLowerCase().replace('_', '-');
    }
}
