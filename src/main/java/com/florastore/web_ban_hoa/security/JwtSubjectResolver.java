package com.florastore.web_ban_hoa.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class JwtSubjectResolver {

    private static final Pattern SUB_PATTERN = Pattern.compile("\"sub\"\\s*:\\s*\"([^\"]+)\"");

    public String resolveUserId(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Bearer token");
        }

        String token = authorizationHeader.substring(7).trim();
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid JWT token format");
        }

        try {
            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            String payloadJson = new String(payload, StandardCharsets.UTF_8);
            Matcher matcher = SUB_PATTERN.matcher(payloadJson);
            if (!matcher.find()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT subject is missing");
            }
            String subject = matcher.group(1);
            if (subject == null || subject.isBlank()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT subject is missing");
            }
            return subject;
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid JWT payload", ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unable to parse JWT payload", ex);
        }
    }
}
