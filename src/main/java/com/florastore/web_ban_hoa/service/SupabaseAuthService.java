package com.florastore.web_ban_hoa.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.florastore.web_ban_hoa.dto.SupabaseUserProfile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class SupabaseAuthService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;
    private final String configuredAuthBaseUrl;
    private final String supabaseAnonKey;
    private final String dbUsername;

    public SupabaseAuthService(
            @Value("${SUPABASE_AUTH_BASE_URL:}") String configuredAuthBaseUrl,
            @Value("${SUPABASE_ANON_KEY:}") String supabaseAnonKey,
            @Value("${DB_USERNAME:}") String dbUsername
    ) {
        this.httpClient = HttpClient.newHttpClient();
        this.configuredAuthBaseUrl = configuredAuthBaseUrl;
        this.supabaseAnonKey = supabaseAnonKey;
        this.dbUsername = dbUsername;
    }

    public SupabaseUserProfile verifyAndGetProfile(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing Supabase access token");
        }

        String authBaseUrl = resolveAuthBaseUrl();
        String endpoint = authBaseUrl + "/user";

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .GET();

        if (supabaseAnonKey != null && !supabaseAnonKey.isBlank()) {
            requestBuilder.header("apikey", supabaseAnonKey);
        }

        try {
            HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            int status = response.statusCode();
            if (status == 401 || status == 403) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Supabase access token");
            }
            if (status < 200 || status >= 300) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Unable to verify Supabase token (status " + status + ")"
                );
            }

            JsonNode root = objectMapper.readTree(response.body());
            String email = text(root, "email");
            if (email == null || email.isBlank()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Supabase user email is missing");
            }

            JsonNode metadata = root.path("user_metadata");
            String fullName = text(metadata, "full_name");
            if (fullName == null || fullName.isBlank()) {
                fullName = text(metadata, "name");
            }
            if (fullName == null || fullName.isBlank()) {
                fullName = email.split("@")[0];
            }

            String phone = text(metadata, "phone");
            if (phone == null || phone.isBlank()) {
                phone = "0000000000";
            }

            return new SupabaseUserProfile(email, fullName, phone);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to verify Supabase token",
                    ex
            );
        }
    }

    private String resolveAuthBaseUrl() {
        String sanitized = sanitizeConfiguredAuthBaseUrl(configuredAuthBaseUrl);
        if (isValidConfiguredAuthBaseUrl(sanitized)) {
            return stripTrailingSlash(sanitized);
        }

        if (dbUsername != null && dbUsername.startsWith("postgres.")) {
            String projectRef = dbUsername.substring("postgres.".length()).trim();
            if (!projectRef.isBlank()) {
                return "https://" + projectRef + ".supabase.co/auth/v1";
            }
        }

        throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Cannot resolve Supabase auth base URL. Set SUPABASE_AUTH_BASE_URL in .env"
        );
    }

    private static boolean isValidConfiguredAuthBaseUrl(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.trim();
        if (normalized.contains("<") || normalized.contains(">")) {
            return false;
        }
        return !normalized.contains("YOUR_PROJECT_REF");
    }

    private static String sanitizeConfiguredAuthBaseUrl(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        String prefix = "SUPABASE_AUTH_BASE_URL=";
        if (normalized.startsWith(prefix)) {
            normalized = normalized.substring(prefix.length()).trim();
        }
        return normalized;
    }

    private static String stripTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode()) {
            return null;
        }
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
