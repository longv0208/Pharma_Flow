package model;

/**
 * Mirror of `prescription_items` — one prescribed drug line of a prescription.
 * product_id links the line to a catalog product when the drug is stocked.
 */
public class PrescriptionItem {

    private Long prescriptionItemId;
    private Long prescriptionId;
    private Long productId;
    private String drugName;
    private String strength;
    private Integer prescribedQuantity;
    private String usageInstruction;

    public Long getPrescriptionItemId() {
        return prescriptionItemId;
    }

    public void setPrescriptionItemId(Long prescriptionItemId) {
        this.prescriptionItemId = prescriptionItemId;
    }

    public Long getPrescriptionId() {
        return prescriptionId;
    }

    public void setPrescriptionId(Long prescriptionId) {
        this.prescriptionId = prescriptionId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getDrugName() {
        return drugName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }

    public String getStrength() {
        return strength;
    }

    public void setStrength(String strength) {
        this.strength = strength;
    }

    public Integer getPrescribedQuantity() {
        return prescribedQuantity;
    }

    public void setPrescribedQuantity(Integer prescribedQuantity) {
        this.prescribedQuantity = prescribedQuantity;
    }

    public String getUsageInstruction() {
        return usageInstruction;
    }

    public void setUsageInstruction(String usageInstruction) {
        this.usageInstruction = usageInstruction;
    }
}
