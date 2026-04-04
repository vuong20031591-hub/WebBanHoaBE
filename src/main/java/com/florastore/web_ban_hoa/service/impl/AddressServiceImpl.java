package com.florastore.web_ban_hoa.service.impl;

import com.florastore.web_ban_hoa.dto.AddressResponse;
import com.florastore.web_ban_hoa.dto.CreateAddressRequest;
import com.florastore.web_ban_hoa.dto.UpdateAddressRequest;
import com.florastore.web_ban_hoa.entity.Address;
import com.florastore.web_ban_hoa.repository.AddressRepository;
import com.florastore.web_ban_hoa.service.AddressService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;

    public AddressServiceImpl(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @Override
    public List<AddressResponse> getUserAddresses(String userId) {
        return addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId)
                .stream()
                .map(AddressResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public AddressResponse getAddress(String userId, Long addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));
        return AddressResponse.from(address);
    }

    @Override
    @Transactional
    public AddressResponse createAddress(String userId, CreateAddressRequest request) {
        boolean setAsDefault = request.isDefault() != null && request.isDefault();
        
        if (setAsDefault) {
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
                setAsDefault
        );

        Address savedAddress = addressRepository.save(address);
        return AddressResponse.from(savedAddress);
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(String userId, Long addressId, UpdateAddressRequest request) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));

        if (request.fullName() != null && !request.fullName().isBlank()) {
            address.setFullName(request.fullName().trim());
        }
        if (request.phone() != null && !request.phone().isBlank()) {
            address.setPhone(request.phone().trim());
        }
        if (request.address() != null && !request.address().isBlank()) {
            address.setAddress(request.address().trim());
        }
        if (request.city() != null && !request.city().isBlank()) {
            address.setCity(request.city().trim());
        }
        if (request.district() != null && !request.district().isBlank()) {
            address.setDistrict(request.district().trim());
        }
        if (request.ward() != null) {
            address.setWard(request.ward().trim());
        }

        if (request.isDefault() != null && request.isDefault() && !address.getIsDefault()) {
            addressRepository.unsetDefaultForUser(userId, addressId);
            address.setIsDefault(true);
        }

        Address savedAddress = addressRepository.save(address);
        return AddressResponse.from(savedAddress);
    }

    @Override
    @Transactional
    public void deleteAddress(String userId, Long addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));
        addressRepository.delete(address);
    }

    @Override
    @Transactional
    public AddressResponse setPrimaryAddress(String userId, Long addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));

        if (!address.getIsDefault()) {
            addressRepository.unsetDefaultForUser(userId, addressId);
            address.setIsDefault(true);
            address = addressRepository.save(address);
        }

        return AddressResponse.from(address);
    }
}
