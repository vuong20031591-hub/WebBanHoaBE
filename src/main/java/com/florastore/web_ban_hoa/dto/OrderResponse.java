package com.florastore.web_ban_hoa.dto;

import com.florastore.web_ban_hoa.entity.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String userId,
        BigDecimal totalAmount,
        String paymentMethod,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime confirmedAt,
        List<OrderItemResponse> items
) {
    public static OrderResponse fromEntity(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getTotalAmount(),
                order.getPaymentMethod().name(),
                order.getStatus().name(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getConfirmedAt(),
                null
        );
    }

    public static OrderResponse fromEntityWithItems(Order order, List<OrderItemResponse> items) {
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getTotalAmount(),
                order.getPaymentMethod().name(),
                order.getStatus().name(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getConfirmedAt(),
                items
        );
    }
}
