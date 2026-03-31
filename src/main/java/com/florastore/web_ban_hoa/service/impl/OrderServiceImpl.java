package com.florastore.web_ban_hoa.service.impl;

import com.florastore.web_ban_hoa.dto.*;
import com.florastore.web_ban_hoa.entity.*;
import com.florastore.web_ban_hoa.repository.CartRepository;
import com.florastore.web_ban_hoa.repository.OrderRepository;
import com.florastore.web_ban_hoa.repository.ProductRepository;
import com.florastore.web_ban_hoa.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final ProductRepository productRepository;

    public OrderServiceImpl(OrderRepository orderRepository, CartRepository cartRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
    }

    @Override
    public OrderResponse createOrder(CreateOrderRequest request) {
        Order order = new Order(request.userId(), request.totalAmount(), request.paymentMethod());
        return OrderResponse.fromEntity(orderRepository.save(order));
    }

    @Override
    public OrderResponse createOrderFromCart(String userId, CreateOrderFromCartRequest request) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        Order order = new Order(userId, totalAmount, request.paymentMethod());

        for (CartItem cartItem : cart.getItems()) {
            Product product = productRepository.findById(cartItem.getProduct().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Insufficient stock for product: " + product.getName());
            }

            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);

            BigDecimal lineTotal = cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);

            OrderItem orderItem = new OrderItem(
                    order,
                    product.getId(),
                    product.getName(),
                    cartItem.getQuantity(),
                    cartItem.getPrice()
            );
            order.getItems().add(orderItem);
        }

        order.setTotalAmount(totalAmount);
        order = orderRepository.save(order);

        cart.getItems().clear();
        cartRepository.save(cart);

        return OrderResponse.fromEntity(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId) {
        return OrderResponse.fromEntity(getOrderOrThrow(orderId));
    }

    @Override
    public OrderResponse confirmCodOrder(Long orderId) {
        Order order = getOrderOrThrow(orderId);
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

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getOrdersWithFilters(
            OrderStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String search,
            Pageable pageable
    ) {
        Page<Order> page = orderRepository.findByFilters(status, startDate, endDate, search, pageable);
        return PagedResponse.from(page.map(OrderResponse::fromEntity));
    }

    @Override
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = getOrderOrThrow(orderId);
        OrderStatus currentStatus = order.getStatus();

        if (!isValidStatusTransition(currentStatus, newStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                String.format("Invalid status transition from %s to %s", currentStatus, newStatus));
        }

        order.setStatus(newStatus);
        if (newStatus == OrderStatus.CONFIRMED && order.getConfirmedAt() == null) {
            order.setConfirmedAt(LocalDateTime.now());
        }
        
        return OrderResponse.fromEntity(orderRepository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminOrderStatsResponse getOrderStats() {
        long pendingCount = orderRepository.countByStatus(OrderStatus.PENDING);
        long confirmedCount = orderRepository.countByStatus(OrderStatus.CONFIRMED);
        long cancelledCount = orderRepository.countByStatus(OrderStatus.CANCELLED);
        long totalCount = orderRepository.count();
        
        return new AdminOrderStatsResponse(pendingCount, confirmedCount, cancelledCount, totalCount);
    }

    private boolean isValidStatusTransition(OrderStatus current, OrderStatus next) {
        if (current == next) {
            return false;
        }
        
        return switch (current) {
            case PENDING -> next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED;
            case CONFIRMED -> next == OrderStatus.CANCELLED;
            case CANCELLED -> false;
        };
    }

    private Order getOrderOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }
}
