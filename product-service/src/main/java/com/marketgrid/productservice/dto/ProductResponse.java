package com.marketgrid.productservice.dto;

import com.marketgrid.productservice.entity.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Public-facing projection of a {@link Product}.
 */
public class ProductResponse {

    private Long id;
    private Long vendorId;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private String category;
    private LocalDateTime createdAt;

    public ProductResponse() {
    }

    public static ProductResponse fromEntity(Product product) {
        ProductResponse r = new ProductResponse();
        r.id = product.getId();
        r.vendorId = product.getVendorId();
        r.name = product.getName();
        r.description = product.getDescription();
        r.price = product.getPrice();
        r.stockQuantity = product.getStockQuantity();
        r.category = product.getCategory();
        r.createdAt = product.getCreatedAt();
        return r;
    }

    // ---- Getters & Setters ----

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVendorId() {
        return vendorId;
    }

    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
