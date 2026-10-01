package model;

/**
 * Mirror of `customer_profiles` table.
 */
public class CustomerProfile {
    private Long customerId;
    private Long userId;
    private String provinceCity;
    private String district;
    private String ward;
    private String detailedAddress;

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long v) { this.customerId = v; }

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }

    public String getProvinceCity() { return provinceCity; }
    public void setProvinceCity(String v) { this.provinceCity = v; }

    public String getDistrict() { return district; }
    public void setDistrict(String v) { this.district = v; }

    public String getWard() { return ward; }
    public void setWard(String v) { this.ward = v; }

    public String getDetailedAddress() { return detailedAddress; }
    public void setDetailedAddress(String v) { this.detailedAddress = v; }
}
