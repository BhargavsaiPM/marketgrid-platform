package com.marketgrid.orderservice.client;

/**
 * Minimal projection of vendor profile from vendor-service.
 */
public class VendorResponse {

    private Long id;
    private Long userId;
    private String businessName;
    private boolean approved;

    public VendorResponse() {
    }

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

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }
}
