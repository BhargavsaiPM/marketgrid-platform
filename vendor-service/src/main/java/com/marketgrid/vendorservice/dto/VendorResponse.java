package com.marketgrid.vendorservice.dto;

import com.marketgrid.vendorservice.entity.Vendor;

import java.time.LocalDateTime;

/**
 * Public-facing projection of a {@link Vendor}.
 */
public class VendorResponse {

    private Long id;
    private Long userId;
    private String businessName;
    private String businessDescription;
    private String contactPhone;
    private String address;
    private boolean approved;
    private LocalDateTime createdAt;

    public VendorResponse() {
    }

    public static VendorResponse fromEntity(Vendor vendor) {
        VendorResponse r = new VendorResponse();
        r.id = vendor.getId();
        r.userId = vendor.getUserId();
        r.businessName = vendor.getBusinessName();
        r.businessDescription = vendor.getBusinessDescription();
        r.contactPhone = vendor.getContactPhone();
        r.address = vendor.getAddress();
        r.approved = vendor.isApproved();
        r.createdAt = vendor.getCreatedAt();
        return r;
    }

    // ---- Getters & Setters ----

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
