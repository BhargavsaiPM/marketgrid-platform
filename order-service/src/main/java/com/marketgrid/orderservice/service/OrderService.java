package com.marketgrid.orderservice.service;

import com.marketgrid.orderservice.client.ProductServiceClient;
import com.marketgrid.orderservice.client.StockDecreaseRequest;
import com.marketgrid.orderservice.dto.CartItemResponse;
import com.marketgrid.orderservice.dto.CartResponse;
import com.marketgrid.orderservice.dto.OrderResponse;
import com.marketgrid.orderservice.entity.Order;
import com.marketgrid.orderservice.entity.OrderItem;
import com.marketgrid.orderservice.entity.OrderItemStatus;
import com.marketgrid.orderservice.exception.ServiceUnavailableException;
import com.marketgrid.orderservice.repository.OrderItemRepository;
import com.marketgrid.orderservice.repository.OrderRepository;
import com.marketgrid.orderservice.security.ServiceAuthTokenProvider;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service managing customer checkout, orders, and vendor-scoped order tracking.
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartService cartService;
    private final ProductServiceClient productServiceClient;
    private final ServiceAuthTokenProvider serviceAuthTokenProvider;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        CartService cartService,
                        ProductServiceClient productServiceClient,
                        ServiceAuthTokenProvider serviceAuthTokenProvider) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartService = cartService;
        this.productServiceClient = productServiceClient;
        this.serviceAuthTokenProvider = serviceAuthTokenProvider;
    }

    /**
     * Executes the checkout process for the customer's cart.
     *
     * <p>TODO: Distributed transaction consistency between order-service and product-service
     * is a known simplification for this project; a production system would use a saga pattern
     * or event-driven compensation.</p>
     *
     * @param userId the ID of the customer checking out
     * @return the created Order with line item outcomes
     * @throws IllegalArgumentException if cart is empty
     * @throws IllegalStateException if all items fail to checkout
     */
    @Transactional
    public OrderResponse checkout(Long userId) {
        // 1. Fetch user's cart
        CartResponse cart = cartService.getCart(userId);
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cannot checkout with an empty cart");
        }

        // 2. Prepare new Order
        Order order = new Order(userId);

        // 3. Process each CartItem
        String adminToken = serviceAuthTokenProvider.getAdminToken();
        String authHeader = "Bearer " + adminToken;

        BigDecimal confirmedTotal = BigDecimal.ZERO;
        int confirmedCount = 0;

        for (CartItemResponse cartItem : cart.getItems()) {
            OrderItemStatus status;
            try {
                // Call product-service stock decrement using internal ADMIN credentials
                productServiceClient.decreaseStock(
                        cartItem.getProductId(),
                        new StockDecreaseRequest(cartItem.getQuantity()),
                        authHeader
                );
                status = OrderItemStatus.CONFIRMED;
                confirmedCount++;
                BigDecimal lineTotal = cartItem.getPriceAtAddTime()
                        .multiply(BigDecimal.valueOf(cartItem.getQuantity()));
                confirmedTotal = confirmedTotal.add(lineTotal);
            } catch (FeignException e) {
                if (e.status() == 400 || e.status() == 404 || e.status() == 409) {
                    // Item-level business failure: out of stock, product removed, or concurrency conflict
                    log.warn("Stock decrement failed for product {} (HTTP {}): {}",
                            cartItem.getProductId(), e.status(), e.getMessage());
                    status = OrderItemStatus.FAILED;
                } else {
                    // product-service is down, unreachable, or 5xx/RetryableException
                    log.error("product-service is unavailable during stock decrement for product {}: {}",
                            cartItem.getProductId(), e.getMessage());
                    throw new ServiceUnavailableException(
                            "Product service is temporarily unavailable, please try again shortly", e);
                }
            } catch (Exception e) {
                log.warn("Failed to decrement stock for product {} during checkout: {}",
                        cartItem.getProductId(), e.getMessage());
                status = OrderItemStatus.FAILED;
            }

            OrderItem orderItem = new OrderItem(
                    order,
                    cartItem.getProductId(),
                    cartItem.getVendorId(),
                    cartItem.getQuantity(),
                    cartItem.getPriceAtAddTime(),
                    status
            );
            order.getItems().add(orderItem);
        }

        // 4. Validate partial/complete success
        if (confirmedCount == 0) {
            // All items failed — leave cart intact and abort order creation
            throw new IllegalStateException(
                    "All items in cart failed to checkout due to stock or service unavailability. Cart preserved.");
        }

        // 5. Set confirmed total amount and persist
        order.setTotalAmount(confirmedTotal);
        Order savedOrder = orderRepository.save(order);

        // 6. Clear user's cart upon success
        cartService.clearCart(userId);

        return OrderResponse.fromEntity(savedOrder);
    }

    /**
     * Fetch a specific order by ID and verify customer ownership.
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + orderId));

        if (!order.getUserId().equals(userId)) {
            throw new SecurityException("You do not own this order");
        }

        return OrderResponse.fromEntity(order);
    }

    /**
     * List all past orders for a customer, grouped by vendor.
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUser(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(OrderResponse::fromEntity)
                .toList();
    }

    /**
     * List orders containing items belonging to a specific vendor.
     * Crucially filters items so the vendor only sees their own items, not items
     * from other vendors within the same customer order.
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByVendor(Long vendorId) {
        List<OrderItem> vendorItems = orderItemRepository.findByVendorId(vendorId);

        Map<Order, List<OrderItem>> itemsByOrder = vendorItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrder));

        return itemsByOrder.entrySet().stream()
                .sorted(Map.Entry.<Order, List<OrderItem>>comparingByKey(
                        Comparator.comparing(Order::getCreatedAt).reversed()))
                .map(entry -> OrderResponse.fromEntityWithItems(entry.getKey(), entry.getValue()))
                .toList();
    }
}
