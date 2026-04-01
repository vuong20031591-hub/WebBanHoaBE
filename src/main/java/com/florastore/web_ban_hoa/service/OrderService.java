package com.florastore.web_ban_hoa.service;

import com.florastore.web_ban_hoa.dto.*;
import com.florastore.web_ban_hoa.entity.OrderStatus;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderService {
    OrderResponse createOrder(String userId, CreateOrderRequest request);

    OrderResponse createOrderFromCart(String userId, CreateOrderFromCartRequest request);

    OrderResponse getOrder(String userId, Long orderId);

    OrderResponse getLatestOrder(String userId);

    OrderResponse confirmCodOrder(String userId, Long orderId);

    List<OrderResponse> getUserOrders(String userId);

    PagedResponse<OrderResponse> getOrdersWithFilters(
            OrderStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String search,
            Pageable pageable
    );

    OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus);

    AdminOrderStatsResponse getOrderStats();
}
