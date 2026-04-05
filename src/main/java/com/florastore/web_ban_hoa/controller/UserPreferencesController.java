package com.florastore.web_ban_hoa.controller;

import com.florastore.web_ban_hoa.dto.TwoFactorSmsCodeResponse;
import com.florastore.web_ban_hoa.dto.UpdateUserPreferencesRequest;
import com.florastore.web_ban_hoa.dto.UserPreferencesResponse;
import com.florastore.web_ban_hoa.dto.VerifyTwoFactorSmsCodeRequest;
import com.florastore.web_ban_hoa.security.JwtSubjectResolver;
import com.florastore.web_ban_hoa.service.UserPreferencesService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user-preferences")
public class UserPreferencesController {

    private final UserPreferencesService service;
    private final JwtSubjectResolver jwtSubjectResolver;

    public UserPreferencesController(
            UserPreferencesService service,
            JwtSubjectResolver jwtSubjectResolver
    ) {
        this.service = service;
        this.jwtSubjectResolver = jwtSubjectResolver;
    }

    @GetMapping
    public ResponseEntity<UserPreferencesResponse> getPreferences(
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        String userId = jwtSubjectResolver.resolveUserId(authorization);
        return ResponseEntity.ok(service.getPreferences(userId));
    }

    @PutMapping
    public ResponseEntity<UserPreferencesResponse> updatePreferences(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @Valid @RequestBody UpdateUserPreferencesRequest request
    ) {
        String userId = jwtSubjectResolver.resolveUserId(authorization);
        return ResponseEntity.ok(service.updatePreferences(userId, request));
    }

    @PostMapping("/two-factor/sms/request-code")
    public ResponseEntity<TwoFactorSmsCodeResponse> requestSmsTwoFactorCode(
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        String userId = jwtSubjectResolver.resolveUserId(authorization);
        return ResponseEntity.ok(service.requestSmsTwoFactorCode(userId));
    }

    @PostMapping("/two-factor/sms/verify-code")
    public ResponseEntity<UserPreferencesResponse> verifySmsTwoFactorCode(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @Valid @RequestBody VerifyTwoFactorSmsCodeRequest request
    ) {
        String userId = jwtSubjectResolver.resolveUserId(authorization);
        return ResponseEntity.ok(service.verifySmsTwoFactorCode(userId, request));
    }
}
