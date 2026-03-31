package com.florastore.web_ban_hoa.dto;

import com.florastore.web_ban_hoa.entity.User;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        String phone,
        String role
) {
    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getRole().name()
        );
    }
}
