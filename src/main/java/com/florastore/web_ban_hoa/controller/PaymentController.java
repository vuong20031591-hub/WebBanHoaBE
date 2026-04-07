package com.florastore.web_ban_hoa.controller;

import com.florastore.web_ban_hoa.dto.PaymentCheckoutResponse;
import com.florastore.web_ban_hoa.dto.PaymentReconciliationResponse;
import com.florastore.web_ban_hoa.dto.PaymentWebhookRequest;
import com.florastore.web_ban_hoa.dto.PaymentWebhookResult;
import com.florastore.web_ban_hoa.dto.SePayWebhookRequest;
import com.florastore.web_ban_hoa.entity.Order;
import com.florastore.web_ban_hoa.entity.OrderStatus;
import com.florastore.web_ban_hoa.repository.OrderRepository;
import com.florastore.web_ban_hoa.security.JwtSubjectResolver;
import com.florastore.web_ban_hoa.service.PaymentService;
import com.florastore.web_ban_hoa.service.PaymentStatusNotifier;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final JwtSubjectResolver jwtSubjectResolver;
    private final OrderRepository orderRepository;
    private final PaymentStatusNotifier paymentStatusNotifier;

    public PaymentController(
            PaymentService paymentService,
            JwtSubjectResolver jwtSubjectResolver,
            OrderRepository orderRepository,
            PaymentStatusNotifier paymentStatusNotifier
    ) {
        this.paymentService = paymentService;
        this.jwtSubjectResolver = jwtSubjectResolver;
        this.orderRepository = orderRepository;
        this.paymentStatusNotifier = paymentStatusNotifier;
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
            @Valid @RequestBody SePayWebhookRequest request,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @RequestHeader(name = "X-Sepay-Secret", required = false) String webhookSecret,
            @RequestHeader(name = "X-Forwarded-For", required = false) String forwardedFor,
            @RequestHeader(name = "CF-Connecting-IP", required = false) String cfConnectingIp,
            @RequestHeader(name = "X-Real-IP", required = false) String realIp,
            HttpServletRequest servletRequest
    ) {
        return ResponseEntity.ok(
                paymentService.handleSePayWebhook(
                        request,
                        authorizationHeader,
                        webhookSecret,
                        extractClientIp(forwardedFor, cfConnectingIp, realIp, servletRequest)
                )
        );
    }

    private String extractClientIp(
            String forwardedFor,
            String cfConnectingIp,
            String realIp,
            HttpServletRequest servletRequest
    ) {
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        if (cfConnectingIp != null && !cfConnectingIp.isBlank()) {
            return cfConnectingIp.trim();
        }
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return servletRequest.getRemoteAddr();
    }

    @GetMapping("/orders/{orderId}/reconcile")
    public ResponseEntity<PaymentReconciliationResponse> reconcile(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable Long orderId
    ) {
        String userId = jwtSubjectResolver.resolveUserId(authorization);
        return ResponseEntity.ok(paymentService.reconcileOrderPayments(userId, orderId));
    }

    @GetMapping(path = "/orders/{orderId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeOrderPaymentEvents(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable Long orderId
    ) {
        String userId = jwtSubjectResolver.resolveUserId(authorization);
        Order order = orderRepository.findById(orderId)
                .filter(item -> item.getUserId().equals(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        SseEmitter emitter = paymentStatusNotifier.subscribe(orderId);
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            paymentStatusNotifier.publishPaid(orderId);
        }
        return emitter;
    }
}
