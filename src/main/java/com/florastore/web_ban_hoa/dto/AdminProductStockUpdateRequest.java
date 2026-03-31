package com.florastore.web_ban_hoa.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AdminProductStockUpdateRequest(
        @NotNull(message = "stockQuantity is required")
        @Min(value = 0, message = "stockQuantity must be at least 0")
        Integer stockQuantity
) {
}
