package com.florastore.web_ban_hoa.controller;

import com.florastore.web_ban_hoa.dto.AdminUpdateUserRoleRequest;
import com.florastore.web_ban_hoa.dto.PagedResponse;
import com.florastore.web_ban_hoa.dto.UserResponse;
import com.florastore.web_ban_hoa.entity.Role;
import com.florastore.web_ban_hoa.security.AdminJwtGuard;
import com.florastore.web_ban_hoa.service.AdminUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@Validated
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final AdminJwtGuard adminJwtGuard;

    public AdminUserController(
            AdminUserService adminUserService,
            AdminJwtGuard adminJwtGuard
    ) {
        this.adminUserService = adminUserService;
        this.adminJwtGuard = adminJwtGuard;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<UserResponse>> getUsers(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestParam(required = false) Role role,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        adminJwtGuard.assertAdminAndGetUserId(authorization);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "fullName"));
        return ResponseEntity.ok(adminUserService.getUsers(role, pageable));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponse> updateUserRole(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateUserRoleRequest request
    ) {
        long actorUserId = adminJwtGuard.assertAdminAndGetUserId(authorization);
        return ResponseEntity.ok(adminUserService.updateUserRole(actorUserId, id, request.role()));
    }
}
