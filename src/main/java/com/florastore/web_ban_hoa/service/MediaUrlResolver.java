package com.florastore.web_ban_hoa.service;

import com.florastore.web_ban_hoa.config.R2Properties;
import com.florastore.web_ban_hoa.config.MediaLocalProperties;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class MediaUrlResolver {

    private final R2Properties r2Properties;
    private final MediaLocalProperties mediaLocalProperties;

    public MediaUrlResolver(R2Properties r2Properties, MediaLocalProperties mediaLocalProperties) {
        this.r2Properties = r2Properties;
        this.mediaLocalProperties = mediaLocalProperties;
    }

    public String resolveProductImage(String image) {
        if (image == null || image.isBlank()) {
            return image;
        }

        String normalized = image.trim();
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            return normalizeLegacyLoopbackMediaUrl(normalized);
        }

        if (normalized.startsWith("/")) {
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

    private String normalizeLegacyLoopbackMediaUrl(String absoluteUrl) {
        String mediaBaseUrl = safeTrim(mediaLocalProperties.getPublicBaseUrl());
        if (mediaBaseUrl.isEmpty()) {
            return absoluteUrl;
        }

        try {
            java.net.URI uri = java.net.URI.create(absoluteUrl);
            String host = safeTrim(uri.getHost()).toLowerCase(Locale.ROOT);
            String path = safeTrim(uri.getPath());

            if (!("localhost".equals(host) || "127.0.0.1".equals(host) || "0.0.0.0".equals(host))) {
                return absoluteUrl;
            }

            final String mediaPrefix = "/media/";
            if (!path.startsWith(mediaPrefix)) {
                return absoluteUrl;
            }

            String relativePath = path.substring(mediaPrefix.length());
            return trimTrailingSlash(mediaBaseUrl) + "/" + relativePath;
        } catch (IllegalArgumentException ex) {
            return absoluteUrl;
        }
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
