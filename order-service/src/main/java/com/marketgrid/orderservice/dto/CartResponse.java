package com.marketgrid.orderservice.dto;

import com.marketgrid.orderservice.entity.Cart;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class CartResponse {

    private Long id;
    private Long userId;
    private List<CartItemResponse> items;
    private BigDecimal total;
    private LocalDateTime createdAt;

    public CartResponse() {
    }

    public static CartResponse fromEntity(Cart cart) {
        CartResponse r = new CartResponse();
        r.id = cart.getId();
        r.userId = cart.getUserId();
        r.createdAt = cart.getCreatedAt();

        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(CartItemResponse::fromEntity)
                .toList();
        r.items = itemResponses;

        BigDecimal totalAmount = itemResponses.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        r.total = totalAmount;

        return r;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<CartItemResponse> getItems() {
        return items;
    }

    public void setItems(List<CartItemResponse> items) {
        this.items = items;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
