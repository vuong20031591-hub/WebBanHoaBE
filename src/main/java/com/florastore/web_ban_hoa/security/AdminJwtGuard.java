package com.florastore.web_ban_hoa.security;

import com.florastore.web_ban_hoa.service.JwtService;
import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AdminJwtGuard {

    private final JwtService jwtService;

    public AdminJwtGuard(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public long assertAdminAndGetUserId(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Bearer token");
        }

        String token = authorizationHeader.substring(7).trim();
        Claims claims = jwtService.validateAndParse(token);

        String role = jwtService.extractRole(claims);
        if (!"ADMIN".equalsIgnoreCase(role.trim())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ADMIN role is required");
        }

        String actorUserId = jwtService.extractUserId(claims);
        try {
            return Long.parseLong(actorUserId);
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid JWT subject for admin user");
        }
    }
}
