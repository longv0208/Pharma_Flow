package model;

import java.sql.Timestamp;

/** Mirror of `storage_racks`. */
public class StorageRack {

    private Long rackId;
    private Long zoneId;
    private String rackCode;
    private String rackName;
    private String status;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    private String zoneCode;
    private String zoneName;
    private Integer binCount;
    private Integer onHandTotal;

    public Long getRackId() { return rackId; }
    public void setRackId(Long rackId) { this.rackId = rackId; }
    public Long getZoneId() { return zoneId; }
    public void setZoneId(Long zoneId) { this.zoneId = zoneId; }
    public String getRackCode() { return rackCode; }
    public void setRackCode(String rackCode) { this.rackCode = rackCode; }
    public String getRackName() { return rackName; }
    public void setRackName(String rackName) { this.rackName = rackName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
    public String getZoneCode() { return zoneCode; }
    public void setZoneCode(String zoneCode) { this.zoneCode = zoneCode; }
    public String getZoneName() { return zoneName; }
    public void setZoneName(String zoneName) { this.zoneName = zoneName; }
    public Integer getBinCount() { return binCount; }
    public void setBinCount(Integer binCount) { this.binCount = binCount; }
    public Integer getOnHandTotal() { return onHandTotal; }
    public void setOnHandTotal(Integer onHandTotal) { this.onHandTotal = onHandTotal; }

    public boolean isActive() {
        return "HOAT_DONG".equals(status);
    }

    public String getStatusLabel() {
        return isActive() ? "Đang dùng" : "Ngừng dùng";
    }
}
