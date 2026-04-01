package com.florastore.web_ban_hoa.dto;

import java.time.LocalDateTime;

public record PaymentCheckoutResponse(
        Long orderId,
        String provider,
        String checkoutUrl,
        String qrContent,
        String note,
        String transactionId,
        LocalDateTime expiresAt,
        long expiresInSeconds
) {
}
