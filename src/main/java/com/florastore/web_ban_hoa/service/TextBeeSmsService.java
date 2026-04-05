package com.florastore.web_ban_hoa.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TextBeeSmsService {

    private static final Logger logger = LoggerFactory.getLogger(TextBeeSmsService.class);

    private final RestClient restClient;
    private final String apiKey;
    private final String deviceId;
    private final Integer simSubscriptionId;
    private final int otpExpiresInSeconds;
    private final int otpMaxAttempts;

    public TextBeeSmsService(
            @Value("${sms.textbee.api-base-url:https://api.textbee.dev/api/v1}") String apiBaseUrl,
            @Value("${sms.textbee.api-key:}") String apiKey,
            @Value("${sms.textbee.device-id:}") String deviceId,
            @Value("${sms.textbee.sim-subscription-id:}") String simSubscriptionId,
            @Value("${sms.otp.expires-in-seconds:300}") int otpExpiresInSeconds,
            @Value("${sms.otp.max-attempts:3}") int otpMaxAttempts
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(trimTrailingSlash(apiBaseUrl))
                .build();
        this.apiKey = normalizeValue(apiKey);
        this.deviceId = normalizeValue(deviceId);
        this.simSubscriptionId = parseOptionalInteger(simSubscriptionId, "sms.textbee.sim-subscription-id");
        this.otpExpiresInSeconds = otpExpiresInSeconds;
        this.otpMaxAttempts = otpMaxAttempts;
    }

    public SmsSendResult sendSmsNotificationsEnabledMessage(String rawPhone) {
        return sendSms(
                rawPhone,
                "Floral Boutique: SMS order updates have been enabled for this phone number."
        );
    }

    public SmsSendResult sendSmsEventRemindersEnabledMessage(String rawPhone) {
        return sendSms(
                rawPhone,
                "Floral Boutique: SMS event reminders have been enabled for this phone number."
        );
    }

    public SmsSendResult sendTestSms(String rawPhone) {
        return sendSms(
                rawPhone,
                "Floral Boutique test SMS: your SMS notifications are configured for this saved phone number."
        );
    }

    public SmsSendResult sendTwoFactorCode(String rawPhone, String code) {
        return sendSms(
                rawPhone,
                "Floral Boutique verification code: " + code +
                        ". It expires in " + Math.max(1, otpExpiresInSeconds / 60) +
                        " minute(s). Max attempts: " + otpMaxAttempts + "."
        );
    }

    public SmsSendResult sendSms(String rawPhone, String message) {
        if (!isConfigured()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "TextBee SMS gateway is not configured. Set TEXTBEE_API_KEY and TEXTBEE_DEVICE_ID."
            );
        }

        String normalizedPhone = normalizePhone(rawPhone);
        if (!StringUtils.hasText(message)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SMS message cannot be empty");
        }

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("recipients", List.of(normalizedPhone));
        requestBody.put("message", message);
        if (simSubscriptionId != null) {
            requestBody.put("simSubscriptionId", simSubscriptionId);
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/gateway/devices/{deviceId}/send-sms", deviceId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("x-api-key", apiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (simSubscriptionId != null) {
                logger.info("TextBee SMS accepted for {} using simSubscriptionId={}", normalizedPhone, simSubscriptionId);
            } else {
                logger.info("TextBee SMS accepted for {}", normalizedPhone);
            }
            return new SmsSendResult(normalizedPhone, stringifyResponse(response));
        } catch (RestClientResponseException ex) {
            String providerBody = ex.getResponseBodyAsString();
            logger.warn("TextBee rejected SMS request for {}. status={} body={}",
                    normalizedPhone, ex.getStatusCode(), providerBody);
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "TextBee rejected the SMS request. " + summarizeProviderError(providerBody),
                    ex
            );
        } catch (Exception ex) {
            logger.error("Failed to send SMS through TextBee to {}", normalizedPhone, ex);
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to send SMS through TextBee right now.",
                    ex
            );
        }
    }

    public boolean isConfigured() {
        return StringUtils.hasText(apiKey) && StringUtils.hasText(deviceId);
    }

    private static String normalizePhone(String rawPhone) {
        String trimmed = normalizeValue(rawPhone).replaceAll("[^0-9+]", "");
        if (!StringUtils.hasText(trimmed)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone number is required for SMS delivery");
        }

        if (trimmed.startsWith("+")) {
            return trimmed;
        }

        if (trimmed.startsWith("0")) {
            return "+84" + trimmed.substring(1);
        }

        if (trimmed.startsWith("84")) {
            return "+" + trimmed;
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Phone number must be a valid Vietnamese mobile number"
        );
    }

    private static String stringifyResponse(Map<String, Object> response) {
        if (response == null || response.isEmpty()) {
            return "accepted";
        }
        return response.toString();
    }

    private static String summarizeProviderError(String providerBody) {
        String normalized = normalizeValue(providerBody);
        return normalized.isEmpty() ? "No provider response body was returned." : normalized;
    }

    private static String trimTrailingSlash(String value) {
        String normalized = normalizeValue(value);
        if (normalized.endsWith("/")) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String normalizeValue(String value) {
        return value == null ? "" : value.trim();
    }

    private Integer parseOptionalInteger(String rawValue, String propertyName) {
        String value = normalizeValue(rawValue);
        if (!StringUtils.hasText(value)) {
            return null;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            logger.warn("Invalid integer for {}: '{}'. Falling back to device default SIM.", propertyName, value);
            return null;
        }
    }

    public record SmsSendResult(
            String normalizedPhone,
            String providerDetail
    ) {
    }
}
