package model;

import java.sql.Timestamp;

/**
 * Mirror of `inventory_alert_settings` — the threshold configuration row.
 * Only the GLOBAL row (product_id IS NULL) is used by this project;
 * product_id stays null for it.
 */
public class InventoryAlertSetting {

    private Long settingId;
    private Long productId;
    private Integer minimumStockLevel;
    private Integer nearExpiryWarningDays;
    private Long updatedBy;
    private Timestamp updatedAt;

    public Long getSettingId() {
        return settingId;
    }

    public void setSettingId(Long settingId) {
        this.settingId = settingId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getMinimumStockLevel() {
        return minimumStockLevel;
    }

    public void setMinimumStockLevel(Integer minimumStockLevel) {
        this.minimumStockLevel = minimumStockLevel;
    }

    public Integer getNearExpiryWarningDays() {
        return nearExpiryWarningDays;
    }

    public void setNearExpiryWarningDays(Integer nearExpiryWarningDays) {
        this.nearExpiryWarningDays = nearExpiryWarningDays;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
