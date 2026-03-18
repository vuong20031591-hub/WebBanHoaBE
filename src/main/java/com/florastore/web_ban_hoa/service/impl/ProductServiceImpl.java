package com.florastore.web_ban_hoa.service.impl;

import com.florastore.web_ban_hoa.domain.Category;
import com.florastore.web_ban_hoa.domain.Product;
import com.florastore.web_ban_hoa.dto.ProductDTO;
import com.florastore.web_ban_hoa.repository.ProductRepository;
import com.florastore.web_ban_hoa.service.ProductService;
import com.florastore.web_ban_hoa.specification.ProductSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Page<ProductDTO> search(String name, Long minPrice, Long maxPrice, Long categoryId, Pageable pageable) {
        Specification<Product> specification = Specification
                .where(ProductSpecification.hasName(name))
                .and(ProductSpecification.hasPriceBetween(minPrice, maxPrice))
                .and(ProductSpecification.hasCategory(categoryId));

        return productRepository.findAll(specification, pageable).map(this::toDto);
    }

    private ProductDTO toDto(Product product) {
        Category category = product.getCategory();
        String categoryName = category != null ? category.getName() : null;

        return new ProductDTO(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getDescription(),
                product.getImageUrl(),
                categoryName
        );
    }
}
