package com.marketgrid.productservice.service;

import com.marketgrid.productservice.client.VendorResponse;
import com.marketgrid.productservice.client.VendorServiceClient;
import com.marketgrid.productservice.entity.Product;
import com.marketgrid.productservice.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private VendorServiceClient vendorServiceClient;

    @InjectMocks
    private ProductService productService;

    private VendorResponse approvedVendor;
    private VendorResponse unapprovedVendor;
    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        approvedVendor = new VendorResponse();
        approvedVendor.setId(5L);
        approvedVendor.setUserId(10L);
        approvedVendor.setBusinessName("Alpha Tech");
        approvedVendor.setApproved(true);

        unapprovedVendor = new VendorResponse();
        unapprovedVendor.setId(6L);
        unapprovedVendor.setUserId(11L);
        unapprovedVendor.setBusinessName("Beta Pending");
        unapprovedVendor.setApproved(false);

        sampleProduct = new Product(5L, "Wireless Mouse", "Ergonomic",
                new BigDecimal("29.99"), 20, "ELECTRONICS");
        sampleProduct.setId(50L);
    }

    @Test
    void testCreateProduct_ApprovedVendor_Success() {
        when(vendorServiceClient.getMyVendor()).thenReturn(approvedVendor);
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        Product created = productService.createProduct("Wireless Mouse", "Ergonomic",
                new BigDecimal("29.99"), 20, "ELECTRONICS");

        assertNotNull(created);
        assertEquals("Wireless Mouse", created.getName());
        assertEquals(5L, created.getVendorId());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void testCreateProduct_UnapprovedVendor_ThrowsIllegalStateException() {
        when(vendorServiceClient.getMyVendor()).thenReturn(unapprovedVendor);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                productService.createProduct("Unapproved Prod", "Desc",
                        new BigDecimal("10.00"), 5, "ELECTRONICS"));

        assertTrue(ex.getMessage().contains("is not yet approved"));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void testUpdateProduct_OwnerVendor_Success() {
        when(productRepository.findById(50L)).thenReturn(Optional.of(sampleProduct));
        when(vendorServiceClient.getMyVendor()).thenReturn(approvedVendor); // vendorId = 5
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        Product updated = productService.updateProduct(50L, "Updated Mouse", null,
                new BigDecimal("34.99"), 15, null);

        assertNotNull(updated);
        assertEquals("Updated Mouse", updated.getName());
        verify(productRepository, times(1)).save(sampleProduct);
    }

    @Test
    void testUpdateProduct_OtherVendor_ThrowsSecurityException() {
        when(productRepository.findById(50L)).thenReturn(Optional.of(sampleProduct)); // vendorId = 5

        VendorResponse otherVendor = new VendorResponse();
        otherVendor.setId(99L);
        when(vendorServiceClient.getMyVendor()).thenReturn(otherVendor);

        SecurityException ex = assertThrows(SecurityException.class, () ->
                productService.updateProduct(50L, "Hacked", null, null, null, null));

        assertTrue(ex.getMessage().contains("You do not own this product"));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void testDecreaseStock_Success() {
        when(productRepository.findById(50L)).thenReturn(Optional.of(sampleProduct)); // stock = 20
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        Product updated = productService.decreaseStock(50L, 5);

        assertNotNull(updated);
        assertEquals(15, updated.getStockQuantity());
        verify(productRepository, times(1)).save(sampleProduct);
    }

    @Test
    void testDecreaseStock_InsufficientStock_ThrowsIllegalArgumentException() {
        when(productRepository.findById(50L)).thenReturn(Optional.of(sampleProduct)); // stock = 20

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                productService.decreaseStock(50L, 25));

        assertTrue(ex.getMessage().contains("Insufficient stock"));
        assertEquals(20, sampleProduct.getStockQuantity()); // unchanged
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void testDecreaseStock_NonPositiveQuantity_ThrowsIllegalArgumentException() {
        IllegalArgumentException exZero = assertThrows(IllegalArgumentException.class, () ->
                productService.decreaseStock(50L, 0));
        assertTrue(exZero.getMessage().contains("must be a positive number"));

        IllegalArgumentException exNeg = assertThrows(IllegalArgumentException.class, () ->
                productService.decreaseStock(50L, -5));
        assertTrue(exNeg.getMessage().contains("must be a positive number"));
    }
}
