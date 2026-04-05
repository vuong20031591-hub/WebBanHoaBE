package com.florastore.web_ban_hoa.dto;

public record TwoFactorSmsCodeResponse(
        String deliveryMethod,
        String message,
        String maskedPhone
) {
}
