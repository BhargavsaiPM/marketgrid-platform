package com.marketgrid.vendorservice.dto;

/**
 * Request body for POST /api/vendors/register and PUT /api/vendors/me.
 */
public class VendorRequest {

    private String businessName;
    private String businessDescription;
    private String contactPhone;
    private String address;

    public VendorRequest() {
    }

    public VendorRequest(String businessName, String businessDescription,
                         String contactPhone, String address) {
        this.businessName = businessName;
        this.businessDescription = businessDescription;
        this.contactPhone = contactPhone;
        this.address = address;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getBusinessDescription() {
        return businessDescription;
    }

    public void setBusinessDescription(String businessDescription) {
        this.businessDescription = businessDescription;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
