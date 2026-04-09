package com.florastore.web_ban_hoa.dto;

import com.florastore.web_ban_hoa.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminCreateUserRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email is invalid")
        String email,

        @NotBlank(message = "Full name is required")
        @Size(max = 120, message = "Full name must be at most 120 characters")
        String fullName,

        @NotBlank(message = "Phone is required")
        @Size(max = 20, message = "Phone must be at most 20 characters")
        String phone,

        @NotBlank(message = "Password is required")
        String password,

        @NotNull(message = "Role is required")
        Role role,

        @Size(max = 1024, message = "Avatar URL must be at most 1024 characters")
        String avatarUrl
) {
}
