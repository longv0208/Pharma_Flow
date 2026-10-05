package model;

/**
 * Mirror of products.product_type ENUM('OTC','RX','RESTRICTED'). Only OTC may
 * be sold online.
 */
public enum ProductType {
    OTC, RX, RESTRICTED;

    public static ProductType fromString(String value) {
        if (value == null) {
            return OTC;
        }
        try {
            return ProductType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return OTC;
        }
    }
}
