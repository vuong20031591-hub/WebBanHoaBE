package com.florastore.web_ban_hoa.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record EventInquiryRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 120, message = "Full name must not exceed 120 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 254, message = "Email must not exceed 254 characters")
        String email,

        @NotBlank(message = "Event type is required")
        @Size(max = 80, message = "Event type must not exceed 80 characters")
        String eventType,

        @NotNull(message = "Event date is required")
        @FutureOrPresent(message = "Event date must be today or in the future")
        LocalDate eventDate,

        @NotBlank(message = "Vision is required")
        @Size(max = 2000, message = "Vision must not exceed 2000 characters")
        String vision
) {
}
