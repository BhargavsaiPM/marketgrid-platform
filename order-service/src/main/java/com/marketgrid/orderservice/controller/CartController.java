package com.marketgrid.orderservice.controller;

import com.marketgrid.orderservice.dto.AddToCartRequest;
import com.marketgrid.orderservice.dto.CartResponse;
import com.marketgrid.orderservice.dto.UpdateQuantityRequest;
import com.marketgrid.orderservice.exception.ServiceUnavailableException;
import com.marketgrid.orderservice.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for customer shopping cart operations.
 * All endpoints require valid JWT authentication.
 */
@Tag(name = "Cart Management", description = "Shopping cart operations including adding, updating, and removing items")
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    private Long extractUserId(Authentication auth) {
        if (auth == null || auth.getDetails() == null) {
            throw new SecurityException("User authentication details not found");
        }
        return (Long) auth.getDetails();
    }

    /**
     * GET /api/cart — returns the authenticated user's cart with items and total.
     */
    @Operation(summary = "View current authenticated customer's shopping cart")
    @GetMapping
    public ResponseEntity<?> getCart(Authentication authentication) {
        try {
            Long userId = extractUserId(authentication);
            CartResponse cart = cartService.getCart(userId);
            return ResponseEntity.ok(cart);
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * POST /api/cart/items — add a product to the cart.
     */
    @Operation(summary = "Add a product to the shopping cart")
    @PostMapping("/items")
    public ResponseEntity<?> addItem(@RequestBody AddToCartRequest request,
                                     Authentication authentication) {
        try {
            Long userId = extractUserId(authentication);
            CartResponse cart = cartService.addItem(userId, request.getProductId(), request.getQuantity());
            return ResponseEntity.ok(cart);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (ServiceUnavailableException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", ex.getMessage()));
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * PUT /api/cart/items/{cartItemId} — update item quantity.
     */
    @Operation(summary = "Update quantity of an item in the cart")
    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<?> updateItemQuantity(@PathVariable Long cartItemId,
                                                @RequestBody UpdateQuantityRequest request,
                                                Authentication authentication) {
        try {
            Long userId = extractUserId(authentication);
            CartResponse cart = cartService.updateItemQuantity(userId, cartItemId, request.getQuantity());
            return ResponseEntity.ok(cart);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * DELETE /api/cart/items/{cartItemId} — remove a single item from the cart.
     */
    @Operation(summary = "Remove a single item from the cart")
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<?> removeItem(@PathVariable Long cartItemId,
                                        Authentication authentication) {
        try {
            Long userId = extractUserId(authentication);
            CartResponse cart = cartService.removeItem(userId, cartItemId);
            return ResponseEntity.ok(cart);
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * DELETE /api/cart — remove all items from the customer's cart.
     */
    @Operation(summary = "Clear all items from the shopping cart")
    @DeleteMapping
    public ResponseEntity<?> clearCart(Authentication authentication) {
        try {
            Long userId = extractUserId(authentication);
            CartResponse cart = cartService.clearCart(userId);
            return ResponseEntity.ok(cart);
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
        }
    }
}
