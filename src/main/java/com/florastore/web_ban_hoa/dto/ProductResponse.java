package com.florastore.web_ban_hoa.dto;

import com.florastore.web_ban_hoa.entity.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String name,
        BigDecimal price,
        String description,
        String image,
    Integer stockQuantity,
        LocalDateTime createdAt,
        Long categoryId,
        String categoryName
) {
    public static ProductResponse fromEntity(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getDescription(),
                product.getImage(),
                product.getStockQuantity(),
                product.getCreatedAt(),
                product.getCategory().getId(),
                product.getCategory().getName()
        );
    }
}
