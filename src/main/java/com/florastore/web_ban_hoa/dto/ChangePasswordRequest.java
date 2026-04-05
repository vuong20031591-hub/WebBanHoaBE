package com.florastore.web_ban_hoa.dto;

import com.florastore.web_ban_hoa.validation.AuthValidationRules;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Pattern(regexp = AuthValidationRules.PASSWORD_REGEX, message = AuthValidationRules.PASSWORD_MESSAGE)
        String newPassword
) {
}
