package com.florastore.web_ban_hoa.controller;

import com.florastore.web_ban_hoa.dto.AdminProductStockUpdateRequest;
import com.florastore.web_ban_hoa.dto.AdminProductUpsertRequest;
import com.florastore.web_ban_hoa.dto.ProductDetailResponse;
import com.florastore.web_ban_hoa.security.AdminRoleGuard;
import com.florastore.web_ban_hoa.service.AdminProductService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final AdminProductService adminProductService;
    private final AdminRoleGuard adminRoleGuard;

    public AdminProductController(AdminProductService adminProductService, AdminRoleGuard adminRoleGuard) {
        this.adminProductService = adminProductService;
        this.adminRoleGuard = adminRoleGuard;
    }

    @PostMapping
    public ResponseEntity<ProductDetailResponse> createProduct(
            @RequestHeader(name = "X-Role", required = false) String role,
            @Valid @RequestBody AdminProductUpsertRequest request
    ) {
        adminRoleGuard.assertAdmin(role);
        return ResponseEntity.ok(adminProductService.createProduct(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDetailResponse> updateProduct(
            @RequestHeader(name = "X-Role", required = false) String role,
            @PathVariable Long id,
            @Valid @RequestBody AdminProductUpsertRequest request
    ) {
        adminRoleGuard.assertAdmin(role);
        return ResponseEntity.ok(adminProductService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @RequestHeader(name = "X-Role", required = false) String role,
            @PathVariable Long id
    ) {
        adminRoleGuard.assertAdmin(role);
        adminProductService.softDeleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/restore")
    public ResponseEntity<ProductDetailResponse> restoreProduct(
            @RequestHeader(name = "X-Role", required = false) String role,
            @PathVariable Long id
    ) {
        adminRoleGuard.assertAdmin(role);
        return ResponseEntity.ok(adminProductService.restoreProduct(id));
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<ProductDetailResponse> updateStock(
            @RequestHeader(name = "X-Role", required = false) String role,
            @PathVariable Long id,
            @Valid @RequestBody AdminProductStockUpdateRequest request
    ) {
        adminRoleGuard.assertAdmin(role);
        return ResponseEntity.ok(adminProductService.updateStock(id, request.stockQuantity()));
    }
}
