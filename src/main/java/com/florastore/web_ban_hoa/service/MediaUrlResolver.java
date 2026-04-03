package com.florastore.web_ban_hoa.service;

import com.florastore.web_ban_hoa.config.R2Properties;
import org.springframework.stereotype.Component;

@Component
public class MediaUrlResolver {

    private final R2Properties r2Properties;

    public MediaUrlResolver(R2Properties r2Properties) {
        this.r2Properties = r2Properties;
    }

    public String resolveProductImage(String image) {
        if (image == null || image.isBlank()) {
            return image;
        }

        String normalized = image.trim();
        if (normalized.startsWith("http://") || normalized.startsWith("https://") || normalized.startsWith("/")) {
            return normalized;
        }

        String publicBaseUrl = safeTrim(r2Properties.getPublicBaseUrl());
        if (!publicBaseUrl.isEmpty()) {
            return trimTrailingSlash(publicBaseUrl) + "/" + normalized;
        }

        String accountId = safeTrim(r2Properties.getAccountId());
        String bucket = safeTrim(r2Properties.getBucket());
        if (!accountId.isEmpty() && !bucket.isEmpty()) {
            return "https://" + accountId + ".r2.cloudflarestorage.com/" + bucket + "/" + normalized;
        }

        return normalized;
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimTrailingSlash(String value) {
        int index = value.length();
        while (index > 0 && value.charAt(index - 1) == '/') {
            index--;
        }
        return value.substring(0, index);
    }
}
