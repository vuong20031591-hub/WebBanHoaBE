package com.florastore.web_ban_hoa.security;

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.net.URL;
import java.util.Date;
import java.util.Map;

@Component
public class SupabaseJwtValidator {

    private static final Logger log = LoggerFactory.getLogger(SupabaseJwtValidator.class);
    
    private final String jwksUrl;
    private final String issuer;
    private JWKSet jwkSet;
    private long jwkSetLastFetch = 0;
    private static final long JWKS_CACHE_TTL = 3600000; // 1 hour

    public SupabaseJwtValidator(
            @Value("${jwt.jwks-url}") String jwksUrl,
            @Value("${jwt.issuer}") String issuer
    ) {
        this.jwksUrl = jwksUrl;
        this.issuer = issuer;
    }

    public Map<String, Object> validateAndParse(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            
            if (!verifySignature(signedJWT)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid JWT signature");
            }
            
            Map<String, Object> claims = signedJWT.getJWTClaimsSet().getClaims();
            
            String tokenIssuer = (String) claims.get("iss");
            if (!issuer.equals(tokenIssuer)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid JWT issuer");
            }
            
            Date expiration = signedJWT.getJWTClaimsSet().getExpirationTime();
            if (expiration != null && expiration.before(new Date())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT token expired");
            }
            
            log.debug("JWT validated successfully for user: {}", claims.get("sub"));
            return claims;
            
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to validate JWT", ex);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unable to validate JWT: " + ex.getMessage(), ex);
        }
    }

    private boolean verifySignature(SignedJWT signedJWT) throws Exception {
        String kid = signedJWT.getHeader().getKeyID();
        
        if (jwkSet == null || System.currentTimeMillis() - jwkSetLastFetch > JWKS_CACHE_TTL) {
            jwkSet = JWKSet.load(new URL(jwksUrl));
            jwkSetLastFetch = System.currentTimeMillis();
            log.info("JWKS refreshed from {}", jwksUrl);
        }
        
        ECKey ecKey = (ECKey) jwkSet.getKeyByKeyId(kid);
        if (ecKey == null) {
            log.warn("Key ID {} not found in JWKS", kid);
            return false;
        }
        
        JWSVerifier verifier = new ECDSAVerifier(ecKey);
        return signedJWT.verify(verifier);
    }

    public String extractUserId(Map<String, Object> claims) {
        String subject = (String) claims.get("sub");
        if (subject == null || subject.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT subject is missing");
        }
        return subject;
    }
}
