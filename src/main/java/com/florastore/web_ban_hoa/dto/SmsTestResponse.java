package com.florastore.web_ban_hoa.dto;

public record SmsTestResponse(
        String status,
        String phone,
        String provider,
        String detail
) {
}
