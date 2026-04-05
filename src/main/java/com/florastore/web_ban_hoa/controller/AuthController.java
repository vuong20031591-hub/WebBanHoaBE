package com.florastore.web_ban_hoa.controller;

import com.florastore.web_ban_hoa.dto.LoginRequest;
import com.florastore.web_ban_hoa.dto.LoginResponse;
import com.florastore.web_ban_hoa.dto.RegisterRequest;
import com.florastore.web_ban_hoa.dto.SupabaseUserProfile;
import com.florastore.web_ban_hoa.dto.ChangePasswordRequest;
import com.florastore.web_ban_hoa.dto.ForgotPasswordRequest;
import com.florastore.web_ban_hoa.dto.ForgotPasswordResponse;
import com.florastore.web_ban_hoa.dto.ResetPasswordWithCodeRequest;
import com.florastore.web_ban_hoa.dto.UpdateProfileRequest;
import com.florastore.web_ban_hoa.dto.UserResponse;
import com.florastore.web_ban_hoa.security.JwtSubjectResolver;
import com.florastore.web_ban_hoa.service.AuthService;
import com.florastore.web_ban_hoa.service.PasswordResetService;
import com.florastore.web_ban_hoa.service.SupabaseAuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtSubjectResolver jwtSubjectResolver;
    private final SupabaseAuthService supabaseAuthService;
    private final PasswordResetService passwordResetService;

    public AuthController(
            AuthService authService,
            JwtSubjectResolver jwtSubjectResolver,
            SupabaseAuthService supabaseAuthService,
            PasswordResetService passwordResetService
    ) {
        this.authService = authService;
        this.jwtSubjectResolver = jwtSubjectResolver;
        this.supabaseAuthService = supabaseAuthService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password/request")
    public ResponseEntity<ForgotPasswordResponse> requestForgotPasswordCode(@Valid @RequestBody ForgotPasswordRequest request) {
        ForgotPasswordResponse response = passwordResetService.requestResetCode(request.email());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password/confirm")
    public ResponseEntity<Void> confirmForgotPassword(
            @Valid @RequestBody ResetPasswordWithCodeRequest request
    ) {
        passwordResetService.resetPasswordWithCode(
                request.email(),
                request.code(),
                request.newPassword()
        );
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/oauth/google")
    public ResponseEntity<LoginResponse> loginWithGoogleOAuth(
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Supabase Bearer token");
        }

        String supabaseAccessToken = authorization.substring(7).trim();
        SupabaseUserProfile profile = supabaseAuthService.verifyAndGetProfile(supabaseAccessToken);
        LoginResponse response = authService.loginWithSupabaseProfile(profile);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        String userId = jwtSubjectResolver.resolveUserId(authorization);
        UserResponse user = authService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        String userId = jwtSubjectResolver.resolveUserId(authorization);
        UserResponse user = authService.updateProfile(userId, request);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        String userId = jwtSubjectResolver.resolveUserId(authorization);
        authService.changePassword(userId, request);
        return ResponseEntity.noContent().build();
    }
}
