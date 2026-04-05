package com.florastore.web_ban_hoa.dto;

import com.florastore.web_ban_hoa.validation.AuthValidationRules;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password is required")
        @Pattern(regexp = AuthValidationRules.PASSWORD_REGEX, message = AuthValidationRules.PASSWORD_MESSAGE)
        String password,

        @NotBlank(message = "Full name is required")
        String fullName,

        @NotBlank(message = "Phone is required")
        @Size(max = 20, message = "Phone must be at most 20 characters")
        String phone
) {
}
