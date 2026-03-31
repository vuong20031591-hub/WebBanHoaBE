package com.florastore.web_ban_hoa.dto;

public record AuthResponse(
        String token,
        String email,
        String fullName,
        String role
) {}
