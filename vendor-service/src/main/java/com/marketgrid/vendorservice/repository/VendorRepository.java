package com.marketgrid.vendorservice.repository;

import com.marketgrid.vendorservice.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Vendor} entities.
 */
public interface VendorRepository extends JpaRepository<Vendor, Long> {

    Optional<Vendor> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    List<Vendor> findByIsApprovedTrue();

    List<Vendor> findByIsApprovedFalse();
}
