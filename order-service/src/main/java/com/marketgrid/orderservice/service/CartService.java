package com.marketgrid.orderservice.service;

import com.marketgrid.orderservice.client.ProductResponse;
import com.marketgrid.orderservice.client.ProductServiceClient;
import com.marketgrid.orderservice.dto.CartResponse;
import com.marketgrid.orderservice.entity.Cart;
import com.marketgrid.orderservice.entity.CartItem;
import com.marketgrid.orderservice.exception.ServiceUnavailableException;
import com.marketgrid.orderservice.repository.CartRepository;
import feign.FeignException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service managing customer shopping carts and items.
 */
@Service
public class CartService {

    private final CartRepository cartRepository;
    private final ProductServiceClient productServiceClient;

    public CartService(CartRepository cartRepository,
                       ProductServiceClient productServiceClient) {
        this.cartRepository = cartRepository;
        this.productServiceClient = productServiceClient;
    }

    /**
     * Find existing cart for user, or create a new empty one.
     */
    @Transactional
    public Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(new Cart(userId)));
    }

    /**
     * Retrieve the user's cart with items and computed total.
     */
    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return CartResponse.fromEntity(cart);
    }

    /**
     * Add a product to the customer's cart. If the product is already in the cart,
     * its quantity is incremented.
     *
     * @throws IllegalArgumentException if quantity <= 0, productId is null, or product not found
     * @throws ServiceUnavailableException if product-service cannot be reached
     */
    @Transactional
    public CartResponse addItem(Long userId, Long productId, Integer quantity) {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID must not be null");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be a positive number");
        }

        // Call product-service to fetch product details (price, vendor, stock)
        ProductResponse product;
        try {
            product = productServiceClient.getProductById(productId);
        } catch (FeignException.NotFound e) {
            throw new IllegalArgumentException("Product not found with id: " + productId);
        } catch (FeignException e) {
            throw new ServiceUnavailableException(
                    "Product service is currently unavailable. Please try again later.", e);
        }

        if (product == null) {
            throw new IllegalArgumentException("Product not found with id: " + productId);
        }

        Cart cart = getOrCreateCart(userId);

        // Check if item already exists in this cart
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
        } else {
            CartItem newItem = new CartItem(
                    cart,
                    product.getId(),
                    product.getVendorId(),
                    quantity,
                    product.getPrice() // snapshot price at add time
            );
            cart.getItems().add(newItem);
        }

        Cart savedCart = cartRepository.save(cart);
        return CartResponse.fromEntity(savedCart);
    }

    /**
     * Update an item's quantity in the user's cart. If newQuantity is 0,
     * the item is removed. Validates ownership against the caller's cart.
     *
     * @throws SecurityException if the item does not belong to the user's cart
     * @throws IllegalArgumentException if quantity is negative or null
     */
    @Transactional
    public CartResponse updateItemQuantity(Long userId, Long cartItemId, Integer newQuantity) {
        if (newQuantity == null || newQuantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative or null");
        }

        Cart cart = getOrCreateCart(userId);

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new SecurityException("Cart item not found in your cart"));

        if (newQuantity == 0) {
            cart.getItems().remove(item);
        } else {
            item.setQuantity(newQuantity);
        }

        Cart savedCart = cartRepository.save(cart);
        return CartResponse.fromEntity(savedCart);
    }

    /**
     * Remove an item from the customer's cart. Validates ownership against the caller's cart.
     *
     * @throws SecurityException if the item does not belong to the user's cart
     */
    @Transactional
    public CartResponse removeItem(Long userId, Long cartItemId) {
        Cart cart = getOrCreateCart(userId);

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new SecurityException("Cart item not found in your cart"));

        cart.getItems().remove(item);
        Cart savedCart = cartRepository.save(cart);
        return CartResponse.fromEntity(savedCart);
    }

    /**
     * Remove all items from the customer's cart.
     */
    @Transactional
    public CartResponse clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().clear();
        Cart savedCart = cartRepository.save(cart);
        return CartResponse.fromEntity(savedCart);
    }
}
