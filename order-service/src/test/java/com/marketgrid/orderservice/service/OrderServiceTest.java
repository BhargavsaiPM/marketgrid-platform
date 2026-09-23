package com.marketgrid.orderservice.service;

import com.marketgrid.orderservice.client.ProductServiceClient;
import com.marketgrid.orderservice.dto.OrderResponse;
import com.marketgrid.orderservice.entity.Cart;
import com.marketgrid.orderservice.entity.CartItem;
import com.marketgrid.orderservice.entity.Order;
import com.marketgrid.orderservice.entity.OrderItem;
import com.marketgrid.orderservice.entity.OrderItemStatus;
import com.marketgrid.orderservice.repository.CartRepository;
import com.marketgrid.orderservice.repository.OrderItemRepository;
import com.marketgrid.orderservice.repository.OrderRepository;
import com.marketgrid.orderservice.security.ServiceAuthTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductServiceClient productServiceClient;

    private ServiceAuthTokenProvider serviceAuthTokenProvider;
    private CartService cartService;
    private OrderService orderService;

    private Cart sampleCart;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartRepository, productServiceClient);
        serviceAuthTokenProvider = new ServiceAuthTokenProvider(null, null) {
            @Override
            public String getAdminToken() {
                return "mock-admin-token";
            }
        };

        orderService = new OrderService(orderRepository, orderItemRepository,
                cartService, productServiceClient, serviceAuthTokenProvider);

        sampleCart = new Cart(1L);
        CartItem item1 = new CartItem(sampleCart, 10L, 5L, 2, new BigDecimal("50.00"));
        CartItem item2 = new CartItem(sampleCart, 20L, 6L, 1, new BigDecimal("150.00"));
        sampleCart.getItems().add(item1);
        sampleCart.getItems().add(item2);
    }

    @Test
    void testCheckout_EmptyCart_ThrowsIllegalArgumentException() {
        Cart emptyCart = new Cart(1L);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(emptyCart));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                orderService.checkout(1L));

        assertTrue(ex.getMessage().contains("Cannot checkout with an empty cart"));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void testCheckout_SuccessfulMultiVendorCheckout() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(sampleCart));

        Order savedOrder = new Order(1L);
        savedOrder.setId(99L);
        savedOrder.setTotalAmount(new BigDecimal("250.00"));
        savedOrder.setCreatedAt(LocalDateTime.now());
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(cartRepository.save(any(Cart.class))).thenReturn(sampleCart);

        OrderResponse response = orderService.checkout(1L);

        assertNotNull(response);
        assertEquals(99L, response.getOrderId());
        assertEquals(new BigDecimal("250.00"), response.getTotalAmount());
        assertEquals(0, sampleCart.getItems().size()); // Cart cleared upon checkout
    }

    @Test
    void testGetOrderById_SuccessForOwner() {
        Order order = new Order(1L);
        order.setId(50L);
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setCreatedAt(LocalDateTime.now());

        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

        OrderResponse res = orderService.getOrderById(50L, 1L);

        assertNotNull(res);
        assertEquals(50L, res.getOrderId());
    }

    @Test
    void testGetOrderById_OtherCustomer_ThrowsSecurityException() {
        Order order = new Order(1L); // Owned by customer 1
        order.setId(50L);

        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

        // Customer 2 tries to access Customer 1's order
        SecurityException ex = assertThrows(SecurityException.class, () ->
                orderService.getOrderById(50L, 2L));

        assertTrue(ex.getMessage().contains("You do not own this order"));
    }

    @Test
    void testGetOrdersByVendor_VendorIsolation() {
        Order order = new Order(1L);
        order.setId(10L);
        order.setCreatedAt(LocalDateTime.now());

        OrderItem vendorItem = new OrderItem(order, 100L, 5L, 2, new BigDecimal("40.00"), OrderItemStatus.CONFIRMED);
        vendorItem.setId(1L);

        when(orderItemRepository.findByVendorId(5L)).thenReturn(List.of(vendorItem));

        List<OrderResponse> results = orderService.getOrdersByVendor(5L);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(1, results.get(0).getVendorGroups().size());
        assertEquals(5L, results.get(0).getVendorGroups().get(0).getVendorId());
    }
}
