package com.marketgrid.productservice.service;

import com.marketgrid.productservice.client.VendorResponse;
import com.marketgrid.productservice.client.VendorServiceClient;
import com.marketgrid.productservice.entity.Product;
import com.marketgrid.productservice.exception.ServiceUnavailableException;
import com.marketgrid.productservice.repository.ProductRepository;
import feign.FeignException;
import jakarta.persistence.OptimisticLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     */
    @Transactional
    public Product createProduct(String name, String description,
                                 BigDecimal price, Integer stockQuantity,
                                 String category) {
        return createProduct(name, description, price, stockQuantity, category, null);
    }

    /**
     * Create a new product for the authenticated vendor including optional image URL.
     */
    @Transactional
    public Product createProduct(String name, String description,
                                 BigDecimal price, Integer stockQuantity,
                                 String category, String imageUrl) {

        // Call vendor-service to get the authenticated user's vendor profile
        VendorResponse vendor;
        try {
            vendor = vendorServiceClient.getMyVendor();
        } catch (FeignException.NotFound e) {
            throw new IllegalArgumentException(
                    "No vendor profile found for your account. Register as a vendor first.");
        } catch (FeignException e) {
            throw new ServiceUnavailableException(
                    "Vendor service is temporarily unavailable, please try again shortly", e);
        }

        if (!vendor.isApproved()) {
            throw new IllegalStateException(
                    "Vendor '" + vendor.getBusinessName() + "' is not yet approved. " +
                    "Products cannot be created until an admin approves your vendor profile.");
        }

        Product product = new Product(vendor.getId(), name, description,
                price, stockQuantity, category, imageUrl);
        return productRepository.save(product);
    }

    /**
     * Retrieve a product by its primary key.
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
     */
    public List<Product> getAllProducts(String category) {
        if (category != null && !category.isBlank()) {
            return productRepository.findByCategory(category);
        }
        return productRepository.findAll();
    }

    /**
     * Update a product's mutable fields. Only the owning vendor may update.
     */
    @Transactional
    public Product updateProduct(Long productId, String name,
                                 String description, BigDecimal price,
                                 Integer stockQuantity, String category) {
        return updateProduct(productId, name, description, price, stockQuantity, category, null);
    }

    /**
     * Update a product's mutable fields including optional image URL. Only the owning vendor may update.
     */
    @Transactional
    public Product updateProduct(Long productId, String name,
                                 String description, BigDecimal price,
                                 Integer stockQuantity, String category,
                                 String imageUrl) {

        Product product = getProductById(productId);

        // Verify ownership via the authenticated vendor profile
        VendorResponse vendor;
        try {
            vendor = vendorServiceClient.getMyVendor();
        } catch (FeignException.NotFound e) {
            throw new SecurityException("No vendor profile found for your account.");
        } catch (FeignException e) {
            throw new ServiceUnavailableException(
                    "Vendor service is temporarily unavailable, please try again shortly", e);
        }

        if (!product.getVendorId().equals(vendor.getId())) {
            throw new SecurityException("You do not own this product");
        }

        if (name != null)          product.setName(name);
        if (description != null)   product.setDescription(description);
        if (price != null)         product.setPrice(price);
        if (stockQuantity != null) product.setStockQuantity(stockQuantity);
        if (category != null)      product.setCategory(category);
        if (imageUrl != null)      product.setImageUrl(imageUrl);

        return productRepository.save(product);
    }

    /**
     * Delete a product. Only the owning vendor may delete.
     */
    @Transactional
    public void deleteProduct(Long productId, Long requestingUserId) {
        Product product = getProductById(productId);

        // Verify ownership via the authenticated vendor profile
        VendorResponse vendor;
        try {
            vendor = vendorServiceClient.getMyVendor();
        } catch (FeignException.NotFound e) {
            throw new SecurityException("No vendor profile found for your account.");
        } catch (FeignException e) {
            throw new ServiceUnavailableException(
                    "Vendor service is temporarily unavailable, please try again shortly", e);
        }

        if (requestingUserId != null && vendor.getUserId() != null && !vendor.getUserId().equals(requestingUserId)) {
            throw new SecurityException("You do not own this product");
        }

        if (!product.getVendorId().equals(vendor.getId())) {
            throw new SecurityException("You do not own this product");
        }

        productRepository.delete(product);
    }

    /**
     * Decrement a product's stock by the given quantity.
     */
    @Transactional
    public Product decreaseStock(Long productId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity to decrement must be a positive number");
        }

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
