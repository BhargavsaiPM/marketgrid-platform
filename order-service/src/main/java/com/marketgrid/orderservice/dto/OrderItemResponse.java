package com.marketgrid.orderservice.dto;

import com.marketgrid.orderservice.entity.OrderItem;
import com.marketgrid.orderservice.entity.OrderItemStatus;

import java.math.BigDecimal;

public class OrderItemResponse {

    private Long id;
    private Long productId;
    private Long vendorId;
    private Integer quantity;
    private BigDecimal priceAtOrderTime;
    private BigDecimal subtotal;
    private OrderItemStatus status;

    public OrderItemResponse() {
    }

    public static OrderItemResponse fromEntity(OrderItem item) {
        OrderItemResponse r = new OrderItemResponse();
        r.id = item.getId();
        r.productId = item.getProductId();
        r.vendorId = item.getVendorId();
        r.quantity = item.getQuantity();
        r.priceAtOrderTime = item.getPriceAtOrderTime();
        r.subtotal = item.getPriceAtOrderTime().multiply(BigDecimal.valueOf(item.getQuantity()));
        r.status = item.getStatus();
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

    public BigDecimal getPriceAtOrderTime() {
        return priceAtOrderTime;
    }

    public void setPriceAtOrderTime(BigDecimal priceAtOrderTime) {
        this.priceAtOrderTime = priceAtOrderTime;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public OrderItemStatus getStatus() {
        return status;
    }

    public void setStatus(OrderItemStatus status) {
        this.status = status;
    }
}
