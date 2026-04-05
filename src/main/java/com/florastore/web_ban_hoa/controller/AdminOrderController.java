package com.florastore.web_ban_hoa.controller;

import com.florastore.web_ban_hoa.dto.AdminOrderStatsResponse;
import com.florastore.web_ban_hoa.dto.AdminCreateOrderRequest;
import com.florastore.web_ban_hoa.dto.OrderResponse;
import com.florastore.web_ban_hoa.dto.PagedResponse;
import com.florastore.web_ban_hoa.dto.UpdateOrderStatusRequest;
import com.florastore.web_ban_hoa.entity.OrderStatus;
import com.florastore.web_ban_hoa.security.AdminRoleGuard;
import com.florastore.web_ban_hoa.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;
    private final AdminRoleGuard adminRoleGuard;

    public AdminOrderController(OrderService orderService, AdminRoleGuard adminRoleGuard) {
        this.orderService = orderService;
        this.adminRoleGuard = adminRoleGuard;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader(name = "X-Role", required = false) String role,
            @Valid @RequestBody AdminCreateOrderRequest request
    ) {
        adminRoleGuard.assertAdmin(role);
        return ResponseEntity.ok(orderService.createAdminOrder(request));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<OrderResponse>> getOrders(
            @RequestHeader(name = "X-Role", required = false) String role,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "false") boolean includeUserProfile,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        adminRoleGuard.assertAdmin(role);

        Sort.Direction direction = sortDir.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        return ResponseEntity.ok(
                orderService.getOrdersWithFilters(
                        status,
                        startDate,
                        endDate,
                        userId,
                        pageable,
                        includeUserProfile
                )
        );
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @RequestHeader(name = "X-Role", required = false) String role,
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        adminRoleGuard.assertAdmin(role);
        return ResponseEntity.ok(orderService.updateOrderStatus(id, request.status()));
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminOrderStatsResponse> getOrderStats(
            @RequestHeader(name = "X-Role", required = false) String role
    ) {
        adminRoleGuard.assertAdmin(role);
        return ResponseEntity.ok(orderService.getOrderStats());
    }
}
