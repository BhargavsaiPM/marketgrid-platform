package com.marketgrid.productservice.controller;

import com.marketgrid.productservice.dto.ProductRequest;
import com.marketgrid.productservice.dto.ProductResponse;
import com.marketgrid.productservice.dto.StockDecreaseRequest;
import com.marketgrid.productservice.entity.Product;
import com.marketgrid.productservice.service.ProductService;
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
 * REST controller for product catalog management.
 *
 * <p>Public GET endpoints allow browsing; protected POST/PUT endpoints
 * require a VENDOR-role JWT; the PATCH stock endpoint accepts any valid
 * JWT (intended for order-service calls).</p>
 */
@Tag(name = "Product Management", description = "Product catalog browsing, vendor product creation, and stock updates")
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // -----------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------

    private boolean hasRole(Authentication auth, String role) {
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_" + role));
    }

    // -----------------------------------------------------------
    // POST /api/products — create a product (VENDOR role required)
    // -----------------------------------------------------------

    /**
     * Create a new product. The vendorId is resolved from the JWT via
     * a Feign call to vendor-service ({@code GET /api/vendors/me}),
     * not from the request body, to prevent spoofing.
     */
    @Operation(summary = "Create a new product (VENDOR role required)")
    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody ProductRequest request,
                                           Authentication authentication) {
        if (!hasRole(authentication, "VENDOR")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only users with the VENDOR role can create products"));
        }

        try {
            Product product = productService.createProduct(
                    request.getName(),
                    request.getDescription(),
                    request.getPrice(),
                    request.getStockQuantity(),
                    request.getCategory()
            );
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ProductResponse.fromEntity(product));
        } catch (IllegalStateException ex) {
            // Vendor not approved
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    // -----------------------------------------------------------
    // GET /api/products — list all products (public, optional filter)
    // -----------------------------------------------------------

    /**
     * Browse all products. Pass an optional {@code ?category=Electronics}
     * query parameter to filter by category.
     */
    @Operation(summary = "Browse all products with optional category filter")
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts(
            @RequestParam(required = false) String category) {

        List<ProductResponse> products = productService.getAllProducts(category).stream()
                .map(ProductResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(products);
    }

    // -----------------------------------------------------------
    // GET /api/products/{productId} — single product detail (public)
    // -----------------------------------------------------------

    @Operation(summary = "Get single product detail by product ID")
    @GetMapping("/{productId}")
    public ResponseEntity<?> getProduct(@PathVariable Long productId) {
        try {
            Product product = productService.getProductById(productId);
            return ResponseEntity.ok(ProductResponse.fromEntity(product));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    // -----------------------------------------------------------
    // GET /api/products/vendor/{vendorId} — products by vendor (public)
    // -----------------------------------------------------------

    @Operation(summary = "Get all products belonging to a vendor")
    @GetMapping("/vendor/{vendorId}")
    public ResponseEntity<List<ProductResponse>> getProductsByVendor(
            @PathVariable Long vendorId) {

        List<ProductResponse> products = productService.getProductsByVendor(vendorId).stream()
                .map(ProductResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(products);
    }

    // -----------------------------------------------------------
    // PUT /api/products/{productId} — update product (owner only)
    // -----------------------------------------------------------

    /**
     * Update a product. Only the owning vendor (verified via JWT → Feign
     * call to vendor-service) may update.
     */
    @Operation(summary = "Update product details (owner vendor required)")
    @PutMapping("/{productId}")
    public ResponseEntity<?> updateProduct(@PathVariable Long productId,
                                           @RequestBody ProductRequest request,
                                           Authentication authentication) {
        if (!hasRole(authentication, "VENDOR")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only users with the VENDOR role can update products"));
        }

        try {
            Product updated = productService.updateProduct(
                    productId,
                    request.getName(),
                    request.getDescription(),
                    request.getPrice(),
                    request.getStockQuantity(),
                    request.getCategory()
            );
            return ResponseEntity.ok(ProductResponse.fromEntity(updated));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", ex.getMessage()));
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    // -----------------------------------------------------------
    // PATCH /api/products/{productId}/stock — decrement stock
    // -----------------------------------------------------------

    /**
     * Decrement a product's stock quantity. Intended for order-service
     * to call when an order is placed.
     *
     * <p>Requires any valid JWT (not restricted to VENDOR role).
     * This endpoint relies on JPA optimistic locking to prevent overselling
     * under concurrent requests.</p>
     *
     * <p><strong>TODO:</strong> Harden with service-to-service authentication
     * (e.g. dedicated service account or mTLS) to prevent arbitrary callers
     * from decrementing stock.</p>
     */
    @Operation(summary = "Decrement product stock quantity (ADMIN role required)")
    @PatchMapping("/{productId}/stock")
    public ResponseEntity<?> decreaseStock(@PathVariable Long productId,
                                           @RequestBody StockDecreaseRequest request,
                                           Authentication authentication) {
        // TODO: In a production system, this should also allow trusted internal service-to-service calls (e.g. from order-service), not just ADMIN users.
        if (!hasRole(authentication, "ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only users with the ADMIN role can decrement stock"));
        }

        try {
            Product updated = productService.decreaseStock(productId, request.getQuantity());
            return ResponseEntity.ok(ProductResponse.fromEntity(updated));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            // Optimistic lock conflict — client should retry
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", ex.getMessage()));
        }
    }
}
