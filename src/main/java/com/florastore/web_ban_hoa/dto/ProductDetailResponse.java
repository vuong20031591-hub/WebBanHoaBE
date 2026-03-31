package com.florastore.web_ban_hoa.dto;

import com.florastore.web_ban_hoa.entity.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductDetailResponse(
        Long id,
        String name,
        BigDecimal price,
        String description,
        String image,
        Integer stockQuantity,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt,
        Long categoryId,
        String categoryName
) {
    public static ProductDetailResponse fromEntity(Product product) {
        return new ProductDetailResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getDescription(),
                product.getImage(),
                product.getStockQuantity(),
                product.getCreatedAt(),
                product.getUpdatedAt(),
                product.getDeletedAt(),
                product.getCategory().getId(),
                product.getCategory().getName()
        );
    }
}
