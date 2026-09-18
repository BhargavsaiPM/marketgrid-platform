package com.marketgrid.orderservice.client;

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
