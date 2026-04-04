package com.florastore.web_ban_hoa.controller;

import com.florastore.web_ban_hoa.dto.NotificationPreferencesResponse;
import com.florastore.web_ban_hoa.dto.UpdateNotificationPreferencesRequest;
import com.florastore.web_ban_hoa.security.JwtSubjectResolver;
import com.florastore.web_ban_hoa.service.NotificationPreferencesService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notification-preferences")
public class NotificationPreferencesController {

    private final NotificationPreferencesService service;
    private final JwtSubjectResolver jwtSubjectResolver;

    public NotificationPreferencesController(
            NotificationPreferencesService service,
            JwtSubjectResolver jwtSubjectResolver
    ) {
        this.service = service;
        this.jwtSubjectResolver = jwtSubjectResolver;
    }

    @GetMapping
    public ResponseEntity<NotificationPreferencesResponse> getPreferences(
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        String userId = jwtSubjectResolver.resolveUserId(authorization);
        return ResponseEntity.ok(service.getPreferences(userId));
    }

    @PutMapping
    public ResponseEntity<NotificationPreferencesResponse> updatePreferences(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @Valid @RequestBody UpdateNotificationPreferencesRequest request
    ) {
        String userId = jwtSubjectResolver.resolveUserId(authorization);
        return ResponseEntity.ok(service.updatePreferences(userId, request));
    }
}
