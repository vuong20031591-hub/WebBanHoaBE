package com.florastore.web_ban_hoa.service;

import com.florastore.web_ban_hoa.dto.AddressResponse;
import com.florastore.web_ban_hoa.dto.CreateAddressRequest;
import com.florastore.web_ban_hoa.dto.UpdateAddressRequest;
import com.florastore.web_ban_hoa.entity.Address;
import com.florastore.web_ban_hoa.repository.AddressRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class AddressService {

    private final AddressRepository addressRepository;

    public AddressService(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    public List<AddressResponse> getAddressesByUserId(String userId) {
        return addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId)
                .stream()
                .map(AddressResponse::from)
                .toList();
    }

    public AddressResponse createAddress(String userId, CreateAddressRequest request) {
        Boolean isDefault = request.isDefault() != null ? request.isDefault() : false;

        if (isDefault) {
            addressRepository.unsetAllDefaultForUser(userId);
        }

        Address address = new Address(
                userId,
                request.fullName(),
                request.phone(),
                request.address(),
                request.city(),
                request.district(),
                request.ward(),
                isDefault
        );

        address = addressRepository.save(address);
        return AddressResponse.from(address);
    }

    public AddressResponse updateAddress(String userId, Long addressId, UpdateAddressRequest request) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));

        if (request.fullName() != null) {
            address.setFullName(request.fullName());
        }
        if (request.phone() != null) {
            address.setPhone(request.phone());
        }
        if (request.address() != null) {
            address.setAddress(request.address());
        }
        if (request.city() != null) {
            address.setCity(request.city());
        }
        if (request.district() != null) {
            address.setDistrict(request.district());
        }
        if (request.ward() != null) {
            address.setWard(request.ward());
        }

        address = addressRepository.save(address);
        return AddressResponse.from(address);
    }

    public void deleteAddress(String userId, Long addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));

        addressRepository.delete(address);
    }

    public AddressResponse setDefaultAddress(String userId, Long addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));

        addressRepository.unsetDefaultForUser(userId, addressId);
        address.setIsDefault(true);
        address = addressRepository.save(address);

        return AddressResponse.from(address);
    }
}
