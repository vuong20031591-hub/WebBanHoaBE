package com.florastore.web_ban_hoa.service;

import com.florastore.web_ban_hoa.dto.ProductDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    Page<ProductDTO> search(String name, Long minPrice, Long maxPrice, Long categoryId, Pageable pageable);
}
