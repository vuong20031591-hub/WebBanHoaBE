package com.florastore.web_ban_hoa.controller;

import com.florastore.web_ban_hoa.dto.AdminCollaboratorResponse;
import com.florastore.web_ban_hoa.dto.AdminInviteCollaboratorRequest;
import com.florastore.web_ban_hoa.dto.AdminUpdateCollaboratorRequest;
import com.florastore.web_ban_hoa.dto.UserResponse;
import com.florastore.web_ban_hoa.security.AdminJwtGuard;
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
    private final AdminJwtGuard adminJwtGuard;

    public AdminCollaboratorController(
            AdminCollaboratorService adminCollaboratorService,
            AdminJwtGuard adminJwtGuard
    ) {
        this.adminCollaboratorService = adminCollaboratorService;
        this.adminJwtGuard = adminJwtGuard;
    }

    @GetMapping
    public ResponseEntity<List<AdminCollaboratorResponse>> getCollaborators(
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        authorizeAdmin(authorization);
        return ResponseEntity.ok(adminCollaboratorService.getCollaborators());
    }

    @GetMapping("/candidates")
    public ResponseEntity<List<UserResponse>> getInviteCandidates(
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        authorizeAdmin(authorization);
        return ResponseEntity.ok(adminCollaboratorService.getInviteCandidates());
    }

    @PostMapping
    public ResponseEntity<AdminCollaboratorResponse> inviteCollaborator(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @Valid @RequestBody AdminInviteCollaboratorRequest request
    ) {
        long actorUserId = authorizeAdmin(authorization);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                adminCollaboratorService.inviteCollaborator(actorUserId, request)
        );
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AdminCollaboratorResponse> updateCollaborator(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateCollaboratorRequest request
    ) {
        long actorUserId = authorizeAdmin(authorization);
        return ResponseEntity.ok(adminCollaboratorService.updateCollaborator(actorUserId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeCollaborator(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable Long id
    ) {
        long actorUserId = authorizeAdmin(authorization);
        adminCollaboratorService.removeCollaborator(actorUserId, id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    private long authorizeAdmin(String authorization) {
        return adminJwtGuard.assertAdminAndGetUserId(authorization);
    }
}
