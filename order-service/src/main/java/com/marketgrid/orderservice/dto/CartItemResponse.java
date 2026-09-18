package com.marketgrid.orderservice.dto;

import com.marketgrid.orderservice.entity.CartItem;

import java.math.BigDecimal;

public class CartItemResponse {

    private Long id;
    private Long productId;
    private Long vendorId;
    private Integer quantity;
    private BigDecimal priceAtAddTime;
    private BigDecimal subtotal;

    public CartItemResponse() {
    }

    public static CartItemResponse fromEntity(CartItem item) {
        CartItemResponse r = new CartItemResponse();
        r.id = item.getId();
        r.productId = item.getProductId();
        r.vendorId = item.getVendorId();
        r.quantity = item.getQuantity();
        r.priceAtAddTime = item.getPriceAtAddTime();
        r.subtotal = item.getPriceAtAddTime().multiply(BigDecimal.valueOf(item.getQuantity()));
        return r;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getVendorId() {
        return vendorId;
    }

    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPriceAtAddTime() {
        return priceAtAddTime;
    }

    public void setPriceAtAddTime(BigDecimal priceAtAddTime) {
        this.priceAtAddTime = priceAtAddTime;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
}
