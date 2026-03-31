package com.florastore.web_ban_hoa;

import com.florastore.web_ban_hoa.dto.CartResponse;
import com.florastore.web_ban_hoa.entity.Product;
import com.florastore.web_ban_hoa.repository.ProductRepository;
import com.florastore.web_ban_hoa.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class CartControllerIntegrationTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductRepository productRepository;

    private Long productId;
    private String userId;

    @BeforeEach
    void setup() {
        Product product = productRepository.findAll().stream().findFirst().orElseThrow();
        productId = product.getId();
        userId = "user-cart-test-" + UUID.randomUUID();
    }

    @Test
    void cartCrudFlow_shouldSucceed() {
        CartResponse addResponse = cartService.addItem(userId, productId, 2);
        assertThat(addResponse.totalItems()).isEqualTo(2);
        assertThat(addResponse.items()).hasSize(1);
        Long itemId = addResponse.items().getFirst().id();

        CartResponse getResponse = cartService.getCart(userId);
        assertThat(getResponse.totalItems()).isEqualTo(2);

        CartResponse updateResponse = cartService.updateItemQuantity(userId, itemId, 3);
        assertThat(updateResponse.totalItems()).isEqualTo(3);

        cartService.deleteItem(userId, itemId);
        CartResponse emptyResponse = cartService.getCart(userId);
        assertThat(emptyResponse.totalItems()).isEqualTo(0);
        assertThat(emptyResponse.items()).isEmpty();
    }

    @Test
    void addItem_withQuantityExceedingStock_shouldReturnBadRequest() {
        assertThatThrownBy(() -> cartService.addItem(userId, productId, 99999))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");
    }
}
