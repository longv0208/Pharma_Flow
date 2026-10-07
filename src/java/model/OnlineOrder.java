package model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Mirror of `online_orders` — one customer online order. The customer_* /
 * address columns are a per-order delivery snapshot (editable at checkout,
 * never synced back to customer_profiles).
 *
 * Status: CHO_XU_LY | DA_XAC_NHAN | DANG_CHUAN_BI | SAN_SANG | DANG_GIAO |
 * HOAN_TAT | DA_HUY | TU_CHOI — this module only creates CHO_XU_LY and moves
 * CHO_XU_LY -> DA_HUY on customer cancel; the rest belongs to fulfillment.
 */
public class OnlineOrder {

    private Long onlineOrderId;
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private String provinceCity;
    private String district;
    private String ward;
    private String detailedAddress;
    private String paymentMethod;    // THANH_TOAN_KHI_NHAN_HANG | THANH_TOAN_TRUC_TUYEN
    private String paymentStatus;    // CHO_THANH_TOAN | DA_THANH_TOAN | THAT_BAI | KHONG_YEU_CAU
    private BigDecimal totalAmount;
    private String orderStatus;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    /** Vietnamese order-status label, e.g. CHO_XU_LY -> "Chờ xử lý". */
    public String getStatusLabel() {
        if (orderStatus == null) {
            return "";
        }
        switch (orderStatus) {
            case "CHO_XU_LY":
                return "Chờ xử lý";
            case "DA_XAC_NHAN":
                return "Đã xác nhận";
            case "DANG_CHUAN_BI":
                return "Đang chuẩn bị";
            case "SAN_SANG":
                return "Sẵn sàng";
            case "DANG_GIAO":
                return "Đang giao";
            case "HOAN_TAT":
                return "Hoàn tất";
            case "DA_HUY":
                return "Đã hủy";
            case "TU_CHOI":
                return "Từ chối";
            default:
                return orderStatus;
        }
    }

    /** CSS modifier, e.g. CHO_XU_LY -> "order-cho-xu-ly". */
    public String getStatusCss() {
        if (orderStatus == null) {
            return "order-other";
        }
        return "order-" + orderStatus.toLowerCase().replace('_', '-');
    }

    /** Vietnamese payment-method label, e.g. THANH_TOAN_KHI_NHAN_HANG -> COD text. */
    public String getPaymentLabel() {
        if (paymentMethod == null) {
            return "";
        }
        switch (paymentMethod) {
            case "THANH_TOAN_KHI_NHAN_HANG":
                return "Thanh toán khi nhận hàng";
            case "THANH_TOAN_TRUC_TUYEN":
                return "Thanh toán trực tuyến";
            default:
                return paymentMethod;
        }
    }

    /** Vietnamese payment-status label, e.g. KHONG_YEU_CAU -> "Không yêu cầu". */
    public String getPaymentStatusLabel() {
        if (paymentStatus == null) {
            return "";
        }
        switch (paymentStatus) {
            case "CHO_THANH_TOAN":
                return "Chờ thanh toán";
            case "DA_THANH_TOAN":
                return "Đã thanh toán";
            case "THAT_BAI":
                return "Thất bại";
            case "KHONG_YEU_CAU":
                return "Không yêu cầu";
            default:
                return paymentStatus;
        }
    }

    /** Customer may cancel only while the order waits for staff processing. */
    public boolean isCancellable() {
        return "CHO_XU_LY".equals(orderStatus);
    }

    /** Full delivery address in one line for the detail page. */
    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        if (detailedAddress != null && !detailedAddress.isEmpty()) {
            sb.append(detailedAddress);
        }
        if (ward != null && !ward.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(ward);
        }
        if (district != null && !district.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(district);
        }
        if (provinceCity != null && !provinceCity.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(provinceCity);
        }
        return sb.toString();
    }

    public Long getOnlineOrderId() {
        return onlineOrderId;
    }

    public void setOnlineOrderId(Long onlineOrderId) {
        this.onlineOrderId = onlineOrderId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getProvinceCity() {
        return provinceCity;
    }

    public void setProvinceCity(String provinceCity) {
        this.provinceCity = provinceCity;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getWard() {
        return ward;
    }

    public void setWard(String ward) {
        this.ward = ward;
    }

    public String getDetailedAddress() {
        return detailedAddress;
    }

    public void setDetailedAddress(String detailedAddress) {
        this.detailedAddress = detailedAddress;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
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
