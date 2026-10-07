package model;

import java.sql.Timestamp;

/**
 * Mirror of `stocktakes` — one physical counting session. Status moves only
 * forward: BAN_NHAP -> DANG_KIEM_KE -> HOAN_TAT. Audit-only once completed:
 * never edited, reopened or deleted.
 */
public class Stocktake {

    private Long stocktakeId;
    private Long createdBy;
    private String status;          // BAN_NHAP | DANG_KIEM_KE | HOAN_TAT
    private Timestamp createdAt;
    private Timestamp completedAt;

    /* Display-only fields from JOINs/aggregates — not columns of stocktakes. */
    private String createdByName;
    private Integer itemCount;        // total stocktake_items rows
    private Integer countedCount;     // items with actual_quantity set
    private Integer differenceCount;  // items with difference_quantity != 0

    /** Vietnamese status label, e.g. DANG_KIEM_KE -> "Đang kiểm kê". */
    public String getStatusLabel() {
        if (status == null) {
            return "";
        }
        switch (status) {
            case "BAN_NHAP":
                return "Bản nháp";
            case "DANG_KIEM_KE":
                return "Đang kiểm kê";
            case "HOAN_TAT":
                return "Hoàn tất";
            default:
                return status;
        }
    }

    /** CSS modifier, e.g. DANG_KIEM_KE -> "st-dang-kiem-ke". */
    public String getStatusCss() {
        if (status == null) {
            return "st-other";
        }
        return "st-" + status.toLowerCase().replace('_', '-');
    }

    /** List-page action label: BAN_NHAP opens, DANG_KIEM_KE continues, else view. */
    public String getActionLabel() {
        if ("BAN_NHAP".equals(status)) {
            return "Mở";
        }
        if ("DANG_KIEM_KE".equals(status)) {
            return "Tiếp tục";
        }
        return "Xem";
    }

    public Long getStocktakeId() {
        return stocktakeId;
    }

    public void setStocktakeId(Long stocktakeId) {
        this.stocktakeId = stocktakeId;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
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

    public Timestamp getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Timestamp completedAt) {
        this.completedAt = completedAt;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public Integer getItemCount() {
        return itemCount;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
    }

    public Integer getCountedCount() {
        return countedCount;
    }

    public void setCountedCount(Integer countedCount) {
        this.countedCount = countedCount;
    }

    public Integer getDifferenceCount() {
        return differenceCount;
    }

    public void setDifferenceCount(Integer differenceCount) {
        this.differenceCount = differenceCount;
    }
}
