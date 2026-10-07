package model;

import java.sql.Timestamp;

/**
 * Mirror of `inventory_reservations` — the FEFO allocation table linking one
 * online_order_item to the batch(es) its quantity is reserved from. Status:
 * DANG_GIU | DA_GIAI_PHONG | DA_HOAN_TAT — this module only writes DANG_GIU and
 * flips to DA_GIAI_PHONG on cancel; DA_HOAN_TAT belongs to fulfillment.
 */
public class InventoryReservation {

    private Long reservationId;
    private Long onlineOrderId;
    private Long onlineOrderItemId;
    private Long batchId;
    private Integer reservedQuantity;
    private String status;
    private Timestamp reservedAt;
    private Timestamp releasedAt;

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public Long getOnlineOrderId() {
        return onlineOrderId;
    }

    public void setOnlineOrderId(Long onlineOrderId) {
        this.onlineOrderId = onlineOrderId;
    }

    public Long getOnlineOrderItemId() {
        return onlineOrderItemId;
    }

    public void setOnlineOrderItemId(Long onlineOrderItemId) {
        this.onlineOrderItemId = onlineOrderItemId;
    }

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }

    public Integer getReservedQuantity() {
        return reservedQuantity;
    }

    public void setReservedQuantity(Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getReservedAt() {
        return reservedAt;
    }

    public void setReservedAt(Timestamp reservedAt) {
        this.reservedAt = reservedAt;
    }

    public Timestamp getReleasedAt() {
        return releasedAt;
    }

    public void setReleasedAt(Timestamp releasedAt) {
        this.releasedAt = releasedAt;
    }
}
