package com.florastore.web_ban_hoa.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserPreferencesRequest(
        @Size(max = 10, message = "Language code must not exceed 10 characters")
        @Pattern(regexp = "^(vi|en)$", message = "Language must be 'vi' or 'en'")
        String language,

        @Size(max = 10, message = "Currency code must not exceed 10 characters")
        @Pattern(regexp = "^(VND|USD)$", message = "Currency must be 'VND' or 'USD'")
        String currency,

        @Size(max = 20, message = "Theme must not exceed 20 characters")
        @Pattern(regexp = "^(light|dark)$", message = "Theme must be 'light' or 'dark'")
        String theme,

        @Size(max = 50, message = "Timezone must not exceed 50 characters")
        String timezone,

        Boolean signatureWrap,

        Boolean ecoDelivery
) {
}
