package com.florastore.web_ban_hoa.controller;

import com.florastore.web_ban_hoa.dto.AdminCollaboratorResponse;
import com.florastore.web_ban_hoa.dto.AdminInviteCollaboratorRequest;
import com.florastore.web_ban_hoa.dto.AdminUpdateCollaboratorRequest;
import com.florastore.web_ban_hoa.dto.UserResponse;
import com.florastore.web_ban_hoa.security.AdminRoleGuard;
import com.florastore.web_ban_hoa.service.AdminCollaboratorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/collaborators")
public class AdminCollaboratorController {

    private final AdminCollaboratorService adminCollaboratorService;
    private final AdminRoleGuard adminRoleGuard;

    public AdminCollaboratorController(
            AdminCollaboratorService adminCollaboratorService,
            AdminRoleGuard adminRoleGuard
    ) {
        this.adminCollaboratorService = adminCollaboratorService;
        this.adminRoleGuard = adminRoleGuard;
    }

    @GetMapping
    public ResponseEntity<List<AdminCollaboratorResponse>> getCollaborators(
            @RequestHeader(name = "X-Role", required = false) String roleHeader
    ) {
        adminRoleGuard.assertAdmin(roleHeader);
        return ResponseEntity.ok(adminCollaboratorService.getCollaborators());
    }

    @GetMapping("/candidates")
    public ResponseEntity<List<UserResponse>> getInviteCandidates(
            @RequestHeader(name = "X-Role", required = false) String roleHeader
    ) {
        adminRoleGuard.assertAdmin(roleHeader);
        return ResponseEntity.ok(adminCollaboratorService.getInviteCandidates());
    }

    @PostMapping
    public ResponseEntity<AdminCollaboratorResponse> inviteCollaborator(
            @RequestHeader(name = "X-Role", required = false) String roleHeader,
            @Valid @RequestBody AdminInviteCollaboratorRequest request
    ) {
        adminRoleGuard.assertAdmin(roleHeader);
        return ResponseEntity.ok(adminCollaboratorService.inviteCollaborator(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AdminCollaboratorResponse> updateCollaborator(
            @RequestHeader(name = "X-Role", required = false) String roleHeader,
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateCollaboratorRequest request
    ) {
        adminRoleGuard.assertAdmin(roleHeader);
        return ResponseEntity.ok(adminCollaboratorService.updateCollaborator(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeCollaborator(
            @RequestHeader(name = "X-Role", required = false) String roleHeader,
            @PathVariable Long id
    ) {
        adminRoleGuard.assertAdmin(roleHeader);
        adminCollaboratorService.removeCollaborator(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
