package com.marketgrid.productservice.controller;

import com.marketgrid.productservice.dto.ProductRequest;
import com.marketgrid.productservice.dto.ProductResponse;
import com.marketgrid.productservice.dto.StockDecreaseRequest;
import com.marketgrid.productservice.entity.Product;
import com.marketgrid.productservice.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

/**
 * REST controller for product catalog, stock updates, and vendor management.
 */
@Tag(name = "Product Management", description = "Product catalog browsing, vendor product creation, and stock updates")
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    @Value("${server.port:8083}")
    private String serverPort;

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

    private Long extractUserId(Authentication auth) {
        if (auth == null || auth.getDetails() == null) {
            throw new SecurityException("User authentication details not found");
        }
        return (Long) auth.getDetails();
    }

    // -----------------------------------------------------------
    // GET /api/products/instance-info — Load Balancing Diagnostic
    // -----------------------------------------------------------

    @Operation(summary = "Get service instance diagnostic info for load balancing demonstration")
    @GetMapping("/instance-info")
    public ResponseEntity<Map<String, String>> getInstanceInfo() {
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostname = "unknown-host";
        }

        return ResponseEntity.ok(Map.of(
                "service", "product-service",
                "port", serverPort,
                "hostname", hostname
        ));
    }

    // -----------------------------------------------------------
    // POST /api/products — create a product (VENDOR role required)
    // -----------------------------------------------------------

    @Operation(summary = "Create a new product (VENDOR role required)")
    @PostMapping
    public ResponseEntity<?> createProduct(@Valid @RequestBody ProductRequest request,
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

    @Operation(summary = "Update product details (owner vendor required)")
    @PutMapping("/{productId}")
    public ResponseEntity<?> updateProduct(@PathVariable Long productId,
                                           @Valid @RequestBody ProductRequest request,
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
    // DELETE /api/products/{productId} — delete product (owner only)
    // -----------------------------------------------------------

    @Operation(summary = "Delete a product (owner vendor required)")
    @DeleteMapping("/{productId}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long productId,
                                           Authentication authentication) {
        if (!hasRole(authentication, "VENDOR")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only users with the VENDOR role can delete products"));
        }

        try {
            Long userId = extractUserId(authentication);
            productService.deleteProduct(productId, userId);
            return ResponseEntity.noContent().build();
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

    @Operation(summary = "Decrement product stock quantity (ADMIN role required)")
    @PatchMapping("/{productId}/stock")
    public ResponseEntity<?> decreaseStock(@PathVariable Long productId,
                                           @RequestBody StockDecreaseRequest request,
                                           Authentication authentication) {
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
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", ex.getMessage()));
        }
    }
}
