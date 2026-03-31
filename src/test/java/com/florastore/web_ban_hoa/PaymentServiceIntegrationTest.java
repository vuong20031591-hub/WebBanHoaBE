package com.florastore.web_ban_hoa;

import com.florastore.web_ban_hoa.dto.CreateOrderRequest;
import com.florastore.web_ban_hoa.dto.OrderResponse;
import com.florastore.web_ban_hoa.dto.PaymentWebhookRequest;
import com.florastore.web_ban_hoa.dto.PaymentWebhookResult;
import com.florastore.web_ban_hoa.entity.PaymentMethod;
import com.florastore.web_ban_hoa.service.OrderService;
import com.florastore.web_ban_hoa.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "payments.vietqr.signing-secret=test-sign",
        "payments.vietqr.webhook-secret=test-secret"
})
class PaymentServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Test
    void codConfirm_shouldUpdatePendingCodOrder() {
        OrderResponse order = orderService.createOrder(new CreateOrderRequest("cod-user", new BigDecimal("200000"), PaymentMethod.COD));
        OrderResponse confirmed = orderService.confirmCodOrder(order.id());
        assertThat(confirmed.status()).isEqualTo("CONFIRMED");
        assertThat(confirmed.confirmedAt()).isNotNull();
    }

    @Test
    void vietQrWebhook_shouldBeIdempotent() {
        OrderResponse order = orderService.createOrder(new CreateOrderRequest("vietqr-user", new BigDecimal("350000"), PaymentMethod.VIETQR));

        PaymentWebhookRequest webhook = new PaymentWebhookRequest(
                "tx-vietqr-001",
                order.id(),
                new BigDecimal("350000"),
                null,
                "test-secret"
        );

        String signature = sign("test-sign", "tx-vietqr-001|" + order.id() + "|350000");

        PaymentWebhookResult first = paymentService.handleVietQrWebhook(webhook, signature);
        PaymentWebhookResult second = paymentService.handleVietQrWebhook(webhook, signature);

        assertThat(first.status()).isEqualTo("OK");
        assertThat(second.status()).isEqualTo("IGNORED");
        assertThat(paymentService.reconcileOrderPayments(order.id()).transactionCount()).isEqualTo(1);
        assertThat(paymentService.reconcileOrderPayments(order.id()).orderStatus()).isEqualTo("CONFIRMED");
    }

    private String sign(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
