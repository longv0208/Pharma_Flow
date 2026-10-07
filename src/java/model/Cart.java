package model;

import java.sql.Timestamp;

/**
 * Mirror of `carts` — one logical shopping cart per customer. A customer has at
 * most one DANG_HOAT_DONG cart at a time (enforced by CartDAO.getOrCreateActiveCart,
 * not a DB constraint). Status: DANG_HOAT_DONG | DA_CHUYEN_THANH_DON | DA_XOA.
 */
public class Cart {

    private Long cartId;
    private Long customerId;
    private String status;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Long getCartId() {
        return cartId;
    }

    public void setCartId(Long cartId) {
        this.cartId = cartId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
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
