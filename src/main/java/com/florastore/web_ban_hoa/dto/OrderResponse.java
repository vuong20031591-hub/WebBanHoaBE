package com.florastore.web_ban_hoa.dto;

import com.florastore.web_ban_hoa.entity.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String userId,
        String userFullName,
        String userEmail,
        BigDecimal totalAmount,
        Integer redeemedPoints,
        BigDecimal rewardsDiscountAmount,
        String paymentMethod,
        String status,
        List<OrderItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime confirmedAt
) {
    public static OrderResponse fromEntity(Order order) {
        return fromEntity(order, null, null);
    }

    public static OrderResponse fromEntity(Order order, String userFullName, String userEmail) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(OrderItemResponse::from)
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                userFullName,
                userEmail,
                order.getTotalAmount(),
                order.getRedeemedPoints(),
                order.getRewardsDiscountAmount(),
                order.getPaymentMethod().name(),
                order.getStatus().name(),
                items,
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getConfirmedAt()
        );
    }
}
