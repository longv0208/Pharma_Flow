package model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Mirror of `sale_transactions` — one POS counter sale. Status moves
 * CHO_XU_LY -> HOAN_TAT inside the checkout transaction; a completed sale is
 * audit history and is never edited (rule.md §45).
 *
 * staff_id references staff_profiles.staff_id, NOT users.user_id — the DAO
 * resolves it during checkout.
 */
public class SaleTransaction {

    private Long saleTransactionId;
    private Long staffId;
    private Long prescriptionId;
    private String paymentMethod;    // TIEN_MAT | CHUYEN_KHOAN | THE
    private BigDecimal totalAmount;
    private String status;           // CHO_XU_LY | HOAN_TAT | THAT_BAI | DA_HUY
    private Timestamp saleDatetime;

    /* Display-only field from JOIN — not a column of sale_transactions. */
    private String staffName;

    /** Vietnamese status label, e.g. HOAN_TAT -> "Hoàn tất". */
    public String getStatusLabel() {
        if (status == null) {
            return "";
        }
        switch (status) {
            case "CHO_XU_LY":
                return "Chờ xử lý";
            case "HOAN_TAT":
                return "Hoàn tất";
            case "THAT_BAI":
                return "Thất bại";
            case "DA_HUY":
                return "Đã hủy";
            default:
                return status;
        }
    }

    /** CSS modifier, e.g. HOAN_TAT -> "sale-hoan-tat". */
    public String getStatusCss() {
        if (status == null) {
            return "sale-other";
        }
        return "sale-" + status.toLowerCase().replace('_', '-');
    }

    /** Vietnamese payment label, e.g. CHUYEN_KHOAN -> "Chuyển khoản". */
    public String getPaymentLabel() {
        if (paymentMethod == null) {
            return "";
        }
        switch (paymentMethod) {
            case "TIEN_MAT":
                return "Tiền mặt";
            case "CHUYEN_KHOAN":
                return "Chuyển khoản";
            case "THE":
                return "Thẻ";
            default:
                return paymentMethod;
        }
    }

    public Long getSaleTransactionId() {
        return saleTransactionId;
    }

    public void setSaleTransactionId(Long saleTransactionId) {
        this.saleTransactionId = saleTransactionId;
    }

    public Long getStaffId() {
        return staffId;
    }

    public void setStaffId(Long staffId) {
        this.staffId = staffId;
    }

    public Long getPrescriptionId() {
        return prescriptionId;
    }

    public void setPrescriptionId(Long prescriptionId) {
        this.prescriptionId = prescriptionId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getSaleDatetime() {
        return saleDatetime;
    }

    public void setSaleDatetime(Timestamp saleDatetime) {
        this.saleDatetime = saleDatetime;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }
}
