package com.florastore.web_ban_hoa.controller;

import com.florastore.web_ban_hoa.dto.LoginRequest;
import com.florastore.web_ban_hoa.dto.LoginResponse;
import com.florastore.web_ban_hoa.dto.RegisterRequest;
import com.florastore.web_ban_hoa.dto.UserResponse;
import com.florastore.web_ban_hoa.security.JwtSubjectResolver;
import com.florastore.web_ban_hoa.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtSubjectResolver jwtSubjectResolver;

    public AuthController(AuthService authService, JwtSubjectResolver jwtSubjectResolver) {
        this.authService = authService;
        this.jwtSubjectResolver = jwtSubjectResolver;
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

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        String userId = jwtSubjectResolver.resolveUserId(authorization);
        UserResponse user = authService.getUserById(userId);
        return ResponseEntity.ok(user);
    }
}
