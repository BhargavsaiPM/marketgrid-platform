package com.marketgrid.productservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for PATCH /api/products/{productId}/stock.
 * Used by order-service to decrement stock when an order is placed.
 */
public class StockDecreaseRequest {

    @NotNull(message = "Quantity cannot be null")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    public StockDecreaseRequest() {
    }

    public StockDecreaseRequest(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
