package model;

public class Category {
    private Long categoryId;
    private String categoryName;
    private String description;
    private String status;

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long v) { this.categoryId = v; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String v) { this.categoryName = v; }

    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
}
