package com.florastore.web_ban_hoa.dto;

import com.florastore.web_ban_hoa.entity.Role;
import jakarta.validation.constraints.NotNull;

public record AdminUpdateUserRoleRequest(
        @NotNull(message = "Role is required")
        Role role
) {
}
