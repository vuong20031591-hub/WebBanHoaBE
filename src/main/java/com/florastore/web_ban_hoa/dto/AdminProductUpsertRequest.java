package com.florastore.web_ban_hoa.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AdminProductUpsertRequest(
        @NotBlank(message = "name is required")
        String name,

        @NotNull(message = "price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "price must be greater than 0")
        BigDecimal price,

        String description,
        String image,

        @NotNull(message = "stockQuantity is required")
        @Min(value = 0, message = "stockQuantity must be at least 0")
        Integer stockQuantity,

        @NotNull(message = "categoryId is required")
        Long categoryId
) {
}
