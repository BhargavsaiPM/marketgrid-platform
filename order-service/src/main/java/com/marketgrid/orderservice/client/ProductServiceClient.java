package com.marketgrid.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * OpenFeign client for product-service, resolved via Eureka.
 */
@FeignClient(name = "product-service")
public interface ProductServiceClient {

    /**
     * GET /api/products/{productId} — public endpoint on product-service.
     */
    @GetMapping("/api/products/{productId}")
    ProductResponse getProductById(@PathVariable("productId") Long productId);

    /**
     * PATCH /api/products/{productId}/stock — decrements product stock.
     * Requires ROLE_ADMIN authorization token.
     */
    @PatchMapping("/api/products/{productId}/stock")
    ProductResponse decreaseStock(@PathVariable("productId") Long productId,
                                  @RequestBody StockDecreaseRequest request,
                                  @RequestHeader("Authorization") String bearerToken);
}
