package com.florastore.web_ban_hoa.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
    public static LoginResponse of(String accessToken, long expiresInMs, UserResponse user) {
        return new LoginResponse(
                accessToken,
                "Bearer",
                expiresInMs / 1000,
                user
        );
    }
}
