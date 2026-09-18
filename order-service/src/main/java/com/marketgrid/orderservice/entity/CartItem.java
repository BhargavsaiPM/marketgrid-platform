package com.marketgrid.orderservice.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Line item inside a customer's cart.
 */
@Entity
@Table(name = "cart_items")
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Long vendorId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal priceAtAddTime;

    public CartItem() {
    }

    public CartItem(Cart cart, Long productId, Long vendorId, Integer quantity, BigDecimal priceAtAddTime) {
        this.cart = cart;
        this.productId = productId;
        this.vendorId = vendorId;
        this.quantity = quantity;
        this.priceAtAddTime = priceAtAddTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Cart getCart() {
        return cart;
    }

    public void setCart(Cart cart) {
        this.cart = cart;
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
}
