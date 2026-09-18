package com.marketgrid.orderservice.dto;

import com.marketgrid.orderservice.entity.Order;
import com.marketgrid.orderservice.entity.OrderItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OrderResponse {

    private Long orderId;
    private Long userId;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private List<VendorOrderGroupResponse> vendorGroups;

    public OrderResponse() {
    }

    public static OrderResponse fromEntity(Order order) {
        return fromEntityWithItems(order, order.getItems());
    }

    public static OrderResponse fromEntityWithItems(Order order, List<OrderItem> itemsToInclude) {
        OrderResponse r = new OrderResponse();
        r.orderId = order.getId();
        r.userId = order.getUserId();
        r.totalAmount = order.getTotalAmount();
        r.createdAt = order.getCreatedAt();

        // Group items by vendorId
        Map<Long, List<OrderItem>> groupedByVendor = itemsToInclude.stream()
                .collect(Collectors.groupingBy(OrderItem::getVendorId));

        r.vendorGroups = groupedByVendor.entrySet().stream()
                .map(entry -> {
                    List<OrderItemResponse> itemResponses = entry.getValue().stream()
                            .map(OrderItemResponse::fromEntity)
                            .toList();
                    return new VendorOrderGroupResponse(entry.getKey(), itemResponses);
                })
                .toList();

        return r;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<VendorOrderGroupResponse> getVendorGroups() {
        return vendorGroups;
    }

    public void setVendorGroups(List<VendorOrderGroupResponse> vendorGroups) {
        this.vendorGroups = vendorGroups;
    }
}
