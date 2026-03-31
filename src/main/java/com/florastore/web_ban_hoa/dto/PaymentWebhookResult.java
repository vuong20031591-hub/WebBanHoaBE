package com.florastore.web_ban_hoa.dto;

public record PaymentWebhookResult(
        String status,
        String message,
        Long orderId,
        String providerTransactionId
) {
}
