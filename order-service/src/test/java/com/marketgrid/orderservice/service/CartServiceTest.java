package com.marketgrid.orderservice.service;

import com.marketgrid.orderservice.client.ProductResponse;
import com.marketgrid.orderservice.client.ProductServiceClient;
import com.marketgrid.orderservice.dto.CartResponse;
import com.marketgrid.orderservice.entity.Cart;
import com.marketgrid.orderservice.entity.CartItem;
import com.marketgrid.orderservice.repository.CartRepository;
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
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductServiceClient productServiceClient;

    @InjectMocks
    private CartService cartService;

    private Cart sampleCart;
    private ProductResponse sampleProduct;

    @BeforeEach
    void setUp() {
        sampleCart = new Cart(1L);
        sampleCart.setId(10L);

        sampleProduct = new ProductResponse();
        sampleProduct.setId(100L);
        sampleProduct.setVendorId(5L);
        sampleProduct.setName("Keyboard");
        sampleProduct.setPrice(new BigDecimal("84.99"));
        sampleProduct.setStockQuantity(50);
    }

    @Test
    void testAddItem_Success() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(sampleCart));
        when(productServiceClient.getProductById(100L)).thenReturn(sampleProduct);
        when(cartRepository.save(any(Cart.class))).thenReturn(sampleCart);

        CartResponse res = cartService.addItem(1L, 100L, 2);

        assertNotNull(res);
        assertEquals(1, sampleCart.getItems().size());
        assertEquals(new BigDecimal("169.98"), res.getTotal());
        verify(cartRepository, times(1)).save(sampleCart);
    }

    @Test
    void testAddItem_InvalidQuantity_ThrowsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                cartService.addItem(1L, 100L, 0));
        assertTrue(ex.getMessage().contains("must be a positive number"));

        IllegalArgumentException exNeg = assertThrows(IllegalArgumentException.class, () ->
                cartService.addItem(1L, 100L, -1));
        assertTrue(exNeg.getMessage().contains("must be a positive number"));
    }

    @Test
    void testUpdateItemQuantity_SetToZero_RemovesItem() {
        CartItem item = new CartItem(sampleCart, 100L, 5L, 2, new BigDecimal("84.99"));
        item.setId(500L);
        sampleCart.getItems().add(item);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(sampleCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(sampleCart);

        CartResponse res = cartService.updateItemQuantity(1L, 500L, 0);

        assertNotNull(res);
        assertEquals(0, sampleCart.getItems().size());
        assertEquals(BigDecimal.ZERO, res.getTotal());
    }

    @Test
    void testUpdateItemQuantity_ItemNotInCart_ThrowsSecurityException() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(sampleCart));

        // Cart is empty, trying to update item 999L
        SecurityException ex = assertThrows(SecurityException.class, () ->
                cartService.updateItemQuantity(1L, 999L, 5));

        assertTrue(ex.getMessage().contains("Cart item not found in your cart"));
    }

    @Test
    void testRemoveItem_ItemNotInCart_ThrowsSecurityException() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(sampleCart));

        SecurityException ex = assertThrows(SecurityException.class, () ->
                cartService.removeItem(1L, 999L));

        assertTrue(ex.getMessage().contains("Cart item not found in your cart"));
    }
}
