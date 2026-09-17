package com.marketgrid.productservice.client;

import java.time.LocalDateTime;

/**
 * Minimal DTO mirroring the JSON returned by vendor-service's
 * {@code GET /api/vendors/me} endpoint. Only the fields needed
 * by product-service are included.
 */
public class VendorResponse {

    private Long id;
    private Long userId;
    private String businessName;
    private boolean approved;
    private LocalDateTime createdAt;

    public VendorResponse() {
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
