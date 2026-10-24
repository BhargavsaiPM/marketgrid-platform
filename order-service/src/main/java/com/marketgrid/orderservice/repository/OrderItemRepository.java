package com.marketgrid.orderservice.repository;

import com.marketgrid.orderservice.entity.OrderItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // Performance Optimization: Fetch associated order and items eagerly in one JOIN
    @EntityGraph(attributePaths = {"order", "order.items"})
    List<OrderItem> findByVendorId(Long vendorId);

    List<OrderItem> findByOrderId(Long orderId);
}
