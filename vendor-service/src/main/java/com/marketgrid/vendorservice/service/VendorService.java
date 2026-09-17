package com.marketgrid.vendorservice.service;

import com.marketgrid.vendorservice.entity.Vendor;
import com.marketgrid.vendorservice.repository.VendorRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Core business logic for vendor profile management.
 */
@Service
public class VendorService {

    private final VendorRepository vendorRepository;

    public VendorService(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }

    /**
     * Create a new vendor profile linked to the given userId.
     *
     * @throws IllegalArgumentException if a vendor profile already exists for this userId
     */
    public Vendor registerVendor(Long userId, String businessName,
                                 String businessDescription,
                                 String contactPhone, String address) {
        if (vendorRepository.existsByUserId(userId)) {
            throw new IllegalArgumentException(
                    "A vendor profile already exists for userId: " + userId);
        }

        Vendor vendor = new Vendor(userId, businessName, businessDescription,
                contactPhone, address);
        return vendorRepository.save(vendor);
    }

    /**
     * Retrieve a vendor by its primary key.
     *
     * @throws IllegalArgumentException if not found
     */
    public Vendor getVendorById(Long vendorId) {
        return vendorRepository.findById(vendorId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Vendor not found with id: " + vendorId));
    }

    /**
     * Retrieve the vendor profile belonging to a specific user.
     *
     * @throws IllegalArgumentException if not found
     */
    public Vendor getVendorByUserId(Long userId) {
        return vendorRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No vendor profile found for userId: " + userId));
    }

    /**
     * Update a vendor's mutable fields. Only allowed if the requesting userId
     * owns the vendor profile.
     *
     * @throws IllegalArgumentException if vendorId not found
     * @throws SecurityException        if the requesting user doesn't own the vendor
     */
    public Vendor updateVendor(Long vendorId, Long requestingUserId,
                               String businessName, String businessDescription,
                               String contactPhone, String address) {
        Vendor vendor = getVendorById(vendorId);

        if (!vendor.getUserId().equals(requestingUserId)) {
            throw new SecurityException("You do not own this vendor profile");
        }

        if (businessName != null)        vendor.setBusinessName(businessName);
        if (businessDescription != null) vendor.setBusinessDescription(businessDescription);
        if (contactPhone != null)        vendor.setContactPhone(contactPhone);
        if (address != null)             vendor.setAddress(address);

        return vendorRepository.save(vendor);
    }

    /**
     * List all approved vendors (for customer-facing browsing).
     */
    public List<Vendor> getAllApprovedVendors() {
        return vendorRepository.findByIsApprovedTrue();
    }

    /**
     * Approve a vendor profile by id.
     *
     * @param vendorId the vendor ID to approve
     * @return the updated Vendor entity
     * @throws IllegalArgumentException if vendor not found
     */
    public Vendor approveVendor(Long vendorId) {
        Vendor vendor = getVendorById(vendorId);
        vendor.setApproved(true);
        return vendorRepository.save(vendor);
    }

    /**
     * Reject a vendor profile by id.
     *
     * @param vendorId the vendor ID to reject
     * @return the updated Vendor entity
     * @throws IllegalArgumentException if vendor not found
     */
    public Vendor rejectVendor(Long vendorId) {
        Vendor vendor = getVendorById(vendorId);
        vendor.setApproved(false);
        return vendorRepository.save(vendor);
    }

    /**
     * List all pending vendors (where isApproved is false).
     *
     * @return list of pending vendors
     */
    public List<Vendor> getAllPendingVendors() {
        return vendorRepository.findByIsApprovedFalse();
    }
}

