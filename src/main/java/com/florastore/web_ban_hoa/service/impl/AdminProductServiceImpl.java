package com.florastore.web_ban_hoa.service.impl;

import com.florastore.web_ban_hoa.dto.AdminProductUpsertRequest;
import com.florastore.web_ban_hoa.dto.ProductDetailResponse;
import com.florastore.web_ban_hoa.entity.Category;
import com.florastore.web_ban_hoa.entity.Product;
import com.florastore.web_ban_hoa.repository.CategoryRepository;
import com.florastore.web_ban_hoa.repository.ProductRepository;
import com.florastore.web_ban_hoa.service.AdminProductService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class AdminProductServiceImpl implements AdminProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public AdminProductServiceImpl(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public ProductDetailResponse createProduct(AdminProductUpsertRequest request) {
        Category category = getCategoryOrThrow(request.categoryId());

        Product product = new Product();
        applyUpsert(product, request, category);

        return ProductDetailResponse.fromEntity(productRepository.save(product));
    }

    @Override
    public ProductDetailResponse updateProduct(Long id, AdminProductUpsertRequest request) {
        Product product = getProductAnyByIdOrThrow(id);
        Category category = getCategoryOrThrow(request.categoryId());

        applyUpsert(product, request, category);

        return ProductDetailResponse.fromEntity(productRepository.save(product));
    }

    @Override
    public void softDeleteProduct(Long id) {
        Product product = getProductAnyByIdOrThrow(id);
        productRepository.delete(product);
    }

    @Override
    public ProductDetailResponse restoreProduct(Long id) {
        Product product = getProductAnyByIdOrThrow(id);
        product.setDeletedAt(null);
        return ProductDetailResponse.fromEntity(productRepository.save(product));
    }

    @Override
    public ProductDetailResponse updateStock(Long id, Integer stockQuantity) {
        Product product = getProductAnyByIdOrThrow(id);
        product.setStockQuantity(stockQuantity);
        return ProductDetailResponse.fromEntity(productRepository.save(product));
    }

    private Product getProductAnyByIdOrThrow(Long id) {
        return productRepository.findAnyById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    private Category getCategoryOrThrow(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    }

    private void applyUpsert(Product product, AdminProductUpsertRequest request, Category category) {
        product.setName(request.name().trim());
        product.setPrice(request.price());
        product.setDescription(request.description());
        product.setImage(request.image());
        product.setStockQuantity(request.stockQuantity());
        product.setCategory(category);
    }
}
