package com.florastore.web_ban_hoa.dto;

import com.florastore.web_ban_hoa.validation.AuthValidationRules;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordWithCodeRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Verification code is required")
        @Pattern(regexp = "\\d{6}", message = "Verification code must be 6 digits")
        String code,

        @NotBlank(message = "New password is required")
        @Pattern(regexp = AuthValidationRules.PASSWORD_REGEX, message = AuthValidationRules.PASSWORD_MESSAGE)
        String newPassword
) {
}
