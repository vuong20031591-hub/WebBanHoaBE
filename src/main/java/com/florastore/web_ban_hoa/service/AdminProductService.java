package com.florastore.web_ban_hoa.service;

import com.florastore.web_ban_hoa.dto.AdminProductUpsertRequest;
import com.florastore.web_ban_hoa.dto.ProductDetailResponse;

public interface AdminProductService {
    ProductDetailResponse createProduct(AdminProductUpsertRequest request);

    ProductDetailResponse updateProduct(Long id, AdminProductUpsertRequest request);

    void softDeleteProduct(Long id);

    ProductDetailResponse restoreProduct(Long id);

    ProductDetailResponse updateStock(Long id, Integer stockQuantity);
}
