package com.florastore.web_ban_hoa.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Component
public class JwtSubjectResolver {

    private final SupabaseJwtValidator supabaseJwtValidator;

    public JwtSubjectResolver(SupabaseJwtValidator supabaseJwtValidator) {
        this.supabaseJwtValidator = supabaseJwtValidator;
    }

    public String resolveUserId(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Bearer token");
        }

        String token = authorizationHeader.substring(7).trim();
        Map<String, Object> claims = supabaseJwtValidator.validateAndParse(token);
        return supabaseJwtValidator.extractUserId(claims);
    }
}
