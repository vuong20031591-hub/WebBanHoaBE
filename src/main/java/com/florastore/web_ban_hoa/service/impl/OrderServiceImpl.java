package com.florastore.web_ban_hoa.service.impl;

import com.florastore.web_ban_hoa.dto.CreateOrderRequest;
import com.florastore.web_ban_hoa.dto.OrderResponse;
import com.florastore.web_ban_hoa.entity.Cart;
import com.florastore.web_ban_hoa.entity.CartItem;
import com.florastore.web_ban_hoa.entity.Order;
import com.florastore.web_ban_hoa.entity.OrderStatus;
import com.florastore.web_ban_hoa.entity.PaymentMethod;
import com.florastore.web_ban_hoa.entity.Product;
import com.florastore.web_ban_hoa.repository.CartRepository;
import com.florastore.web_ban_hoa.repository.OrderRepository;
import com.florastore.web_ban_hoa.service.CartService;
import com.florastore.web_ban_hoa.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartService cartService;

    public OrderServiceImpl(OrderRepository orderRepository, CartRepository cartRepository, CartService cartService) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.cartService = cartService;
    }

    @Override
    public OrderResponse createOrder(String userId, CreateOrderRequest request) {
        Cart cart = loadCartOrThrow(userId);
        if (cart.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot create order from empty cart");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            if (product == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart contains invalid product");
            }

            Integer stockQuantity = product.getStockQuantity();
            if (stockQuantity == null || item.getQuantity() > stockQuantity) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Insufficient stock for product id " + product.getId()
                );
            }

            totalAmount = totalAmount.add(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        Order order = new Order(userId, totalAmount, request.paymentMethod());
        Order savedOrder = orderRepository.save(order);
        cartService.clearCart(userId);
        return OrderResponse.fromEntity(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrder(String userId, Long orderId) {
        return OrderResponse.fromEntity(getOwnedOrderOrThrow(userId, orderId));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getLatestOrder(String userId) {
        Order order = orderRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        return OrderResponse.fromEntity(order);
    }

    @Override
    public OrderResponse confirmCodOrder(String userId, Long orderId) {
        Order order = getOwnedOrderOrThrow(userId, orderId);
        if (order.getPaymentMethod() != PaymentMethod.COD) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order payment method is not COD");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PENDING order can be confirmed");
        }

        order.setStatus(OrderStatus.CONFIRMED);
        order.setConfirmedAt(LocalDateTime.now());
        return OrderResponse.fromEntity(orderRepository.save(order));
    }

    private Cart loadCartOrThrow(String userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart not found"));
    }

    private Order getOwnedOrderOrThrow(String userId, Long orderId) {
        return orderRepository.findById(orderId)
                .filter(order -> order.getUserId().equals(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }
}
