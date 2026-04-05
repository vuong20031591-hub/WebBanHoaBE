package com.florastore.web_ban_hoa.dto;

import com.florastore.web_ban_hoa.entity.CollaboratorBadge;
import com.florastore.web_ban_hoa.entity.User;

public record AdminCollaboratorResponse(
        Long id,
        String email,
        String fullName,
        String phone,
        String role,
        CollaboratorBadge badge,
        String positionTitle,
        String positionDescription
) {
    public static AdminCollaboratorResponse from(
            User user,
            CollaboratorBadge badge,
            String positionTitle,
            String positionDescription
    ) {
        return new AdminCollaboratorResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getRole().name(),
                badge,
                positionTitle,
                positionDescription
        );
    }
}
