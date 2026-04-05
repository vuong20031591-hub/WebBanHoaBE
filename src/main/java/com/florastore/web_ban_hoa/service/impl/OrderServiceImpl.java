package com.florastore.web_ban_hoa.service.impl;

import com.florastore.web_ban_hoa.dto.*;
import com.florastore.web_ban_hoa.entity.*;
import com.florastore.web_ban_hoa.mapper.OrderResponseMapper;
import com.florastore.web_ban_hoa.repository.CartRepository;
import com.florastore.web_ban_hoa.repository.OrderItemRepository;
import com.florastore.web_ban_hoa.repository.OrderRepository;
import com.florastore.web_ban_hoa.repository.ProductRepository;
import com.florastore.web_ban_hoa.repository.UserRepository;
import com.florastore.web_ban_hoa.service.CartService;
import com.florastore.web_ban_hoa.service.OrderService;
import com.florastore.web_ban_hoa.service.RewardsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartService cartService;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    private final OrderResponseMapper orderResponseMapper;

    private final RewardsService rewardsService;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            CartRepository cartRepository,
            CartService cartService,
            OrderItemRepository orderItemRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            OrderResponseMapper orderResponseMapper,
            RewardsService rewardsService) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.cartService = cartService;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.orderResponseMapper = orderResponseMapper;
        this.rewardsService = rewardsService;
    }

    @Override
    @Transactional
    public OrderResponse createAdminOrder(AdminCreateOrderRequest request) {
        User user = userRepository.findByEmail(request.customerEmail().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));

        BigDecimal totalAmount = BigDecimal.ZERO;
        Order order = new Order(String.valueOf(user.getId()), BigDecimal.ZERO, request.paymentMethod());
        Order savedOrder = orderRepository.save(order);

        for (AdminCreateOrderItemRequest itemRequest : request.items()) {
            Product product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product not found"));

            Integer stockQuantity = product.getStockQuantity();
            if (stockQuantity == null || itemRequest.quantity() > stockQuantity) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Insufficient stock for product id " + product.getId()
                );
            }

            OrderItem orderItem = new OrderItem(
                    savedOrder,
                    product.getId(),
                    product.getName(),
                    itemRequest.quantity(),
                    product.getPrice()
            );
            orderItemRepository.save(orderItem);

            product.setStockQuantity(product.getStockQuantity() - itemRequest.quantity());
            productRepository.save(product);

            totalAmount = totalAmount.add(
                    product.getPrice().multiply(BigDecimal.valueOf(itemRequest.quantity()))
            );
        }

        savedOrder.setTotalAmount(totalAmount);
        Order finalOrder = orderRepository.save(savedOrder);
        return orderResponseMapper.toResponse(finalOrder);
    }

    @Override
    @Transactional
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
        return orderResponseMapper.toResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse createOrderFromCart(String userId, CreateOrderFromCartRequest request) {
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

        Long userIdLong = parseUserId(userId);
        int appliedRedeemPoints = rewardsService.getApplicableRedeemPoints(userIdLong, totalAmount, request.redeemPoints());
        BigDecimal rewardsDiscountAmount = rewardsService.calculateDiscountForPoints(appliedRedeemPoints);
        BigDecimal payableAmount = totalAmount.subtract(rewardsDiscountAmount).max(BigDecimal.ZERO);

        Order order = new Order(userId, payableAmount, request.paymentMethod());
        order.setRedeemedPoints(appliedRedeemPoints);
        order.setRewardsDiscountAmount(rewardsDiscountAmount);
        Order savedOrder = orderRepository.save(order);

        if (appliedRedeemPoints > 0) {
            rewardsService.redeemExactPointsForOrder(userIdLong, appliedRedeemPoints, savedOrder.getId());
        }

        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            OrderItem orderItem = new OrderItem(
                    savedOrder,
                    product.getId(),
                    product.getName(),
                    item.getQuantity(),
                    product.getPrice()
            );
            orderItemRepository.save(orderItem);

            product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
            productRepository.save(product);
        }

        if (payableAmount.signum() == 0) {
            savedOrder.setStatus(OrderStatus.CONFIRMED);
            savedOrder.setConfirmedAt(LocalDateTime.now());
            savedOrder = orderRepository.save(savedOrder);
            awardRewardsIfConfirmed(savedOrder);
        }

        cartService.clearCart(userId);

        return orderResponseMapper.toResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrder(String userId, Long orderId) {
        Order order = getOwnedOrderOrThrow(userId, orderId);
        return orderResponseMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getLatestOrder(String userId) {
        Order order = orderRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        return orderResponseMapper.toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse confirmCodOrder(String userId, Long orderId) {
        Order order = getOwnedOrderOrThrow(userId, orderId);
        if (order.getPaymentMethod() != PaymentMethod.COD) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order payment method is not COD");
        }
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            return OrderResponse.fromEntity(order);
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PENDING order can be confirmed");
        }

        order.setStatus(OrderStatus.CONFIRMED);
        order.setConfirmedAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);
        if (savedOrder.getStatus() == OrderStatus.CANCELLED) {
            rollbackRewardsIfCancelled(savedOrder);
        } else {
            awardRewardsIfConfirmed(savedOrder);
        }

        return orderResponseMapper.toResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrders(String userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(orderResponseMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getOrdersWithFilters(
            OrderStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String search,
            Pageable pageable,
            boolean includeUserProfile) {
        Page<Order> orderPage = findOrdersWithFilters(
                status,
                startDate,
                endDate,
                normalizeNullable(search),
                pageable
        );
        List<Order> orders = hydrateOrdersWithItems(orderPage.getContent());
        List<OrderResponse> responses = toOrderResponses(orders, includeUserProfile);
        return new PagedResponse<>(
                responses,
                orderPage.getTotalElements(),
                orderPage.getTotalPages(),
                orderPage.getNumber(),
                orderPage.getSize()
        );
    }

    @Override
    @Transactional
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
        if (savedOrder.getStatus() == OrderStatus.CANCELLED) {
            rollbackRewardsIfCancelled(savedOrder);
        } else {
            awardRewardsIfConfirmed(savedOrder);
        }

        return orderResponseMapper.toResponse(savedOrder);
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

    private Page<Order> findOrdersWithFilters(
            OrderStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String search,
            Pageable pageable
    ) {
        return orderRepository.findByFilters(status, startDate, endDate, search, pageable);
    }

    private List<Order> hydrateOrdersWithItems(List<Order> orders) {
        if (orders.isEmpty()) {
            return List.of();
        }

        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        Map<Long, Order> ordersByIdWithItems = new HashMap<>();
        for (Order orderWithItems : orderRepository.findAllWithItemsByIdIn(orderIds)) {
            ordersByIdWithItems.put(orderWithItems.getId(), orderWithItems);
        }

        return orders.stream()
                .map(order -> ordersByIdWithItems.getOrDefault(order.getId(), order))
                .toList();
    }

    private List<OrderResponse> toOrderResponses(List<Order> orders, boolean includeUserProfile) {
        if (orders.isEmpty()) {
            return List.of();
        }

        if (!includeUserProfile) {
            return orderResponseMapper.toResponses(orders);
        }

        Map<String, User> usersByOrderUserId = loadUsersByOrderUserId(orders);
        return orderResponseMapper.toResponses(orders, usersByOrderUserId);
    }

    private Map<String, User> loadUsersByOrderUserId(List<Order> orders) {
        Map<String, Long> parsedUserIdsByOrderUserId = new HashMap<>();
        for (Order order : orders) {
            String orderUserId = normalizeNullable(order.getUserId());
            Long parsedUserId = parseNumericUserId(orderUserId);
            if (orderUserId != null && parsedUserId != null) {
                parsedUserIdsByOrderUserId.putIfAbsent(orderUserId, parsedUserId);
            }
        }

        if (parsedUserIdsByOrderUserId.isEmpty()) {
            return Map.of();
        }

        Set<Long> numericUserIds = new HashSet<>(parsedUserIdsByOrderUserId.values());
        List<User> users = userRepository.findAllById(numericUserIds);
        Map<Long, User> usersById = new HashMap<>();
        for (User user : users) {
            usersById.put(user.getId(), user);
        }

        Map<String, User> usersByOrderUserId = new HashMap<>();
        for (Map.Entry<String, Long> entry : parsedUserIdsByOrderUserId.entrySet()) {
            User user = usersById.get(entry.getValue());
            if (user != null) {
                usersByOrderUserId.put(entry.getKey(), user);
            }
        }

        return usersByOrderUserId;
    }

    private Long parseNumericUserId(String rawUserId) {
        if (rawUserId == null) {
            return null;
        }

        try {
            return Long.parseLong(rawUserId);
        } catch (NumberFormatException ignored) {
            // Some legacy records may contain non-numeric userId values.
            return null;
        }
    }

    private void awardRewardsIfConfirmed(Order order) {
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            return;
        }

        rewardsService.awardPointsForOrder(parseUserId(order.getUserId()), order.getTotalAmount(), order.getId());
    }

    private void rollbackRewardsIfCancelled(Order order) {
        if (order.getStatus() != OrderStatus.CANCELLED) {
            return;
        }

        Long userId = parseUserId(order.getUserId());
        rewardsService.rollbackRewardsForCancelledOrder(
                userId,
                order.getId(),
                order.getRedeemedPoints(),
                order.getTotalAmount()
        );
    }

    private Long parseUserId(String userId) {
        Long parsedUserId = parseNumericUserId(userId);
        if (parsedUserId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid user id for rewards processing");
        }
        return parsedUserId;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
