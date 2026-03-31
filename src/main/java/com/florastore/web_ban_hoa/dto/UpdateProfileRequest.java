package com.florastore.web_ban_hoa.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(
        @NotBlank(message = "Full name is required")
        String fullName,
        String phone
) {}
