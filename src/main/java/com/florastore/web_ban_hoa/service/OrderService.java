package com.florastore.web_ban_hoa.service;

import com.florastore.web_ban_hoa.dto.CreateOrderRequest;
import com.florastore.web_ban_hoa.dto.OrderResponse;

public interface OrderService {
    OrderResponse createOrder(CreateOrderRequest request);

    OrderResponse getOrder(Long orderId);

    OrderResponse confirmCodOrder(Long orderId);
}
