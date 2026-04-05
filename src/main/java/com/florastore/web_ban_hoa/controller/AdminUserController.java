package com.florastore.web_ban_hoa.controller;

import com.florastore.web_ban_hoa.dto.AdminUpdateUserRoleRequest;
import com.florastore.web_ban_hoa.dto.UserResponse;
import com.florastore.web_ban_hoa.entity.Role;
import com.florastore.web_ban_hoa.security.AdminRoleGuard;
import com.florastore.web_ban_hoa.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final AdminRoleGuard adminRoleGuard;

    public AdminUserController(AdminUserService adminUserService, AdminRoleGuard adminRoleGuard) {
        this.adminUserService = adminUserService;
        this.adminRoleGuard = adminRoleGuard;
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getUsers(
            @RequestHeader(name = "X-Role", required = false) String roleHeader,
            @RequestParam(required = false) Role role
    ) {
        adminRoleGuard.assertAdmin(roleHeader);
        return ResponseEntity.ok(adminUserService.getUsers(role));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponse> updateUserRole(
            @RequestHeader(name = "X-Role", required = false) String roleHeader,
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateUserRoleRequest request
    ) {
        adminRoleGuard.assertAdmin(roleHeader);
        return ResponseEntity.ok(adminUserService.updateUserRole(id, request.role()));
    }
}
