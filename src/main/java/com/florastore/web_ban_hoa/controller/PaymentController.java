package com.florastore.web_ban_hoa.controller;

import com.florastore.web_ban_hoa.dto.PaymentCheckoutResponse;
import com.florastore.web_ban_hoa.dto.PaymentReconciliationResponse;
import com.florastore.web_ban_hoa.dto.PaymentWebhookRequest;
import com.florastore.web_ban_hoa.dto.PaymentWebhookResult;
import com.florastore.web_ban_hoa.security.JwtSubjectResolver;
import com.florastore.web_ban_hoa.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final JwtSubjectResolver jwtSubjectResolver;

    public PaymentController(PaymentService paymentService, JwtSubjectResolver jwtSubjectResolver) {
        this.paymentService = paymentService;
        this.jwtSubjectResolver = jwtSubjectResolver;
    }

    @PostMapping("/vietqr/orders/{orderId}/checkout")
    public ResponseEntity<PaymentCheckoutResponse> createVietQrCheckout(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable Long orderId
    ) {
        String userId = jwtSubjectResolver.resolveUserId(authorization);
        return ResponseEntity.ok(paymentService.generateVietQrCheckout(userId, orderId));
    }

    @PostMapping("/sepay/orders/{orderId}/checkout")
    public ResponseEntity<PaymentCheckoutResponse> createSePayCheckout(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable Long orderId
    ) {
        String userId = jwtSubjectResolver.resolveUserId(authorization);
        return ResponseEntity.ok(paymentService.generateSePayCheckout(userId, orderId));
    }

    @PostMapping("/vietqr/webhook")
    public ResponseEntity<PaymentWebhookResult> handleVietQrWebhook(
            @Valid @RequestBody PaymentWebhookRequest request,
            @RequestHeader(name = "X-Signature", required = false) String signature
    ) {
        return ResponseEntity.ok(paymentService.handleVietQrWebhook(request, signature));
    }

    @PostMapping("/sepay/webhook")
    public ResponseEntity<PaymentWebhookResult> handleSePayWebhook(
            @Valid @RequestBody PaymentWebhookRequest request,
            @RequestHeader(name = "X-Signature", required = false) String signature,
            @RequestHeader(name = "X-Sepay-Secret", required = false) String webhookSecret
    ) {
        return ResponseEntity.ok(paymentService.handleSePayWebhook(request, signature, webhookSecret));
    }

    @GetMapping("/orders/{orderId}/reconcile")
    public ResponseEntity<PaymentReconciliationResponse> reconcile(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable Long orderId
    ) {
        String userId = jwtSubjectResolver.resolveUserId(authorization);
        return ResponseEntity.ok(paymentService.reconcileOrderPayments(userId, orderId));
    }
}
