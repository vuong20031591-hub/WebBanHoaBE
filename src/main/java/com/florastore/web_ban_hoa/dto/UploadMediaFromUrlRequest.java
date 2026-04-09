package com.florastore.web_ban_hoa.dto;

import jakarta.validation.constraints.NotBlank;

public record UploadMediaFromUrlRequest(
        @NotBlank(message = "imageUrl is required")
        String imageUrl
) {
}
