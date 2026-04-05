package com.florastore.web_ban_hoa.mapper;

import com.florastore.web_ban_hoa.dto.OrderResponse;
import com.florastore.web_ban_hoa.entity.Order;
import com.florastore.web_ban_hoa.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class OrderResponseMapper {

    public OrderResponse toResponse(Order order) {
        return OrderResponse.fromEntity(order);
    }

    public OrderResponse toResponse(Order order, User user) {
        return OrderResponse.fromEntity(
                order,
                user != null ? user.getFullName() : null,
                user != null ? user.getEmail() : null
        );
    }

    public List<OrderResponse> toResponses(List<Order> orders) {
        return orders.stream()
                .map(this::toResponse)
                .toList();
    }

    public List<OrderResponse> toResponses(List<Order> orders, Map<String, User> usersByOrderUserId) {
        return orders.stream()
                .map(order -> toResponse(order, usersByOrderUserId.get(order.getUserId())))
                .toList();
    }
}
