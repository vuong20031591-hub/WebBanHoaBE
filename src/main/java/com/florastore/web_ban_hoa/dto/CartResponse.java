package com.florastore.web_ban_hoa.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CartResponse(
        Long cartId,
        String userId,
        Integer totalItems,
        BigDecimal totalAmount,
        List<CartItemResponse> items,
        LocalDateTime updatedAt
) {
}
