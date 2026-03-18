package com.florastore.web_ban_hoa.dto;

import com.florastore.web_ban_hoa.entity.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProductResponse {

    private Long id;
    private String name;
    private BigDecimal price;
    private String description;
    private String image;
    private LocalDateTime createdAt;
    private Long categoryId;
    private String categoryName;

    public static ProductResponse fromEntity(Product product) {
        ProductResponse response = new ProductResponse();
        response.id = product.getId();
        response.name = product.getName();
        response.price = product.getPrice();
        response.description = product.getDescription();
        response.image = product.getImage();
        response.createdAt = product.getCreatedAt();
        response.categoryId = product.getCategory().getId();
        response.categoryName = product.getCategory().getName();
        return response;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getDescription() {
        return description;
    }

    public String getImage() {
        return image;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }
}
