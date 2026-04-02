package com.florastore.web_ban_hoa.security;

import com.florastore.web_ban_hoa.service.JwtService;
import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class JwtSubjectResolver {

    private final JwtService jwtService;

    public JwtSubjectResolver(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public String resolveUserId(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Bearer token");
        }

        String token = authorizationHeader.substring(7).trim();
        Claims claims = jwtService.validateAndParse(token);
        return jwtService.extractUserId(claims);
    }
}
