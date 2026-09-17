package com.marketgrid.productservice.repository;

import com.marketgrid.productservice.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link Product} entities.
 *
 * <p>Optimistic locking is handled automatically by JPA via the
 * {@code @Version} field on Product — no custom lock annotations needed.
 * If two transactions read the same row and both try to save, the second
 * will receive an {@link jakarta.persistence.OptimisticLockException}.</p>
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByVendorId(Long vendorId);

    List<Product> findByCategory(String category);
}
