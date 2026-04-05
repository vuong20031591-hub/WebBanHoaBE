package com.florastore.web_ban_hoa.service.impl;

import com.florastore.web_ban_hoa.dto.*;
import com.florastore.web_ban_hoa.entity.*;
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
import java.util.List;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartService cartService;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final RewardsService rewardsService;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            CartRepository cartRepository,
            CartService cartService,
            OrderItemRepository orderItemRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            RewardsService rewardsService) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.cartService = cartService;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.rewardsService = rewardsService;
    }

    @Override
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
        return OrderResponse.fromEntity(finalOrder);
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

        return OrderResponse.fromEntity(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrder(String userId, Long orderId) {
        Order order = getOwnedOrderOrThrow(userId, orderId);
        return OrderResponse.fromEntity(order);
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

        return OrderResponse.fromEntity(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrders(String userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(OrderResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getOrdersWithFilters(
            OrderStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String search,
            Pageable pageable) {

        Page<Order> orderPage;
        boolean hasDateFilter = startDate != null || endDate != null;

        if (hasDateFilter) {
            LocalDateTime effectiveStart = startDate != null
                    ? startDate
                    : LocalDateTime.of(1970, 1, 1, 0, 0);
            LocalDateTime effectiveEnd = endDate != null
                    ? endDate
                    : LocalDateTime.of(3000, 1, 1, 0, 0);

            if (status != null) {
                orderPage = orderRepository.findByStatusAndCreatedAtBetweenOrderByCreatedAtDesc(
                        status,
                        effectiveStart,
                        effectiveEnd,
                        pageable
                );
            } else {
                orderPage = orderRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(
                        effectiveStart,
                        effectiveEnd,
                        pageable
                );
            }
        } else if (status != null) {
            orderPage = orderRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            orderPage = orderRepository.findAll(pageable);
        }

        // Fix N+1 query: fetch all items in one query
        List<Order> orders = orderPage.getContent();
        if (!orders.isEmpty()) {
            List<Long> orderIds = orders.stream().map(Order::getId).toList();
            orderRepository.findAllWithItemsByIdIn(orderIds);
        }

        return PagedResponse.from(orderPage.map(OrderResponse::fromEntity));
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
        if (savedOrder.getStatus() == OrderStatus.CANCELLED) {
            rollbackRewardsIfCancelled(savedOrder);
        } else {
            awardRewardsIfConfirmed(savedOrder);
        }

        return OrderResponse.fromEntity(savedOrder);
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
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid user id for rewards processing");
        }
    }
}
