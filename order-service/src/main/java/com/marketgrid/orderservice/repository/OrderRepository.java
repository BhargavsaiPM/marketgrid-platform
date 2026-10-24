package com.marketgrid.orderservice.repository;

import com.marketgrid.orderservice.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // Performance Optimization: Fetch order items in a single JOIN query to eliminate N+1 query overhead
    @EntityGraph(attributePaths = {"items"})
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"items"})
    Optional<Order> findByIdAndUserId(Long id, Long userId);
}
