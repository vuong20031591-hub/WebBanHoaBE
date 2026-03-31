package com.florastore.web_ban_hoa.service.impl;

import com.florastore.web_ban_hoa.dto.SignedUrlResponse;
import com.florastore.web_ban_hoa.dto.UploadMediaResponse;
import com.florastore.web_ban_hoa.service.MediaStorageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@ConditionalOnProperty(prefix = "r2", name = "enabled", havingValue = "false", matchIfMissing = true)
public class DisabledMediaStorageService implements MediaStorageService {

    private static final String MESSAGE = "R2 storage is disabled. Set r2.enabled=true to use upload APIs.";

    @Override
    public UploadMediaResponse upload(MultipartFile file) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, MESSAGE);
    }

    @Override
    public void delete(String key) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, MESSAGE);
    }

    @Override
    public SignedUrlResponse signedUrl(String key, long expiresInSeconds) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, MESSAGE);
    }
}
