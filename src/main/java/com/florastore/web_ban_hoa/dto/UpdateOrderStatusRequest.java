package com.florastore.web_ban_hoa.dto;

import com.florastore.web_ban_hoa.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @NotNull(message = "Status is required")
        OrderStatus status
) {
}
