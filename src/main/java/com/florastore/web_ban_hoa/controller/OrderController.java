package com.florastore.web_ban_hoa.controller;

import com.florastore.web_ban_hoa.dto.CreateOrderFromCartRequest;
import com.florastore.web_ban_hoa.dto.CreateOrderRequest;
import com.florastore.web_ban_hoa.dto.OrderResponse;
import com.florastore.web_ban_hoa.security.JwtSubjectResolver;
import com.florastore.web_ban_hoa.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final JwtSubjectResolver jwtSubjectResolver;

    public OrderController(OrderService orderService, JwtSubjectResolver jwtSubjectResolver) {
        this.orderService = orderService;
        this.jwtSubjectResolver = jwtSubjectResolver;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        String userId = jwtSubjectResolver.resolveUserId(authorization);
        return ResponseEntity.ok(orderService.createOrder(userId, request));
    }

    @PostMapping("/from-cart")
    public ResponseEntity<OrderResponse> createOrderFromCart(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @Valid @RequestBody CreateOrderFromCartRequest request
    ) {
        String userId = jwtSubjectResolver.resolveUserId(authorization);
        return ResponseEntity.ok(orderService.createOrderFromCart(userId, request));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getUserOrders(
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        String userId = jwtSubjectResolver.resolveUserId(authorization);
        return ResponseEntity.ok(orderService.getUserOrders(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable Long id
    ) {
        String userId = jwtSubjectResolver.resolveUserId(authorization);
        return ResponseEntity.ok(orderService.getOrder(userId, id));
    }

    @GetMapping("/latest")
    public ResponseEntity<OrderResponse> getLatestOrder(
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        String userId = jwtSubjectResolver.resolveUserId(authorization);
        return ResponseEntity.ok(orderService.getLatestOrder(userId));
    }

    @PostMapping("/{id}/cod/confirm")
    public ResponseEntity<OrderResponse> confirmCod(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable Long id
    ) {
        String userId = jwtSubjectResolver.resolveUserId(authorization);
        return ResponseEntity.ok(orderService.confirmCodOrder(userId, id));
    }
}
