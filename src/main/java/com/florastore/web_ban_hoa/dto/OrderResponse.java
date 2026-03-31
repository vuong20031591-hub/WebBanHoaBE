package com.florastore.web_ban_hoa.dto;

import com.florastore.web_ban_hoa.entity.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderResponse(
        Long id,
        String userId,
        BigDecimal totalAmount,
        String paymentMethod,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime confirmedAt
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
                order.getConfirmedAt()
        );
    }
}
