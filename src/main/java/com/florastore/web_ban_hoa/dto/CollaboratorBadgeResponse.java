package com.florastore.web_ban_hoa.dto;

public enum CollaboratorBadgeResponse {
    STAFF,
    ADMIN;

    public static CollaboratorBadgeResponse fromNullable(String value) {
        if (value == null || value.isBlank()) {
            return STAFF;
        }

        try {
            return CollaboratorBadgeResponse.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return STAFF;
        }
    }
}
