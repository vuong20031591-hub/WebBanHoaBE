package com.florastore.web_ban_hoa.service;

import com.florastore.web_ban_hoa.config.MediaLocalProperties;
import com.florastore.web_ban_hoa.dto.SignedUrlResponse;
import com.florastore.web_ban_hoa.dto.UploadMediaResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class LocalMediaStorageSupport {

    private static final String LOCAL_KEY_PREFIX = "local/";
    private static final DateTimeFormatter KEY_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM");

    private final MediaLocalProperties mediaLocalProperties;

    public LocalMediaStorageSupport(MediaLocalProperties mediaLocalProperties) {
        this.mediaLocalProperties = mediaLocalProperties;
    }

    public UploadMediaResponse upload(byte[] bytes, String contentType) {
        String relativePath = buildRelativePath();
        Path targetPath = resolveRelativePath(relativePath);

        try {
            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, bytes);
            return new UploadMediaResponse(
                    LOCAL_KEY_PREFIX + relativePath,
                    buildPublicUrl(relativePath),
                    bytes.length,
                    contentType
            );
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file locally", ex);
        }
    }

    public boolean supports(String key) {
        return key != null && key.startsWith(LOCAL_KEY_PREFIX);
    }

    public void delete(String key) {
        Path targetPath = resolveRelativePath(toRelativePath(key));
        try {
            Files.deleteIfExists(targetPath);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete local file", ex);
        }
    }

    public SignedUrlResponse signedUrl(String key, long expiresInSeconds) {
        String relativePath = toRelativePath(key);
        long effectiveExpiresInSeconds = expiresInSeconds > 0 ? expiresInSeconds : 600;
        return new SignedUrlResponse(key, buildPublicUrl(relativePath), effectiveExpiresInSeconds);
    }

    private String buildRelativePath() {
        return "images/" + LocalDate.now().format(KEY_DATE_FORMATTER) + "/" + UUID.randomUUID() + ".jpg";
    }

    private Path resolveRelativePath(String relativePath) {
        Path rootPath = Paths.get(mediaLocalProperties.getStoragePath()).toAbsolutePath().normalize();
        Path targetPath = rootPath.resolve(relativePath).normalize();

        if (!targetPath.startsWith(rootPath)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid local storage path");
        }

        return targetPath;
    }

    private String toRelativePath(String key) {
        if (!supports(key)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported local media key");
        }
        return key.substring(LOCAL_KEY_PREFIX.length());
    }

    private String buildPublicUrl(String relativePath) {
        String baseUrl = mediaLocalProperties.getPublicBaseUrl() == null
                ? ""
                : mediaLocalProperties.getPublicBaseUrl().trim();

        if (baseUrl.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "media.local.public-base-url is required");
        }

        return trimTrailingSlash(baseUrl) + "/" + relativePath;
    }

    private String trimTrailingSlash(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '/') {
            end--;
        }
        return value.substring(0, end);
    }
}
