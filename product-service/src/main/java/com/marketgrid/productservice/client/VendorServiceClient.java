package com.marketgrid.productservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * OpenFeign client for vendor-service. Resolved via Eureka service discovery.
 *
 * <p>Used to verify that a vendor exists and is approved before allowing
 * product creation. Two methods are provided:</p>
 * <ul>
 *   <li>{@code getVendorById} — public endpoint, no auth needed</li>
 *   <li>{@code getMyVendor} — authenticated endpoint, JWT propagated
 *       via {@link com.marketgrid.productservice.config.FeignClientInterceptor}</li>
 * </ul>
 */
@FeignClient(name = "vendor-service")
public interface VendorServiceClient {

    /**
     * GET /api/vendors/{vendorId} — public endpoint on vendor-service.
     * Returns the vendor profile by its primary key.
     */
    @GetMapping("/api/vendors/{vendorId}")
    VendorResponse getVendorById(@PathVariable("vendorId") Long vendorId);

    /**
     * GET /api/vendors/me — authenticated endpoint on vendor-service.
     * Returns the vendor profile for the currently authenticated user.
     * Requires the Authorization header to be forwarded (handled by FeignClientInterceptor).
     */
    @GetMapping("/api/vendors/me")
    VendorResponse getMyVendor();
}
