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
        List<OrderItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime confirmedAt
) {
    public static OrderResponse fromEntity(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(OrderItemResponse::fromEntity)
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getTotalAmount(),
                order.getPaymentMethod().name(),
                order.getStatus().name(),
                items,
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getConfirmedAt()
        );
    }
}
