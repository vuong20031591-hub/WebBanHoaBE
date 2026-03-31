package com.florastore.web_ban_hoa.dto;

public record SignedUrlResponse(
        String key,
        String signedUrl,
        long expiresInSeconds
) {
}
