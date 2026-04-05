package com.florastore.web_ban_hoa.dto;

import com.florastore.web_ban_hoa.entity.CollaboratorBadge;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminUpdateCollaboratorRequest(
        @NotNull(message = "Badge is required")
        CollaboratorBadge badge,

        @NotBlank(message = "Position title is required")
        @Size(max = 120, message = "Position title must be <= 120 characters")
        String positionTitle,

        @Size(max = 255, message = "Position description must be <= 255 characters")
        String positionDescription
) {
}
