package com.marketgrid.productservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * Request body for POST /api/products and PUT /api/products/{id}.
 */
public class ProductRequest {

    private String name;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private String category;

    @Schema(description = "Optional URL of product image", example = "https://picsum.photos/seed/keyboard/800/800", maxLength = 1000)
    private String imageUrl;

    public ProductRequest() {
    }

    public ProductRequest(String name, String description, BigDecimal price,
                          Integer stockQuantity, String category) {
        this(name, description, price, stockQuantity, category, null);
    }

    public ProductRequest(String name, String description, BigDecimal price,
                          Integer stockQuantity, String category, String imageUrl) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.category = category;
        this.imageUrl = imageUrl;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
