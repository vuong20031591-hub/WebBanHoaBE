package com.florastore.web_ban_hoa.service.impl;

import com.florastore.web_ban_hoa.dto.*;
import com.florastore.web_ban_hoa.entity.*;
import com.florastore.web_ban_hoa.repository.AddressRepository;
import com.florastore.web_ban_hoa.repository.CartRepository;
import com.florastore.web_ban_hoa.repository.OrderItemRepository;
import com.florastore.web_ban_hoa.repository.OrderRepository;
import com.florastore.web_ban_hoa.repository.ProductRepository;
import com.florastore.web_ban_hoa.service.CartService;
import com.florastore.web_ban_hoa.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartService cartService;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            CartRepository cartRepository,
            CartService cartService,
            OrderItemRepository orderItemRepository,
            ProductRepository productRepository,
            AddressRepository addressRepository) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.cartService = cartService;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.addressRepository = addressRepository;
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
    public OrderResponse createOrderFromCart(String userId, CreateOrderFromCartRequest request) {
        Cart cart = loadCartOrThrow(userId);
        if (cart.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot create order from empty cart");
        }

        if (request.addressId() != null) {
            addressRepository.findByIdAndUserId(request.addressId(), userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid address"));
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

        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            OrderItem orderItem = new OrderItem(
                    savedOrder.getId(),
                    product.getId(),
                    product.getName(),
                    item.getQuantity(),
                    product.getPrice()
            );
            orderItemRepository.save(orderItem);

            product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
            productRepository.save(product);
        }

        cartService.clearCart(userId);

        List<OrderItemResponse> items = orderItemRepository.findByOrderId(savedOrder.getId())
                .stream()
                .map(OrderItemResponse::from)
                .toList();

        return OrderResponse.fromEntityWithItems(savedOrder, items);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrder(String userId, Long orderId) {
        Order order = getOwnedOrderOrThrow(userId, orderId);
        List<OrderItemResponse> items = orderItemRepository.findByOrderId(orderId)
                .stream()
                .map(OrderItemResponse::from)
                .toList();
        return OrderResponse.fromEntityWithItems(order, items);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getLatestOrder(String userId) {
        Order order = orderRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        List<OrderItemResponse> items = orderItemRepository.findByOrderId(order.getId())
                .stream()
                .map(OrderItemResponse::from)
                .toList();
        return OrderResponse.fromEntityWithItems(order, items);
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
        Order savedOrder = orderRepository.save(order);

        List<OrderItemResponse> items = orderItemRepository.findByOrderId(orderId)
                .stream()
                .map(OrderItemResponse::from)
                .toList();
        return OrderResponse.fromEntityWithItems(savedOrder, items);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrders(String userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(order -> {
                    List<OrderItemResponse> items = orderItemRepository.findByOrderId(order.getId())
                            .stream()
                            .map(OrderItemResponse::from)
                            .toList();
                    return OrderResponse.fromEntityWithItems(order, items);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getOrdersForAdmin(
            OrderStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String searchUserId,
            Pageable pageable) {

        Page<Order> orderPage;

        if (status != null && startDate != null && endDate != null) {
            orderPage = orderRepository.findByStatusAndCreatedAtBetweenOrderByCreatedAtDesc(status, startDate, endDate, pageable);
        } else if (status != null) {
            orderPage = orderRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else if (startDate != null && endDate != null) {
            orderPage = orderRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(startDate, endDate, pageable);
        } else {
            orderPage = orderRepository.findAll(pageable);
        }

        List<OrderResponse> content = orderPage.getContent()
                .stream()
                .filter(order -> searchUserId == null || order.getUserId().contains(searchUserId))
                .map(order -> {
                    List<OrderItemResponse> items = orderItemRepository.findByOrderId(order.getId())
                            .stream()
                            .map(OrderItemResponse::from)
                            .toList();
                    return OrderResponse.fromEntityWithItems(order, items);
                })
                .toList();

        return new PagedResponse<>(
                content,
                orderPage.getTotalElements(),
                orderPage.getTotalPages(),
                orderPage.getNumber(),
                orderPage.getSize()
        );
    }

    @Override
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        OrderStatus currentStatus = order.getStatus();

        if (currentStatus == OrderStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot change status of cancelled order");
        }

        if (currentStatus == OrderStatus.PENDING && newStatus == OrderStatus.CANCELLED) {
            order.setStatus(newStatus);
        } else if (currentStatus == OrderStatus.PENDING && newStatus == OrderStatus.CONFIRMED) {
            order.setStatus(newStatus);
            order.setConfirmedAt(LocalDateTime.now());
        } else if (currentStatus == OrderStatus.CONFIRMED && newStatus == OrderStatus.CANCELLED) {
            order.setStatus(newStatus);
        } else {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid status transition from " + currentStatus + " to " + newStatus
            );
        }

        Order savedOrder = orderRepository.save(order);

        List<OrderItemResponse> items = orderItemRepository.findByOrderId(savedOrder.getId())
                .stream()
                .map(OrderItemResponse::from)
                .toList();

        return OrderResponse.fromEntityWithItems(savedOrder, items);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminOrderStatsResponse getOrderStats() {
        Long total = orderRepository.count();
        Long pending = orderRepository.countByStatus(OrderStatus.PENDING);
        Long confirmed = orderRepository.countByStatus(OrderStatus.CONFIRMED);
        Long cancelled = orderRepository.countByStatus(OrderStatus.CANCELLED);

        return new AdminOrderStatsResponse(total, pending, confirmed, cancelled);
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
