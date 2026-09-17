package com.marketgrid.productservice.service;

import com.marketgrid.productservice.client.VendorResponse;
import com.marketgrid.productservice.client.VendorServiceClient;
import com.marketgrid.productservice.entity.Product;
import com.marketgrid.productservice.repository.ProductRepository;
import feign.FeignException;
import jakarta.persistence.OptimisticLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Core business logic for product management.
 *
 * <p>Before creating a product, this service calls vendor-service via
 * OpenFeign to verify that the vendor exists and is approved. Stock
 * decrements use JPA optimistic locking ({@code @Version}) to prevent
 * overselling under concurrent requests.</p>
 */
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final VendorServiceClient vendorServiceClient;

    public ProductService(ProductRepository productRepository,
                          VendorServiceClient vendorServiceClient) {
        this.productRepository = productRepository;
        this.vendorServiceClient = vendorServiceClient;
    }

    /**
     * Create a new product for the authenticated vendor.
     *
     * <p>Calls {@code GET /api/vendors/me} on vendor-service (via Feign,
     * forwarding the caller's JWT) to verify the vendor exists and is
     * approved before persisting.</p>
     *
     * @return the created product with the vendor's {@code id} set as {@code vendorId}
     * @throws IllegalStateException    if the vendor is not approved
     * @throws IllegalArgumentException if no vendor profile is found for the user
     */
    public Product createProduct(String name, String description,
                                 BigDecimal price, Integer stockQuantity,
                                 String category) {

        // Call vendor-service to get the authenticated user's vendor profile
        VendorResponse vendor;
        try {
            vendor = vendorServiceClient.getMyVendor();
        } catch (FeignException.NotFound e) {
            throw new IllegalArgumentException(
                    "No vendor profile found for your account. Register as a vendor first.");
        } catch (FeignException e) {
            throw new RuntimeException(
                    "Failed to reach vendor-service: " + e.getMessage(), e);
        }

        if (!vendor.isApproved()) {
            throw new IllegalStateException(
                    "Vendor '" + vendor.getBusinessName() + "' is not yet approved. " +
                    "Products cannot be created until an admin approves your vendor profile.");
        }

        Product product = new Product(vendor.getId(), name, description,
                price, stockQuantity, category);
        return productRepository.save(product);
    }

    /**
     * Retrieve a product by its primary key.
     *
     * @throws IllegalArgumentException if not found
     */
    public Product getProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Product not found with id: " + productId));
    }

    /**
     * Retrieve all products listed by a specific vendor.
     */
    public List<Product> getProductsByVendor(Long vendorId) {
        return productRepository.findByVendorId(vendorId);
    }

    /**
     * Retrieve all products, optionally filtered by category.
     *
     * @param category if non-null and non-blank, only products in this category are returned
     */
    public List<Product> getAllProducts(String category) {
        if (category != null && !category.isBlank()) {
            return productRepository.findByCategory(category);
        }
        return productRepository.findAll();
    }

    /**
     * Update a product's mutable fields. Only the owning vendor may update.
     *
     * <p>Ownership is verified by calling {@code GET /api/vendors/me}
     * (with the caller's JWT) and checking that the returned vendor ID
     * matches the product's vendorId.</p>
     *
     * @throws IllegalArgumentException if product not found
     * @throws SecurityException        if the requesting vendor doesn't own the product
     */
    public Product updateProduct(Long productId, String name,
                                 String description, BigDecimal price,
                                 Integer stockQuantity, String category) {

        Product product = getProductById(productId);

        // Verify ownership via the authenticated vendor profile
        VendorResponse vendor;
        try {
            vendor = vendorServiceClient.getMyVendor();
        } catch (FeignException.NotFound e) {
            throw new SecurityException("No vendor profile found for your account.");
        }

        if (!product.getVendorId().equals(vendor.getId())) {
            throw new SecurityException("You do not own this product");
        }

        if (name != null)          product.setName(name);
        if (description != null)   product.setDescription(description);
        if (price != null)         product.setPrice(price);
        if (stockQuantity != null) product.setStockQuantity(stockQuantity);
        if (category != null)      product.setCategory(category);

        return productRepository.save(product);
    }

    /**
     * Decrement a product's stock by the given quantity.
     *
     * <p>Relies on JPA {@code @Version}-based optimistic locking to prevent
     * overselling. If two concurrent requests both read the same stock level
     * and try to decrement, the second will fail with an
     * {@link OptimisticLockException}, which is caught here and translated
     * to a clear retry message.</p>
     *
     * @throws IllegalArgumentException if product not found or insufficient stock
     * @throws IllegalStateException    if concurrent modification detected (client should retry)
     */
    public Product decreaseStock(Long productId, Integer quantity) {
        Product product = getProductById(productId);

        if (product.getStockQuantity() < quantity) {
            throw new IllegalArgumentException(
                    "Insufficient stock for product '" + product.getName() +
                    "'. Available: " + product.getStockQuantity() +
                    ", requested: " + quantity);
        }

        product.setStockQuantity(product.getStockQuantity() - quantity);

        try {
            return productRepository.save(product);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new IllegalStateException(
                    "Stock for product '" + product.getName() + "' was modified by another " +
                    "request. Please retry the operation.", e);
        }
    }
}
