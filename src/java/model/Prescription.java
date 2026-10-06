package model;

import java.sql.Date;
import java.sql.Timestamp;

/**
 * Mirror of `prescriptions` — a doctor prescription looked up by code at POS
 * checkout. Only VALID prescriptions may be attached to a sale.
 */
public class Prescription {

    private Long prescriptionId;
    private String prescriptionCode;
    private Date prescriptionDate;
    private String healthcareFacility;
    private String prescriber;
    private String patientName;
    private String validationStatus; // VALID | INVALID
    private Long validatedBy;
    private Timestamp validatedAt;
    private Timestamp createdAt;

    public Long getPrescriptionId() {
        return prescriptionId;
    }

    public void setPrescriptionId(Long prescriptionId) {
        this.prescriptionId = prescriptionId;
    }

    public String getPrescriptionCode() {
        return prescriptionCode;
    }

    public void setPrescriptionCode(String prescriptionCode) {
        this.prescriptionCode = prescriptionCode;
    }

    public Date getPrescriptionDate() {
        return prescriptionDate;
    }

    public void setPrescriptionDate(Date prescriptionDate) {
        this.prescriptionDate = prescriptionDate;
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

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getValidationStatus() {
        return validationStatus;
    }

    public void setValidationStatus(String validationStatus) {
        this.validationStatus = validationStatus;
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
