package com.florastore.web_ban_hoa.service.impl;

import com.florastore.web_ban_hoa.entity.Category;
import com.florastore.web_ban_hoa.entity.Product;
import com.florastore.web_ban_hoa.dto.ProductDTO;
import com.florastore.web_ban_hoa.dto.ProductDetailResponse;
import com.florastore.web_ban_hoa.repository.ProductRepository;
import com.florastore.web_ban_hoa.service.MediaUrlResolver;
import com.florastore.web_ban_hoa.service.ProductService;
import com.florastore.web_ban_hoa.specification.ProductSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final MediaUrlResolver mediaUrlResolver;

    public ProductServiceImpl(ProductRepository productRepository, MediaUrlResolver mediaUrlResolver) {
        this.productRepository = productRepository;
        this.mediaUrlResolver = mediaUrlResolver;
    }

    @Override
    public Page<ProductDTO> search(String name, Long minPrice, Long maxPrice, Long categoryId, Pageable pageable) {
        Specification<Product> specification = Specification
                .where(ProductSpecification.hasName(name))
                .and(ProductSpecification.hasPriceBetween(minPrice, maxPrice))
                .and(ProductSpecification.hasCategory(categoryId));

        return productRepository.findAll(specification, pageable).map(this::toDto);
    }

    @Override
    public ProductDetailResponse getProductDetail(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        return toDetailResponse(product);
    }

    private ProductDTO toDto(Product product) {
        Category category = product.getCategory();
        String categoryName = category != null ? category.getName() : null;

        return new ProductDTO(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getDescription(),
                mediaUrlResolver.resolveProductImage(product.getImage()),
                product.getStockQuantity(),
                categoryName
        );
    }

    private ProductDetailResponse toDetailResponse(Product product) {
        return new ProductDetailResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getDescription(),
                mediaUrlResolver.resolveProductImage(product.getImage()),
                product.getStockQuantity(),
                product.getCreatedAt(),
                product.getUpdatedAt(),
                product.getDeletedAt(),
                product.getCategory().getId(),
                product.getCategory().getName()
        );
    }
}
