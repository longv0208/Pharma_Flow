package model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Mirror of `sale_transactions` — one POS counter sale. Status moves
 * PENDING -> COMPLETED inside the checkout transaction; a completed sale is
 * audit history and is never edited (rule.md §45).
 *
 * staff_id references staff_profiles.staff_id, NOT users.user_id — the DAO
 * resolves it during checkout.
 */
public class SaleTransaction {

    private Long saleTransactionId;
    private Long staffId;
    private Long prescriptionId;
    private String paymentMethod;    // CASH | BANK_TRANSFER | CARD
    private BigDecimal totalAmount;
    private String status;           // PENDING | COMPLETED | FAILED | CANCELLED
    private Timestamp saleDatetime;

    /* Display-only field from JOIN — not a column of sale_transactions. */
    private String staffName;

    /** Readable status label, e.g. COMPLETED -> "Completed". */
    public String getStatusLabel() {
        if (status == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        boolean cap = true;
        for (char c : status.toCharArray()) {
            if (c == '_') {
                sb.append(' ');
                cap = true;
            } else if (cap) {
                sb.append(Character.toUpperCase(c));
                cap = false;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }

    /** CSS modifier, e.g. COMPLETED -> "sale-completed". */
    public String getStatusCss() {
        if (status == null) {
            return "sale-other";
        }
        return "sale-" + status.toLowerCase().replace('_', '-');
    }

    /** Readable payment label, e.g. BANK_TRANSFER -> "Bank Transfer". */
    public String getPaymentLabel() {
        if (paymentMethod == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        boolean cap = true;
        for (char c : paymentMethod.toCharArray()) {
            if (c == '_') {
                sb.append(' ');
                cap = true;
            } else if (cap) {
                sb.append(Character.toUpperCase(c));
                cap = false;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
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
