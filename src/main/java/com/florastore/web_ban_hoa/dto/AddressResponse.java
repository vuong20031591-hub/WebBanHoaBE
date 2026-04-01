package com.florastore.web_ban_hoa.dto;

import com.florastore.web_ban_hoa.entity.Address;

import java.time.LocalDateTime;

public record AddressResponse(
        Long id,
        String fullName,
        String phone,
        String address,
        String city,
        String district,
        String ward,
        Boolean isDefault,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AddressResponse from(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getFullName(),
                address.getPhone(),
                address.getAddress(),
                address.getCity(),
                address.getDistrict(),
                address.getWard(),
                address.getIsDefault(),
                address.getCreatedAt(),
                address.getUpdatedAt()
        );
    }
}
