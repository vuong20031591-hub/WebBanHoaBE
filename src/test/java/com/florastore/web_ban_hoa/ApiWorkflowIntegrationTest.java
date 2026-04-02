package com.florastore.web_ban_hoa;

import com.florastore.web_ban_hoa.entity.Product;
import com.florastore.web_ban_hoa.repository.ProductRepository;
import com.florastore.web_ban_hoa.service.CartService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ApiWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TestJwtHelper jwtHelper;

    @Test
    void cartWithoutAuthorization_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createOrder_shouldUseAuthenticatedUserCartTotal() throws Exception {
        String userId = "1";
        Product product = productRepository.findAll().stream().findFirst().orElseThrow();
        cartService.addItem(userId, product.getId(), 2);

        BigDecimal expectedTotal = product.getPrice().multiply(BigDecimal.valueOf(2));

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", jwtHelper.bearer(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"VIETQR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.paymentMethod").value("VIETQR"))
                .andExpect(jsonPath("$.totalAmount").value(expectedTotal.doubleValue()));

        mockMvc.perform(get("/api/cart")
                        .header("Authorization", jwtHelper.bearer(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.totalAmount").value(0));
    }

    @Test
    void checkoutWithoutAuthorization_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/payments/vietqr/orders/999/checkout"))
                .andExpect(status().isUnauthorized());
    }
}
