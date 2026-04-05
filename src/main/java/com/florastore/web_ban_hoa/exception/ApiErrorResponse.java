package com.florastore.web_ban_hoa.exception;

import java.time.Instant;

public record ApiErrorResponse(
        int status,
        String code,
        String message,
        String path,
        Instant timestamp
) {
    public static ApiErrorResponse of(int status, String code, String message, String path) {
        return new ApiErrorResponse(status, code, message, path, Instant.now());
    }
}
