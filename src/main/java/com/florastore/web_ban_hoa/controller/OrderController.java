package com.florastore.web_ban_hoa.controller;

import com.florastore.web_ban_hoa.dto.CreateOrderFromCartRequest;
import com.florastore.web_ban_hoa.dto.CreateOrderRequest;
import com.florastore.web_ban_hoa.dto.OrderResponse;
import com.florastore.web_ban_hoa.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(orderService.createOrder(request));
    }

    @PostMapping("/from-cart")
    public ResponseEntity<OrderResponse> createOrderFromCart(
            Authentication authentication,
            @Valid @RequestBody CreateOrderFromCartRequest request
    ) {
        String userId = authentication.getName();
        return ResponseEntity.ok(orderService.createOrderFromCart(userId, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrder(id));
    }

    @PostMapping("/{id}/cod/confirm")
    public ResponseEntity<OrderResponse> confirmCod(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.confirmCodOrder(id));
    }
}
