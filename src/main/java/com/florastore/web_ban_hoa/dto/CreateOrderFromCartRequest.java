package com.florastore.web_ban_hoa.dto;

import com.florastore.web_ban_hoa.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateOrderFromCartRequest(
        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,

        Long addressId,

        @PositiveOrZero(message = "Redeem points must be zero or positive")
        Integer redeemPoints
) {
}
