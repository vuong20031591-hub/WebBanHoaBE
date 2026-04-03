package com.florastore.web_ban_hoa;

import com.florastore.web_ban_hoa.dto.CreateOrderRequest;
import com.florastore.web_ban_hoa.dto.OrderResponse;
import com.florastore.web_ban_hoa.dto.PaymentCheckoutResponse;
import com.florastore.web_ban_hoa.dto.PaymentWebhookRequest;
import com.florastore.web_ban_hoa.dto.PaymentWebhookResult;
import com.florastore.web_ban_hoa.dto.SePayWebhookRequest;
import com.florastore.web_ban_hoa.entity.PaymentMethod;
import com.florastore.web_ban_hoa.entity.Product;
import com.florastore.web_ban_hoa.repository.ProductRepository;
import com.florastore.web_ban_hoa.service.CartService;
import com.florastore.web_ban_hoa.service.OrderService;
import com.florastore.web_ban_hoa.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "payments.vietqr.signing-secret=test-sign",
        "payments.vietqr.webhook-secret=test-secret",
        "payments.sepay.webhook-secret=test-sepay-secret",
        "payments.webhook.ip-whitelist=127.0.0.1,172.236.138.20"
})
@ActiveProfiles("dev")
class PaymentServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void codConfirm_shouldUpdatePendingCodOrder() {
        String userId = "1";
        seedCart(userId, 2);

        OrderResponse order = orderService.createOrder(userId, new CreateOrderRequest(PaymentMethod.COD));
        OrderResponse confirmed = orderService.confirmCodOrder(userId, order.id());
        assertThat(confirmed.status()).isEqualTo("CONFIRMED");
        assertThat(confirmed.confirmedAt()).isNotNull();
    }

    @Test
    void vietQrWebhook_shouldBeIdempotent() {
        String userId = "2";
        seedCart(userId, 1);

        OrderResponse order = orderService.createOrder(userId, new CreateOrderRequest(PaymentMethod.VIETQR));

        PaymentWebhookRequest webhook = new PaymentWebhookRequest(
                "tx-vietqr-001",
                order.id(),
                new BigDecimal("350000"),
                paymentService.generateVietQrCheckout(userId, order.id()).transactionId(),
                null,
                "test-secret"
        );

        String signature = sign("test-sign", "tx-vietqr-001|" + order.id() + "|350000");

        PaymentWebhookResult first = paymentService.handleVietQrWebhook(webhook, signature);
        PaymentWebhookResult second = paymentService.handleVietQrWebhook(webhook, signature);

        assertThat(first.status()).isEqualTo("OK");
        assertThat(second.status()).isEqualTo("IGNORED");
        assertThat(paymentService.reconcileOrderPayments(userId, order.id()).transactionCount()).isEqualTo(1);
        assertThat(paymentService.reconcileOrderPayments(userId, order.id()).orderStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    void generateCheckout_shouldRejectWrongProviderForOrder() {
        String userId = "provider-user-" + UUID.randomUUID();
        seedCart(userId, 1);

        OrderResponse order = orderService.createOrder(userId, new CreateOrderRequest(PaymentMethod.COD));

        assertThatThrownBy(() -> paymentService.generateVietQrCheckout(userId, order.id()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Order payment method does not match checkout provider");
    }

    @Test
    void generateCheckout_shouldCreateRandomPendingTransactionId() {
        String userId = "3";
        seedCart(userId, 1);

        OrderResponse order = orderService.createOrder(userId, new CreateOrderRequest(PaymentMethod.VIETQR));
        PaymentCheckoutResponse checkout = paymentService.generateVietQrCheckout(userId, order.id());

        assertThat(checkout.transactionId()).startsWith("QRD");
        assertThat(checkout.transactionId()).hasSizeGreaterThan(10);
        assertThat(checkout.expiresInSeconds()).isPositive();
    }

    @Test
    void webhook_shouldRejectWhenPaymentCodeDoesNotMatchTransferContent() {
        String userId = "content-check-user-" + UUID.randomUUID();
        seedCart(userId, 1);

        OrderResponse order = orderService.createOrder(userId, new CreateOrderRequest(PaymentMethod.VIETQR));
        PaymentCheckoutResponse checkout = paymentService.generateVietQrCheckout(userId, order.id());

        PaymentWebhookRequest webhook = new PaymentWebhookRequest(
                "tx-vietqr-002",
                order.id(),
                new BigDecimal("350000"),
                "wrong-content",
                null,
                "test-secret"
        );

        String signature = sign("test-sign", "tx-vietqr-002|" + order.id() + "|350000");

        assertThatThrownBy(() -> paymentService.handleVietQrWebhook(webhook, signature))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Payment content does not match payment code");

        assertThat(checkout.transactionId()).startsWith("QRD");
        assertThat(paymentService.reconcileOrderPayments(userId, order.id()).paid()).isFalse();
    }

    @Test
    void sePayWebhook_shouldConfirmPendingVietQrOrderUsingRealPayload() {
        String userId = "sepay-webhook-user-" + UUID.randomUUID();
        seedCart(userId, 1);

        OrderResponse order = orderService.createOrder(userId, new CreateOrderRequest(PaymentMethod.VIETQR));
        PaymentCheckoutResponse checkout = paymentService.generateVietQrCheckout(userId, order.id());

        SePayWebhookRequest webhook = new SePayWebhookRequest(
                92704L,
                "MB Bank",
                "2026-04-03 15:05:00",
                "0327207918",
                checkout.transactionId(),
                "thanh toan " + checkout.transactionId(),
                "in",
                new BigDecimal("350000"),
                null,
                null,
                "MBVCB.3278907687",
                "Auto matched payment"
        );

        PaymentWebhookResult result = paymentService.handleSePayWebhook(
                webhook,
                "Apikey test-sepay-secret",
                null,
                "172.236.138.20"
        );

        assertThat(result.status()).isEqualTo("OK");
        assertThat(result.orderId()).isEqualTo(order.id());
        assertThat(paymentService.reconcileOrderPayments(userId, order.id()).paid()).isTrue();
        assertThat(paymentService.reconcileOrderPayments(userId, order.id()).orderStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    void sePayWebhook_shouldRejectUnexpectedIp() {
        String userId = "sepay-ip-user-" + UUID.randomUUID();
        seedCart(userId, 1);

        OrderResponse order = orderService.createOrder(userId, new CreateOrderRequest(PaymentMethod.VIETQR));
        PaymentCheckoutResponse checkout = paymentService.generateVietQrCheckout(userId, order.id());

        SePayWebhookRequest webhook = new SePayWebhookRequest(
                92705L,
                "MB Bank",
                "2026-04-03 15:10:00",
                "0327207918",
                checkout.transactionId(),
                checkout.transactionId(),
                "in",
                new BigDecimal("350000"),
                null,
                null,
                "MBVCB.3278907688",
                "Auto matched payment"
        );

        assertThatThrownBy(() -> paymentService.handleSePayWebhook(
                webhook,
                "Apikey test-sepay-secret",
                null,
                "8.8.8.8"
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Webhook IP is not allowed");
    }

    private void seedCart(String userId, int quantity) {
        Product product = productRepository.findAll().stream().findFirst().orElseThrow();
        cartService.addItem(userId, product.getId(), quantity);
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
