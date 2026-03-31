package com.florastore.web_ban_hoa.dto;

public record UploadMediaResponse(
        String key,
        String publicUrl,
        long size,
        String contentType
) {
}
