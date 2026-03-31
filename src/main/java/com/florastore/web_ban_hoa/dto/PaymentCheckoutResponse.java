package com.florastore.web_ban_hoa.dto;

public record PaymentCheckoutResponse(
        Long orderId,
        String provider,
        String checkoutUrl,
        String qrContent,
        String note
) {
}
