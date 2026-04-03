package com.florastore.web_ban_hoa.dto;

import com.florastore.web_ban_hoa.entity.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AdminCreateOrderRequest(
        @NotBlank(message = "customerEmail is required")
        @Email(message = "customerEmail must be a valid email")
        String customerEmail,

        @NotNull(message = "paymentMethod is required")
        PaymentMethod paymentMethod,

        @NotEmpty(message = "items are required")
        List<@Valid AdminCreateOrderItemRequest> items
) {
}
