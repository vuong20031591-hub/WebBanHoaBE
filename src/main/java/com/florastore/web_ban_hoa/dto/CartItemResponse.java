package com.florastore.web_ban_hoa.dto;

import java.math.BigDecimal;

public record CartItemResponse(
        Long id,
        Long productId,
        String productName,
        String image,
        Integer quantity,
        BigDecimal price,
        BigDecimal lineTotal,
        Integer availableStock
) {
}
