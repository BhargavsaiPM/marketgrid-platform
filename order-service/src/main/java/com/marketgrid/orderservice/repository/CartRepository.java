package com.marketgrid.orderservice.repository;

import com.marketgrid.orderservice.entity.Cart;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    // Performance Optimization: Fetch cart items in a single query with cart
    @EntityGraph(attributePaths = {"items"})
    Optional<Cart> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
