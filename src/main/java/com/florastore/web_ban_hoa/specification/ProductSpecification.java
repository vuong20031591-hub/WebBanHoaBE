package com.florastore.web_ban_hoa.specification;

import com.florastore.web_ban_hoa.domain.Product;
import org.springframework.data.jpa.domain.Specification;
import java.math.BigDecimal;

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
            BigDecimal lowerBound = BigDecimal.valueOf(Math.min(minPrice, maxPrice));
            BigDecimal upperBound = BigDecimal.valueOf(Math.max(minPrice, maxPrice));
            return cb.between(root.get("price"), lowerBound, upperBound);
        }

        if (minPrice != null) {
            return cb.greaterThanOrEqualTo(
                    root.get("price"),
                    BigDecimal.valueOf(minPrice)
            );
        }

        return cb.lessThanOrEqualTo(
                root.get("price"),
                BigDecimal.valueOf(maxPrice)
        );
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
