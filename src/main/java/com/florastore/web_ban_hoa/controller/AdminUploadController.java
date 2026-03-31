package com.florastore.web_ban_hoa.controller;

import com.florastore.web_ban_hoa.dto.SignedUrlResponse;
import com.florastore.web_ban_hoa.dto.UploadMediaResponse;
import com.florastore.web_ban_hoa.security.AdminRoleGuard;
import com.florastore.web_ban_hoa.service.MediaStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/upload")
public class AdminUploadController {

    private final MediaStorageService mediaStorageService;
    private final AdminRoleGuard adminRoleGuard;

    public AdminUploadController(MediaStorageService mediaStorageService, AdminRoleGuard adminRoleGuard) {
        this.mediaStorageService = mediaStorageService;
        this.adminRoleGuard = adminRoleGuard;
    }

    @PostMapping
    public ResponseEntity<UploadMediaResponse> upload(
            @RequestHeader(name = "X-Role", required = false) String role,
            @RequestParam("file") MultipartFile file
    ) {
        adminRoleGuard.assertAdmin(role);
        return ResponseEntity.ok(mediaStorageService.upload(file));
    }

    @DeleteMapping("/{key:.+}")
    public ResponseEntity<Void> delete(
            @RequestHeader(name = "X-Role", required = false) String role,
            @PathVariable String key
    ) {
        adminRoleGuard.assertAdmin(role);
        mediaStorageService.delete(key);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{key:.+}/signed-url")
    public ResponseEntity<SignedUrlResponse> signedUrl(
            @RequestHeader(name = "X-Role", required = false) String role,
            @PathVariable String key,
            @RequestParam(defaultValue = "600") long expiresInSeconds
    ) {
        adminRoleGuard.assertAdmin(role);
        return ResponseEntity.ok(mediaStorageService.signedUrl(key, expiresInSeconds));
    }
}
