package com.florastore.web_ban_hoa.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateAddressRequest(
        @NotBlank(message = "Full name is required")
        String fullName,

        @NotBlank(message = "Phone is required")
        String phone,

        @NotBlank(message = "Address is required")
        String address,

        @NotBlank(message = "City is required")
        String city,

        @NotBlank(message = "District is required")
        String district,

        String ward
) {}
