package com.florastore.web_ban_hoa.controller;

import com.florastore.web_ban_hoa.dto.ChangePasswordRequest;
import com.florastore.web_ban_hoa.dto.UpdateProfileRequest;
import com.florastore.web_ban_hoa.dto.UserResponse;
import com.florastore.web_ban_hoa.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(userService.getCurrentUser(email));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        String email = authentication.getName();
        return ResponseEntity.ok(userService.updateProfile(email, request));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        String email = authentication.getName();
        userService.changePassword(email, request);
        return ResponseEntity.noContent().build();
    }
}
