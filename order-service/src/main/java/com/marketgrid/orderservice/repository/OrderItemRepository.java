package com.marketgrid.orderservice.repository;

import com.marketgrid.orderservice.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByVendorId(Long vendorId);

    List<OrderItem> findByOrderId(Long orderId);
}
