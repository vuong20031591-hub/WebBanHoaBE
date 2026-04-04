package com.florastore.web_ban_hoa.controller;

import com.florastore.web_ban_hoa.dto.PagedResponse;
import com.florastore.web_ban_hoa.dto.ProductDTO;
import com.florastore.web_ban_hoa.dto.ProductDetailResponse;
import com.florastore.web_ban_hoa.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductDetail(id));
    }

    @GetMapping("/search")
    public ResponseEntity<PagedResponse<ProductDTO>> searchProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false) Long categoryId,
            @PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<ProductDTO> result = productService.search(name, minPrice, maxPrice, categoryId, pageable);
        return ResponseEntity.ok(PagedResponse.from(result));
    }

    @GetMapping("/search/suggestions")
    public ResponseEntity<PagedResponse<ProductDTO>> getSearchSuggestions(
            @RequestParam String query,
            @PageableDefault(page = 0, size = 5, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<ProductDTO> result = productService.search(query, null, null, null, pageable);
        return ResponseEntity.ok(PagedResponse.from(result));
    }
}
