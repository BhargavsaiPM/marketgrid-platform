package com.marketgrid.productservice.dto;

/**
 * Request body for PATCH /api/products/{productId}/stock.
 * Used by order-service to decrement stock when an order is placed.
 */
public class StockDecreaseRequest {

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
