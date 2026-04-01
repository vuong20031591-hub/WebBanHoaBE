package com.florastore.web_ban_hoa.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Component
public class JwtValidator {

    private static final Logger log = LoggerFactory.getLogger(JwtValidator.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private final String issuer;

    public JwtValidator(@Value("${jwt.issuer}") String issuer) {
        this.issuer = issuer;
    }

    public Map<String, Object> validateAndParse(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid JWT token format");
            }

            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            String payloadJson = new String(payload, StandardCharsets.UTF_8);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> claims = objectMapper.readValue(payloadJson, Map.class);
            
            String tokenIssuer = (String) claims.get("iss");
            if (tokenIssuer == null || !tokenIssuer.equals(issuer)) {
                log.warn("JWT issuer mismatch. Expected: {}, Got: {}", issuer, tokenIssuer);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid JWT issuer");
            }
            
            log.debug("JWT validated successfully for user: {}", claims.get("sub"));
            return claims;
                    
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to parse JWT", ex);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unable to parse JWT token: " + ex.getMessage(), ex);
        }
    }

    public String extractUserId(Map<String, Object> claims) {
        String subject = (String) claims.get("sub");
        if (subject == null || subject.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT subject is missing");
        }
        return subject;
    }
}
