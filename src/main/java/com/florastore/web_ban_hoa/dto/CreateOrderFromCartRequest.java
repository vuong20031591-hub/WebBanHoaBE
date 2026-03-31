package com.florastore.web_ban_hoa.dto;

import com.florastore.web_ban_hoa.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record CreateOrderFromCartRequest(
        @NotNull(message = "paymentMethod is required")
        PaymentMethod paymentMethod
) {}
