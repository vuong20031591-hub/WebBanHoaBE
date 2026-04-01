package com.florastore.web_ban_hoa.service;

import com.florastore.web_ban_hoa.dto.CreateOrderRequest;
import com.florastore.web_ban_hoa.dto.OrderResponse;

public interface OrderService {
    OrderResponse createOrder(String userId, CreateOrderRequest request);

    OrderResponse getOrder(String userId, Long orderId);

    OrderResponse getLatestOrder(String userId);

    OrderResponse confirmCodOrder(String userId, Long orderId);
}
