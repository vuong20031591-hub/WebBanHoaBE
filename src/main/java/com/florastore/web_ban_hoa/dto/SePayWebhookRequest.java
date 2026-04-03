package com.florastore.web_ban_hoa.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SePayWebhookRequest(
        @NotNull(message = "id is required")
        Long id,

        @NotBlank(message = "gateway is required")
        String gateway,

        @NotBlank(message = "transactionDate is required")
        String transactionDate,

        @NotBlank(message = "accountNumber is required")
        String accountNumber,

        String code,
        String content,

        @NotBlank(message = "transferType is required")
        String transferType,

        @NotNull(message = "transferAmount is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "transferAmount must be greater than 0")
        BigDecimal transferAmount,

        BigDecimal accumulated,
        String subAccount,
        String referenceCode,
        String description
) {
}
