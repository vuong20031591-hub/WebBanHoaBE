package com.florastore.web_ban_hoa.dto;

import java.math.BigDecimal;

public record ProductDTO(
        Long id,
        String name,
        BigDecimal price,
        String description,
        String imageUrl,
        String categoryName
) {
}
