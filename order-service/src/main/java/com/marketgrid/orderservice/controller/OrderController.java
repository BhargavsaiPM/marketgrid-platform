package com.marketgrid.orderservice.controller;

import com.marketgrid.orderservice.client.VendorResponse;
import com.marketgrid.orderservice.client.VendorServiceClient;
import com.marketgrid.orderservice.dto.OrderResponse;
import com.marketgrid.orderservice.exception.ServiceUnavailableException;
import com.marketgrid.orderservice.service.OrderService;
import feign.FeignException;
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
 * REST controller for checkout and order tracking operations.
 */
@Tag(name = "Order Management", description = "Customer checkout, order tracking, and vendor order fulfillment")
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final VendorServiceClient vendorServiceClient;

    public OrderController(OrderService orderService,
                           VendorServiceClient vendorServiceClient) {
        this.orderService = orderService;
        this.vendorServiceClient = vendorServiceClient;
    }

    private Long extractUserId(Authentication auth) {
        if (auth == null || auth.getDetails() == null) {
            throw new SecurityException("User authentication details not found");
        }
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
     * POST /api/orders/checkout — checks out the user's cart.
     */
    @Operation(summary = "Checkout cart, decrement stock, and create order")
    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(Authentication authentication) {
        try {
            Long userId = extractUserId(authentication);
            OrderResponse order = orderService.checkout(userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(order);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
        } catch (ServiceUnavailableException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * GET /api/orders — returns the authenticated user's order history.
     */
    @Operation(summary = "Get order history for current authenticated customer")
    @GetMapping
    public ResponseEntity<?> getOrders(Authentication authentication) {
        try {
            Long userId = extractUserId(authentication);
            List<OrderResponse> orders = orderService.getOrdersByUser(userId);
            return ResponseEntity.ok(orders);
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * GET /api/orders/vendor/mine — returns orders containing items for the authenticated vendor.
     */
    @Operation(summary = "Get orders containing items for the authenticated vendor (VENDOR role required)")
    @GetMapping("/vendor/mine")
    public ResponseEntity<?> getVendorOrders(Authentication authentication,
                                             @RequestHeader("Authorization") String authHeader) {
        if (!hasRole(authentication, "VENDOR")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only users with the VENDOR role can access vendor orders"));
        }

        VendorResponse vendor;
        try {
            vendor = vendorServiceClient.getMyVendor(authHeader);
        } catch (FeignException.NotFound e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No vendor profile found for your account"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Vendor service is currently unavailable. Please try again later."));
        }

        if (vendor == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No vendor profile found for your account"));
        }

        List<OrderResponse> orders = orderService.getOrdersByVendor(vendor.getId());
        return ResponseEntity.ok(orders);
    }

    /**
     * GET /api/orders/{orderId} — returns a single order belonging to the caller.
     */
    @Operation(summary = "Get order details by order ID (owner only)")
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderById(@PathVariable Long orderId,
                                          Authentication authentication) {
        try {
            Long userId = extractUserId(authentication);
            OrderResponse order = orderService.getOrderById(orderId, userId);
            return ResponseEntity.ok(order);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
        }
    }
}
