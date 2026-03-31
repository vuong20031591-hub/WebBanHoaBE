package com.florastore.web_ban_hoa.dto;

import com.florastore.web_ban_hoa.entity.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateOrderRequest(
        @NotBlank(message = "userId is required")
        String userId,

        @NotNull(message = "totalAmount is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "totalAmount must be greater than 0")
        BigDecimal totalAmount,

        @NotNull(message = "paymentMethod is required")
        PaymentMethod paymentMethod
) {
}
