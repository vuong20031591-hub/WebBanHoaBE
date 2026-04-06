package com.florastore.web_ban_hoa;

import com.florastore.web_ban_hoa.dto.AdminProductUpsertRequest;
import com.florastore.web_ban_hoa.dto.ProductDetailResponse;
import com.florastore.web_ban_hoa.entity.Category;
import com.florastore.web_ban_hoa.repository.CategoryRepository;
import com.florastore.web_ban_hoa.repository.ProductRepository;
import com.florastore.web_ban_hoa.service.AdminProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
class AdminProductServiceIntegrationTest {

    @Autowired
    private AdminProductService adminProductService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void softDeleteAndRestore_shouldWorkWithDefaultFilter() {
        Category category = categoryRepository.findAll().stream().findFirst().orElseThrow();

        ProductDetailResponse created = adminProductService.createProduct(new AdminProductUpsertRequest(
                "Test Admin Product",
                new BigDecimal("123000"),
                "Soft delete test",
                "admin-test.jpg",
                10,
                category.getId()
        ));

        assertThat(productRepository.findById(created.id())).isPresent();

        adminProductService.softDeleteProduct(created.id());
        assertThat(productRepository.findById(created.id())).isEmpty();
        assertThat(productRepository.findAnyById(created.id())).isPresent();

        adminProductService.restoreProduct(created.id());
        assertThat(productRepository.findById(created.id())).isPresent();
    }
}
