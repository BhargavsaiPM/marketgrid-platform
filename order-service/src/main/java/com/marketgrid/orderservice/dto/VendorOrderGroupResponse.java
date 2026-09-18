package com.marketgrid.orderservice.dto;

import com.marketgrid.orderservice.entity.OrderItemStatus;

import java.math.BigDecimal;
import java.util.List;

public class VendorOrderGroupResponse {

    private Long vendorId;
    private List<OrderItemResponse> items;
    private BigDecimal subtotal;
    private boolean allConfirmed;

    public VendorOrderGroupResponse() {
    }

    public VendorOrderGroupResponse(Long vendorId, List<OrderItemResponse> items) {
        this.vendorId = vendorId;
        this.items = items;
        this.subtotal = items.stream()
                .filter(i -> i.getStatus() == OrderItemStatus.CONFIRMED)
                .map(OrderItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.allConfirmed = !items.isEmpty() && items.stream()
                .allMatch(i -> i.getStatus() == OrderItemStatus.CONFIRMED);
    }

    public Long getVendorId() {
        return vendorId;
    }

    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
    }

    public List<OrderItemResponse> getItems() {
        return items;
    }

    public void setItems(List<OrderItemResponse> items) {
        this.items = items;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public boolean isAllConfirmed() {
        return allConfirmed;
    }

    public void setAllConfirmed(boolean allConfirmed) {
        this.allConfirmed = allConfirmed;
    }
}
