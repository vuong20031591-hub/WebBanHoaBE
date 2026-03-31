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

@Service
@Transactional
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;

    public AddressServiceImpl(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getUserAddresses(String userId) {
        return addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId)
                .stream()
                .map(AddressResponse::fromEntity)
                .toList();
    }

    @Override
    public AddressResponse createAddress(String userId, CreateAddressRequest request) {
        Boolean isDefault = request.isDefault() != null ? request.isDefault() : false;

        if (isDefault) {
            addressRepository.findByUserIdAndIsDefaultTrue(userId)
                    .ifPresent(existingDefault -> {
                        existingDefault.setIsDefault(false);
                        addressRepository.save(existingDefault);
                    });
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
        return AddressResponse.fromEntity(address);
    }

    @Override
    public AddressResponse updateAddress(String userId, Long addressId, UpdateAddressRequest request) {
        Address address = getAddressOrThrow(userId, addressId);

        address.setFullName(request.fullName());
        address.setPhone(request.phone());
        address.setAddress(request.address());
        address.setCity(request.city());
        address.setDistrict(request.district());
        address.setWard(request.ward());

        address = addressRepository.save(address);
        return AddressResponse.fromEntity(address);
    }

    @Override
    public void deleteAddress(String userId, Long addressId) {
        Address address = getAddressOrThrow(userId, addressId);
        addressRepository.delete(address);
    }

    @Override
    public AddressResponse setDefaultAddress(String userId, Long addressId) {
        Address address = getAddressOrThrow(userId, addressId);

        addressRepository.findByUserIdAndIsDefaultTrue(userId)
                .ifPresent(existingDefault -> {
                    if (!existingDefault.getId().equals(addressId)) {
                        existingDefault.setIsDefault(false);
                        addressRepository.save(existingDefault);
                    }
                });

        address.setIsDefault(true);
        address = addressRepository.save(address);
        return AddressResponse.fromEntity(address);
    }

    private Address getAddressOrThrow(String userId, Long addressId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));
    }
}
