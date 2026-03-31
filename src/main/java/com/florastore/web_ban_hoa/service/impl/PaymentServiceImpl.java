package com.florastore.web_ban_hoa.service.impl;

import com.florastore.web_ban_hoa.dto.PaymentCheckoutResponse;
import com.florastore.web_ban_hoa.dto.PaymentReconciliationResponse;
import com.florastore.web_ban_hoa.dto.PaymentWebhookRequest;
import com.florastore.web_ban_hoa.dto.PaymentWebhookResult;
import com.florastore.web_ban_hoa.entity.Order;
import com.florastore.web_ban_hoa.entity.OrderStatus;
import com.florastore.web_ban_hoa.entity.PaymentMethod;
import com.florastore.web_ban_hoa.entity.PaymentTransaction;
import com.florastore.web_ban_hoa.entity.PaymentTransactionStatus;
import com.florastore.web_ban_hoa.repository.OrderRepository;
import com.florastore.web_ban_hoa.repository.PaymentTransactionRepository;
import com.florastore.web_ban_hoa.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;

    private final String vietqrApiBaseUrl;
    private final String vietqrWebhookSecret;
    private final String vietqrSigningSecret;
    private final String sepayApiBaseUrl;
    private final String sepayWebhookSecret;
    private final String sepaySigningSecret;

    public PaymentServiceImpl(
            OrderRepository orderRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            @Value("${payments.vietqr.api-base-url:https://api.vietqr.io}") String vietqrApiBaseUrl,
            @Value("${payments.vietqr.webhook-secret:}") String vietqrWebhookSecret,
            @Value("${payments.vietqr.signing-secret:}") String vietqrSigningSecret,
            @Value("${payments.sepay.api-base-url:https://my.sepay.vn}") String sepayApiBaseUrl,
            @Value("${payments.sepay.webhook-secret:}") String sepayWebhookSecret,
            @Value("${payments.sepay.signing-secret:}") String sepaySigningSecret
    ) {
        this.orderRepository = orderRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.vietqrApiBaseUrl = vietqrApiBaseUrl;
        this.vietqrWebhookSecret = vietqrWebhookSecret;
        this.vietqrSigningSecret = vietqrSigningSecret;
        this.sepayApiBaseUrl = sepayApiBaseUrl;
        this.sepayWebhookSecret = sepayWebhookSecret;
        this.sepaySigningSecret = sepaySigningSecret;
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentCheckoutResponse generateVietQrCheckout(Long orderId) {
        Order order = getOrderOrThrow(orderId);
        String qrContent = "VIETQR|ORDER=" + order.getId() + "|AMOUNT=" + order.getTotalAmount();
        String checkoutUrl = vietqrApiBaseUrl + "/checkout?orderId=" + order.getId();
        return new PaymentCheckoutResponse(order.getId(), "VIETQR", checkoutUrl, qrContent, "Use qrContent to render QR image");
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentCheckoutResponse generateSePayCheckout(Long orderId) {
        Order order = getOrderOrThrow(orderId);
        String checkoutUrl = sepayApiBaseUrl + "/checkout?orderId=" + order.getId() + "&amount=" + order.getTotalAmount();
        return new PaymentCheckoutResponse(order.getId(), "SEPAY", checkoutUrl, null, "Redirect user to checkoutUrl");
    }

    @Override
    public PaymentWebhookResult handleVietQrWebhook(PaymentWebhookRequest request, String headerSignature) {
        log.info("[VIETQR WEBHOOK] payload={}", request);
        validateSignature(request, headerSignature, vietqrSigningSecret);
        validateHeaderSecret(request.secret(), vietqrWebhookSecret);
        return processWebhook(PaymentMethod.VIETQR, request);
    }

    @Override
    public PaymentWebhookResult handleSePayWebhook(PaymentWebhookRequest request, String headerSignature, String headerSecret) {
        log.info("[SEPAY WEBHOOK] payload={}", request);
        validateSignature(request, headerSignature, sepaySigningSecret);
        if (headerSecret != null && !headerSecret.isBlank()) {
            validateHeaderSecret(headerSecret, sepayWebhookSecret);
        } else {
            validateHeaderSecret(request.secret(), sepayWebhookSecret);
        }
        return processWebhook(PaymentMethod.SEPAY, request);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentReconciliationResponse reconcileOrderPayments(Long orderId) {
        Order order = getOrderOrThrow(orderId);
        List<PaymentTransaction> transactions = paymentTransactionRepository.findByOrderId(orderId);
        List<String> txSummary = transactions.stream()
                .map(tx -> tx.getPaymentMethod().name() + ":" + tx.getProviderTransactionId() + ":" + tx.getStatus().name())
                .toList();

        boolean paid = transactions.stream().anyMatch(tx -> tx.getStatus() == PaymentTransactionStatus.SUCCESS);
        return new PaymentReconciliationResponse(order.getId(), order.getStatus().name(), transactions.size(), paid, txSummary);
    }

    private PaymentWebhookResult processWebhook(PaymentMethod method, PaymentWebhookRequest request) {
        Order order = getOrderOrThrow(request.orderId());

        if (order.getTotalAmount().compareTo(request.amount()) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Webhook amount does not match order amount");
        }

        if (paymentTransactionRepository.findByPaymentMethodAndProviderTransactionId(method, request.providerTransactionId()).isPresent()) {
            return new PaymentWebhookResult("IGNORED", "Transaction already processed", order.getId(), request.providerTransactionId());
        }

        PaymentTransaction transaction = new PaymentTransaction(
                order,
                method,
                request.providerTransactionId(),
                request.amount(),
                PaymentTransactionStatus.SUCCESS
        );
        paymentTransactionRepository.save(transaction);

        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CONFIRMED);
            order.setConfirmedAt(LocalDateTime.now());
            orderRepository.save(order);
        }

        return new PaymentWebhookResult("OK", "Webhook processed", order.getId(), request.providerTransactionId());
    }

    private void validateHeaderSecret(String provided, String configured) {
        if (configured == null || configured.isBlank()) {
            return;
        }
        if (provided == null || !configured.equals(provided)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook secret");
        }
    }

    private void validateSignature(PaymentWebhookRequest request, String headerSignature, String signingSecret) {
        if (signingSecret == null || signingSecret.isBlank()) {
            return;
        }
        String providedSignature = headerSignature;
        if ((providedSignature == null || providedSignature.isBlank()) && request.signature() != null) {
            providedSignature = request.signature();
        }
        if (providedSignature == null || providedSignature.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing signature");
        }

        String payload = request.providerTransactionId() + "|" + request.orderId() + "|" + request.amount();
        String expected = hmacSha256Hex(signingSecret, payload);
        if (!expected.equalsIgnoreCase(providedSignature.trim())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid signature");
        }
    }

    private String hmacSha256Hex(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot verify signature", ex);
        }
    }

    private Order getOrderOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }
}
