package com.florastore.web_ban_hoa.service;

import com.florastore.web_ban_hoa.dto.*;
import com.florastore.web_ban_hoa.entity.OrderStatus;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface OrderService {
    OrderResponse createOrder(CreateOrderRequest request);
    OrderResponse createOrderFromCart(String userId, CreateOrderFromCartRequest request);
    OrderResponse getOrder(Long orderId);
    OrderResponse confirmCodOrder(Long orderId);
    
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
