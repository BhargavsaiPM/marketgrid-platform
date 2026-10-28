package com.marketgrid.vendorservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /api/vendors/register and PUT /api/vendors/me.
 */
public class VendorRequest {

    @NotBlank(message = "Business name cannot be blank")
    @Size(max = 255, message = "Business name cannot exceed 255 characters")
    private String businessName;

    @Size(max = 1000, message = "Business description cannot exceed 1000 characters")
    private String businessDescription;

    @Size(max = 50, message = "Contact phone cannot exceed 50 characters")
    private String contactPhone;

    @Size(max = 500, message = "Address cannot exceed 500 characters")
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
