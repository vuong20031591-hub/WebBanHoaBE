package com.florastore.web_ban_hoa.service.impl;

import com.florastore.web_ban_hoa.config.R2Properties;
import com.florastore.web_ban_hoa.dto.SignedUrlResponse;
import com.florastore.web_ban_hoa.dto.UploadMediaResponse;
import com.florastore.web_ban_hoa.service.MediaStorageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "r2", name = "enabled", havingValue = "true")
public class R2MediaStorageService implements MediaStorageService {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final R2Properties properties;

    public R2MediaStorageService(S3Client s3Client, S3Presigner s3Presigner, R2Properties properties) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.properties = properties;
    }

    @Override
    public UploadMediaResponse upload(MultipartFile file) {
        validateFile(file);

        String key = buildKey(file.getOriginalFilename());

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(request, software.amazon.awssdk.core.sync.RequestBody.fromBytes(file.getBytes()));

            String publicUrl = buildPublicUrl(key);
            return new UploadMediaResponse(key, publicUrl, file.getSize(), file.getContentType());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to upload file to R2", ex);
        }
    }

    @Override
    public void delete(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .build());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to delete file from R2", ex);
        }
    }

    @Override
    public SignedUrlResponse signedUrl(String key, long expiresInSeconds) {
        if (expiresInSeconds <= 0) {
            expiresInSeconds = 600;
        }

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .getObjectRequest(getObjectRequest)
                    .signatureDuration(Duration.ofSeconds(expiresInSeconds))
                    .build();

            String url = s3Presigner.presignGetObject(presignRequest).url().toString();
            return new SignedUrlResponse(key, url, expiresInSeconds);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to generate signed URL", ex);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File size exceeds 5MB");
        }
        if (file.getContentType() == null || !ALLOWED_TYPES.contains(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported file type");
        }
    }

    private String buildKey(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        return "uploads-" + UUID.randomUUID() + extension;
    }

    private String buildPublicUrl(String key) {
        if (properties.getPublicBaseUrl() != null && !properties.getPublicBaseUrl().isBlank()) {
            return properties.getPublicBaseUrl().replaceAll("/$", "") + "/" + key;
        }
        return "https://" + properties.getAccountId() + ".r2.cloudflarestorage.com/" + properties.getBucket() + "/" + key;
    }
}
