package com.florastore.web_ban_hoa.dto;

import com.florastore.web_ban_hoa.entity.RewardsTransaction;

import java.time.LocalDateTime;

public record RewardsTransactionResponse(
        Long id,
        Integer points,
        String type,
        String description,
        Long orderId,
        LocalDateTime createdAt
) {
    public static RewardsTransactionResponse from(RewardsTransaction transaction) {
        return new RewardsTransactionResponse(
                transaction.getId(),
                transaction.getPoints(),
                transaction.getType(),
                transaction.getDescription(),
                transaction.getOrderId(),
                transaction.getCreatedAt()
        );
    }
}
