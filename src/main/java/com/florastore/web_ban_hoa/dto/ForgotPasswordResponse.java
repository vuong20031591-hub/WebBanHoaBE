package com.florastore.web_ban_hoa.dto;

public record ForgotPasswordResponse(
        String deliveryMethod,
        String message,
        String debugCode
) {
}
