package model;

import java.sql.Timestamp;

/** Mirror of `storage_zones`. */
public class StorageZone {

    private Long zoneId;
    private String zoneCode;
    private String zoneName;
    private String status; // HOAT_DONG | NGUNG_HOAT_DONG
    private Timestamp createdAt;
    private Timestamp updatedAt;

    /* Display aggregates */
    private Integer rackCount;
    private Integer onHandTotal;

    public Long getZoneId() { return zoneId; }
    public void setZoneId(Long zoneId) { this.zoneId = zoneId; }
    public String getZoneCode() { return zoneCode; }
    public void setZoneCode(String zoneCode) { this.zoneCode = zoneCode; }
    public String getZoneName() { return zoneName; }
    public void setZoneName(String zoneName) { this.zoneName = zoneName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
    public Integer getRackCount() { return rackCount; }
    public void setRackCount(Integer rackCount) { this.rackCount = rackCount; }
    public Integer getOnHandTotal() { return onHandTotal; }
    public void setOnHandTotal(Integer onHandTotal) { this.onHandTotal = onHandTotal; }

    public boolean isActive() {
        return "HOAT_DONG".equals(status);
    }

    public String getStatusLabel() {
        return isActive() ? "Đang dùng" : "Ngừng dùng";
    }
}
