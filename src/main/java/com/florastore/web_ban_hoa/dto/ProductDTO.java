package com.florastore.web_ban_hoa.dto;

public record ProductDTO(
        Long id,
        String name,
        BigDecimal price,
        String description,
        String imageUrl,
        String categoryName
) {
}
