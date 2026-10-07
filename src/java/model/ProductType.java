package model;

/**
 * Mirror of products.product_type ENUM('KHONG_KE_DON','KE_DON','HAN_CHE').
 * Only KHONG_KE_DON may be sold online.
 */
public enum ProductType {
    KHONG_KE_DON, KE_DON, HAN_CHE;

    public static ProductType fromString(String value) {
        if (value == null) {
            return KHONG_KE_DON;
        }
        try {
            return ProductType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return KHONG_KE_DON;
        }
    }
}
