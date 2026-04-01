package com.florastore.web_ban_hoa.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PaymentWebhookRequest(
        @NotBlank(message = "providerTransactionId is required")
        String providerTransactionId,

        @NotNull(message = "orderId is required")
        Long orderId,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "amount must be greater than 0")
        BigDecimal amount,

        String transactionContent,
        String signature,
        String secret
) {
}
