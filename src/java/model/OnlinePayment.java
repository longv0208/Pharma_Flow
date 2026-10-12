package model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Mirror of `online_payments` — one online-payment attempt row attached to an
 * online order. Created together with the order for THANH_TOAN_TRUC_TUYEN
 * (currently the only provider is VNPAY); COD orders have no row here.
 *
 * Status: CHO_XU_LY | THANH_CONG | THAT_BAI.
 */
public class OnlinePayment {

    private Long paymentId;
    private Long onlineOrderId;
    private String provider;
    private String transactionReference;
    private BigDecimal amount;
    private String paymentStatus;
    private Timestamp paidAt;
    private Timestamp createdAt;

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public Long getOnlineOrderId() {
        return onlineOrderId;
    }

    public void setOnlineOrderId(Long onlineOrderId) {
        this.onlineOrderId = onlineOrderId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public Timestamp getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Timestamp paidAt) {
        this.paidAt = paidAt;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
