package model;

import java.sql.Timestamp;

/**
 * Mirror of `stocktakes` — one physical counting session. Status moves only
 * forward: DRAFT -> IN_PROGRESS -> COMPLETED. Audit-only once completed:
 * never edited, reopened or deleted.
 */
public class Stocktake {

    private Long stocktakeId;
    private Long createdBy;
    private String status;          // DRAFT | IN_PROGRESS | COMPLETED
    private Timestamp createdAt;
    private Timestamp completedAt;

    /* Display-only fields from JOINs/aggregates — not columns of stocktakes. */
    private String createdByName;
    private Integer itemCount;        // total stocktake_items rows
    private Integer countedCount;     // items with actual_quantity set
    private Integer differenceCount;  // items with difference_quantity != 0

    /** Readable status label, e.g. IN_PROGRESS -> "In Progress". */
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

    /** CSS modifier, e.g. IN_PROGRESS -> "st-in-progress". */
    public String getStatusCss() {
        if (status == null) {
            return "st-other";
        }
        return "st-" + status.toLowerCase().replace('_', '-');
    }

    /** List-page action label: DRAFT opens, IN_PROGRESS continues, else view. */
    public String getActionLabel() {
        if ("DRAFT".equals(status)) {
            return "Open";
        }
        if ("IN_PROGRESS".equals(status)) {
            return "Continue";
        }
        return "View";
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
