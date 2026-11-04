package model;

import java.sql.Timestamp;

/** Mirror of `storage_bins`. */
public class StorageBin {

    private Long binId;
    private Long rackId;
    private String binCode;
    private String status;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    private Long zoneId;
    private String zoneCode;
    private String zoneName;
    private String rackCode;
    private String rackName;
    private Integer onHandTotal;
    private Integer reservedTotal;

    public Long getBinId() { return binId; }
    public void setBinId(Long binId) { this.binId = binId; }
    public Long getRackId() { return rackId; }
    public void setRackId(Long rackId) { this.rackId = rackId; }
    public String getBinCode() { return binCode; }
    public void setBinCode(String binCode) { this.binCode = binCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
    public Long getZoneId() { return zoneId; }
    public void setZoneId(Long zoneId) { this.zoneId = zoneId; }
    public String getZoneCode() { return zoneCode; }
    public void setZoneCode(String zoneCode) { this.zoneCode = zoneCode; }
    public String getZoneName() { return zoneName; }
    public void setZoneName(String zoneName) { this.zoneName = zoneName; }
    public String getRackCode() { return rackCode; }
    public void setRackCode(String rackCode) { this.rackCode = rackCode; }
    public String getRackName() { return rackName; }
    public void setRackName(String rackName) { this.rackName = rackName; }
    public Integer getOnHandTotal() { return onHandTotal; }
    public void setOnHandTotal(Integer onHandTotal) { this.onHandTotal = onHandTotal; }
    public Integer getReservedTotal() { return reservedTotal; }
    public void setReservedTotal(Integer reservedTotal) { this.reservedTotal = reservedTotal; }

    public boolean isActive() {
        return "HOAT_DONG".equals(status);
    }

    public String getStatusLabel() {
        return isActive() ? "Đang dùng" : "Ngừng dùng";
    }

    /** Display path e.g. TMP / UNASSIGNED / BIN-UNKNOWN */
    public String getFullCode() {
        String z = zoneCode != null ? zoneCode : "?";
        String r = rackCode != null ? rackCode : "?";
        String b = binCode != null ? binCode : "?";
        return z + " / " + r + " / " + b;
    }
}
