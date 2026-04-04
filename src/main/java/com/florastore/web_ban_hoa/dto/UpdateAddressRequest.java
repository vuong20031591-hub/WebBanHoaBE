package com.florastore.web_ban_hoa.dto;

import jakarta.validation.constraints.Size;

public record UpdateAddressRequest(
        @Size(max = 128, message = "Full name must not exceed 128 characters")
        String fullName,

        @Size(max = 20, message = "Phone must not exceed 20 characters")
        String phone,

        @Size(max = 255, message = "Address must not exceed 255 characters")
        String address,

        @Size(max = 100, message = "City must not exceed 100 characters")
        String city,

        @Size(max = 100, message = "District must not exceed 100 characters")
        String district,

        @Size(max = 100, message = "Ward must not exceed 100 characters")
        String ward,

        Boolean isDefault
) {
}
