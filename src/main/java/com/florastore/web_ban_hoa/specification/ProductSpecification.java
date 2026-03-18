package com.florastore.web_ban_hoa.specification;

import com.florastore.web_ban_hoa.domain.Product;
import org.springframework.data.jpa.domain.Specification;

public final class ProductSpecification {

    private ProductSpecification() {
    }

    public static Specification<Product> hasName(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isBlank()) {
                return null;
            }
            String keyword = "%" + name.trim().toLowerCase() + "%";
            return cb.like(cb.lower(root.get("name")), keyword);
        };
    }

    public static Specification<Product> hasPriceBetween(Long minPrice, Long maxPrice) {
        return (root, query, cb) -> {
            if (minPrice == null && maxPrice == null) {
                return null;
            }

            if (minPrice != null && maxPrice != null) {
                long lowerBound = Math.min(minPrice, maxPrice);
                long upperBound = Math.max(minPrice, maxPrice);
                return cb.between(root.get("price"), lowerBound, upperBound);
            }

            if (minPrice != null) {
                return cb.greaterThanOrEqualTo(root.get("price"), minPrice);
            }

            return cb.lessThanOrEqualTo(root.get("price"), maxPrice);
        };
    }

    public static Specification<Product> hasCategory(Long categoryId) {
        return (root, query, cb) -> {
            if (categoryId == null) {
                return null;
            }
            return cb.equal(root.get("category").get("id"), categoryId);
        };
    }
}
