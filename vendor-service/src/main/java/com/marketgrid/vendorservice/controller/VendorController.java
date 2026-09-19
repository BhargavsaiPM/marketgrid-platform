package com.marketgrid.vendorservice.controller;

import com.marketgrid.vendorservice.dto.VendorRequest;
import com.marketgrid.vendorservice.dto.VendorResponse;
import com.marketgrid.vendorservice.entity.Vendor;
import com.marketgrid.vendorservice.service.VendorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for vendor profile management.
 */
@Tag(name = "Vendor Management", description = "Vendor registration, profile management, and administrative approvals")
@RestController
@RequestMapping("/api/vendors")
public class VendorController {

    private final VendorService vendorService;

    public VendorController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    // -----------------------------------------------------------
    // Helper: extract userId stashed by JwtAuthenticationFilter
    // -----------------------------------------------------------
    private Long extractUserId(Authentication auth) {
        return (Long) auth.getDetails();
    }

    private boolean hasRole(Authentication auth, String role) {
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_" + role));
    }

    /**
     * POST /api/vendors/register — create a vendor profile.
     * Only users with role VENDOR may call this. The userId comes from the JWT,
     * not the request body, to prevent spoofing.
     */
    @Operation(summary = "Register a new vendor profile (VENDOR role required)")
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody VendorRequest request,
                                      Authentication authentication) {
        // Role check — must be VENDOR
        if (!hasRole(authentication, "VENDOR")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only users with the VENDOR role can register a vendor profile"));
        }

        try {
            Long userId = extractUserId(authentication);
            Vendor vendor = vendorService.registerVendor(
                    userId,
                    request.getBusinessName(),
                    request.getBusinessDescription(),
                    request.getContactPhone(),
                    request.getAddress()
            );
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(VendorResponse.fromEntity(vendor));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * GET /api/vendors/me — return the authenticated user's own vendor profile.
     */
    @Operation(summary = "Get current authenticated vendor profile")
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        try {
            Long userId = extractUserId(authentication);
            Vendor vendor = vendorService.getVendorByUserId(userId);
            return ResponseEntity.ok(VendorResponse.fromEntity(vendor));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * PUT /api/vendors/me — update the authenticated user's vendor profile.
     */
    @Operation(summary = "Update current authenticated vendor profile")
    @PutMapping("/me")
    public ResponseEntity<?> updateMe(@RequestBody VendorRequest request,
                                      Authentication authentication) {
        try {
            Long userId = extractUserId(authentication);
            Vendor vendor = vendorService.getVendorByUserId(userId);
            Vendor updated = vendorService.updateVendor(
                    vendor.getId(), userId,
                    request.getBusinessName(),
                    request.getBusinessDescription(),
                    request.getContactPhone(),
                    request.getAddress()
            );
            return ResponseEntity.ok(VendorResponse.fromEntity(updated));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", ex.getMessage()));
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * DELETE /api/vendors/me — delete the authenticated user's vendor profile.
     */
    @Operation(summary = "Delete current authenticated vendor profile")
    @DeleteMapping("/me")
    public ResponseEntity<?> deleteMe(Authentication authentication) {
        try {
            Long userId = extractUserId(authentication);
            Vendor vendor = vendorService.getVendorByUserId(userId);
            vendorService.deleteVendor(vendor.getId(), userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", ex.getMessage()));
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * GET /api/vendors/{vendorId} — public endpoint for browsing a single vendor.
     */
    @Operation(summary = "Get public vendor profile by vendor ID")
    @GetMapping("/{vendorId}")
    public ResponseEntity<?> getVendor(@PathVariable Long vendorId) {
        try {
            Vendor vendor = vendorService.getVendorById(vendorId);
            return ResponseEntity.ok(VendorResponse.fromEntity(vendor));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * GET /api/vendors — public endpoint listing all approved vendors.
     */
    @Operation(summary = "List all approved vendors")
    @GetMapping
    public ResponseEntity<List<VendorResponse>> getAllVendors() {
        List<VendorResponse> vendors = vendorService.getAllApprovedVendors().stream()
                .map(VendorResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(vendors);
    }

    /**
     * PUT /api/vendors/{vendorId}/approve — approve a vendor profile.
     * Only accessible by users with ROLE_ADMIN.
     */
    @Operation(summary = "Approve a vendor profile (ADMIN role required)")
    @PutMapping("/{vendorId}/approve")
    public ResponseEntity<?> approveVendor(@PathVariable Long vendorId,
                                           Authentication authentication) {
        if (!hasRole(authentication, "ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only users with the ADMIN role can approve a vendor"));
        }

        try {
            Vendor vendor = vendorService.approveVendor(vendorId);
            return ResponseEntity.ok(VendorResponse.fromEntity(vendor));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * PUT /api/vendors/{vendorId}/reject — reject a vendor profile.
     * Only accessible by users with ROLE_ADMIN.
     */
    @Operation(summary = "Reject a vendor profile (ADMIN role required)")
    @PutMapping("/{vendorId}/reject")
    public ResponseEntity<?> rejectVendor(@PathVariable Long vendorId,
                                          Authentication authentication) {
        if (!hasRole(authentication, "ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only users with the ADMIN role can reject a vendor"));
        }

        try {
            Vendor vendor = vendorService.rejectVendor(vendorId);
            return ResponseEntity.ok(VendorResponse.fromEntity(vendor));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * GET /api/vendors/pending — list all pending (unapproved) vendors.
     * Only accessible by users with ROLE_ADMIN.
     */
    @Operation(summary = "List all pending vendor profiles (ADMIN role required)")
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingVendors(Authentication authentication) {
        if (!hasRole(authentication, "ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only users with the ADMIN role can view pending vendors"));
        }

        List<VendorResponse> pendingVendors = vendorService.getAllPendingVendors().stream()
                .map(VendorResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(pendingVendors);
    }
}

