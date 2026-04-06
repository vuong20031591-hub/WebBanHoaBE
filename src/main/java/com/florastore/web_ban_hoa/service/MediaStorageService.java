package com.florastore.web_ban_hoa.service;

import com.florastore.web_ban_hoa.dto.SignedUrlResponse;
import com.florastore.web_ban_hoa.dto.UploadMediaResponse;
import org.springframework.web.multipart.MultipartFile;

public interface MediaStorageService {
    UploadMediaResponse upload(MultipartFile file);

    UploadMediaResponse uploadFromUrl(String imageUrl);

    void delete(String key);

    SignedUrlResponse signedUrl(String key, long expiresInSeconds);
}
