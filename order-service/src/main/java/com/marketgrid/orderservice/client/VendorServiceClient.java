package com.marketgrid.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * OpenFeign client for vendor-service.
 */
@FeignClient(name = "vendor-service")
public interface VendorServiceClient {

    /**
     * GET /api/vendors/me — returns the vendor profile of the authenticated user.
     */
    @GetMapping("/api/vendors/me")
    VendorResponse getMyVendor(@RequestHeader("Authorization") String bearerToken);
}
