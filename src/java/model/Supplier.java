package model;

/**
 * Mirror of `suppliers` table.
 */
public class Supplier {
    private Long supplierId;
    private String supplierName;
    private String contactPerson;
    private String phone;
    private String email;
    private String address;
    private String taxBusinessInfo;
    private String status;

    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long v) { this.supplierId = v; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String v) { this.supplierName = v; }

    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String v) { this.contactPerson = v; }

    public String getPhone() { return phone; }
    public void setPhone(String v) { this.phone = v; }

    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }

    public String getAddress() { return address; }
    public void setAddress(String v) { this.address = v; }

    public String getTaxBusinessInfo() { return taxBusinessInfo; }
    public void setTaxBusinessInfo(String v) { this.taxBusinessInfo = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
}
