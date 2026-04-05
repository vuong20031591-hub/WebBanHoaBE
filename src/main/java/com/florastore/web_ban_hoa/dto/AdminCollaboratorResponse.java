package com.florastore.web_ban_hoa.dto;

import com.florastore.web_ban_hoa.entity.User;

public record AdminCollaboratorResponse(
        Long id,
        String email,
        String fullName,
        String phone,
        String role,
        CollaboratorBadgeResponse badge,
        String positionTitle,
        String positionDescription
) {
    private static final String DEFAULT_ROLE = "USER";
    private static final String DEFAULT_STAFF_TITLE = "Shop Staff";
    private static final String DEFAULT_ADMIN_TITLE = "Shop Administrator";

    public static AdminCollaboratorResponse from(
            User user,
            String badge,
            String positionTitle,
            String positionDescription
    ) {
        if (user == null) {
            throw new IllegalArgumentException("User is required");
        }

        String safeRole = user.getRole() != null ? user.getRole().name() : DEFAULT_ROLE;
        return fromValues(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                safeRole,
                badge,
                positionTitle,
                positionDescription
        );
    }

    public static AdminCollaboratorResponse fromValues(
            Long id,
            String email,
            String fullName,
            String phone,
            String role,
            String badge,
            String positionTitle,
            String positionDescription
    ) {
        CollaboratorBadgeResponse safeBadge = CollaboratorBadgeResponse.fromNullable(badge);
        String safeRole = normalizeNullable(role) != null ? role.trim().toUpperCase() : DEFAULT_ROLE;
        String safePositionTitle = normalizeNullable(positionTitle);
        if (safePositionTitle == null) {
            safePositionTitle = safeBadge == CollaboratorBadgeResponse.ADMIN
                    ? DEFAULT_ADMIN_TITLE
                    : DEFAULT_STAFF_TITLE;
        }

        return new AdminCollaboratorResponse(
                id,
                normalizeNullable(email),
                normalizeNullable(fullName),
                normalizeNullable(phone),
                safeRole,
                safeBadge,
                safePositionTitle,
                normalizeNullable(positionDescription)
        );
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
