package com.florastore.web_ban_hoa.controller;

import com.florastore.web_ban_hoa.dto.PaymentCheckoutResponse;
import com.florastore.web_ban_hoa.dto.PaymentReconciliationResponse;
import com.florastore.web_ban_hoa.entity.Order;
import com.florastore.web_ban_hoa.entity.PaymentMethod;
import com.florastore.web_ban_hoa.repository.OrderRepository;
import com.florastore.web_ban_hoa.security.AdminRoleGuard;
import com.florastore.web_ban_hoa.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/payments")
public class AdminPaymentController {

    private final PaymentService paymentService;
    private final OrderRepository orderRepository;
    private final AdminRoleGuard adminRoleGuard;

    public AdminPaymentController(
            PaymentService paymentService,
            OrderRepository orderRepository,
            AdminRoleGuard adminRoleGuard
    ) {
        this.paymentService = paymentService;
        this.orderRepository = orderRepository;
        this.adminRoleGuard = adminRoleGuard;
    }

    @GetMapping("/orders/{orderId}/reconcile")
    public ResponseEntity<PaymentReconciliationResponse> reconcileOrderPayments(
            @RequestHeader(name = "X-Role", required = false) String role,
            @PathVariable Long orderId
    ) {
        adminRoleGuard.assertAdmin(role);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        return ResponseEntity.ok(paymentService.reconcileOrderPayments(order.getUserId(), orderId));
    }

    @PostMapping("/orders/{orderId}/checkout-vietqr")
    public ResponseEntity<PaymentCheckoutResponse> createVietQrCheckoutAsAdmin(
            @RequestHeader(name = "X-Role", required = false) String role,
            @PathVariable Long orderId
    ) {
        adminRoleGuard.assertAdmin(role);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (order.getPaymentMethod() != PaymentMethod.VIETQR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order payment method is not VIETQR");
        }

        return ResponseEntity.ok(paymentService.generateVietQrCheckout(order.getUserId(), orderId));
    }

    @PostMapping("/sync-pending")
    public ResponseEntity<Map<String, Integer>> syncPendingPayments(
            @RequestHeader(name = "X-Role", required = false) String role
    ) {
        adminRoleGuard.assertAdmin(role);

        int confirmedCount = paymentService.syncPendingPayments();
        return ResponseEntity.ok(Map.of("confirmedCount", confirmedCount));
    }
}
