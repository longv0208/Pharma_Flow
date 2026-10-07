package model;

import java.sql.Timestamp;

/**
 * Mirror of `prescriptions` — an audit record that a staff member manually
 * checked an external paper prescription during an RX POS sale. The system
 * does NOT verify a prescription code or prescribed quantities; it only stores
 * who checked, when, and which doctor/facility issued the paper.
 *
 * Created inside the sale transaction for KE_DON sales only; a
 * KHONG_KE_DON-only sale leaves sale_transactions.prescription_id NULL.
 */
public class Prescription {

    private Long prescriptionId;
    private String healthcareFacility;
    private String prescriber;
    private Long validatedBy;      // users.user_id of the staff who checked
    private Timestamp validatedAt;
    private Timestamp createdAt;

    public Long getPrescriptionId() {
        return prescriptionId;
    }

    public void setPrescriptionId(Long prescriptionId) {
        this.prescriptionId = prescriptionId;
    }

    public String getHealthcareFacility() {
        return healthcareFacility;
    }

    public void setHealthcareFacility(String healthcareFacility) {
        this.healthcareFacility = healthcareFacility;
    }

    public String getPrescriber() {
        return prescriber;
    }

    public void setPrescriber(String prescriber) {
        this.prescriber = prescriber;
    }

    public Long getValidatedBy() {
        return validatedBy;
    }

    public void setValidatedBy(Long validatedBy) {
        this.validatedBy = validatedBy;
    }

    public Timestamp getValidatedAt() {
        return validatedAt;
    }

    public void setValidatedAt(Timestamp validatedAt) {
        this.validatedAt = validatedAt;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
