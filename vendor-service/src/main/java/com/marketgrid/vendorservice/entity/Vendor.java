package com.marketgrid.vendorservice.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Vendor profile linked to a user account in user-service via {@code userId}.
 */
@Entity
@Table(name = "vendors")
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** References the User (in user-service) who owns this vendor account. */
    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    private String businessName;

    @Column(length = 1000)
    private String businessDescription;

    private String contactPhone;

    private String address;

    @Column(nullable = false)
    private boolean isApproved = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Vendor() {
    }

    public Vendor(Long userId, String businessName, String businessDescription,
                  String contactPhone, String address) {
        this.userId = userId;
        this.businessName = businessName;
        this.businessDescription = businessDescription;
        this.contactPhone = contactPhone;
        this.address = address;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
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
        return isApproved;
    }

    public void setApproved(boolean approved) {
        isApproved = approved;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
