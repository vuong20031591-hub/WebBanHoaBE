package com.florastore.web_ban_hoa.service;

import com.florastore.web_ban_hoa.dto.AddressResponse;
import com.florastore.web_ban_hoa.dto.CreateAddressRequest;
import com.florastore.web_ban_hoa.dto.UpdateAddressRequest;

import java.util.List;

public interface AddressService {
    List<AddressResponse> getUserAddresses(String userId);
    AddressResponse createAddress(String userId, CreateAddressRequest request);
    AddressResponse updateAddress(String userId, Long addressId, UpdateAddressRequest request);
    void deleteAddress(String userId, Long addressId);
    AddressResponse setDefaultAddress(String userId, Long addressId);
}
