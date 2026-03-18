package com.florastore.web_ban_hoa.dto;

public record ProductDTO(
        Long id,
        String name,
        Long price,
        String description,
        String imageUrl,
        String categoryName
) {
}
