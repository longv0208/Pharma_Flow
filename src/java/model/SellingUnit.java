package model;

/**
 * Allowed product selling units. Stored in products.selling_unit as the
 * Vietnamese label so existing rows (Hộp, Chai, Tuýp) stay valid.
 */
public enum SellingUnit {
    HOP("Hộp"),
    VI("Vỉ"),
    CHAI("Chai"),
    TUYP("Tuýp"),
    LO("Lọ"),
    GOI("Gói"),
    VIEN("Viên"),
    ONG("Ống");

    private final String label;

    SellingUnit(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Match a stored label. Null when the value is not in the allowlist. */
    public static SellingUnit fromLabel(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        for (SellingUnit unit : values()) {
            if (unit.label.equalsIgnoreCase(trimmed)) {
                return unit;
            }
        }
        return null;
    }
}
