package com.florastore.web_ban_hoa.controller;

import com.florastore.web_ban_hoa.dto.AddressResponse;
import com.florastore.web_ban_hoa.dto.CreateAddressRequest;
import com.florastore.web_ban_hoa.dto.UpdateAddressRequest;
import com.florastore.web_ban_hoa.security.JwtSubjectResolver;
import com.florastore.web_ban_hoa.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    private final AddressService addressService;
    private final JwtSubjectResolver jwtSubjectResolver;

    public AddressController(AddressService addressService, JwtSubjectResolver jwtSubjectResolver) {
        this.addressService = addressService;
        this.jwtSubjectResolver = jwtSubjectResolver;
    }

    @GetMapping
    public ResponseEntity<List<AddressResponse>> getAddresses(@RequestHeader("Authorization") String authHeader) {
        String userId = jwtSubjectResolver.resolveUserId(authHeader);
        return ResponseEntity.ok(addressService.getAddressesByUserId(userId));
    }

    @PostMapping
    public ResponseEntity<AddressResponse> createAddress(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody CreateAddressRequest request) {
        String userId = jwtSubjectResolver.resolveUserId(authHeader);
        return ResponseEntity.ok(addressService.createAddress(userId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddressResponse> updateAddress(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @Valid @RequestBody UpdateAddressRequest request) {
        String userId = jwtSubjectResolver.resolveUserId(authHeader);
        return ResponseEntity.ok(addressService.updateAddress(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAddress(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        String userId = jwtSubjectResolver.resolveUserId(authHeader);
        addressService.deleteAddress(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/set-default")
    public ResponseEntity<AddressResponse> setDefaultAddress(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        String userId = jwtSubjectResolver.resolveUserId(authHeader);
        return ResponseEntity.ok(addressService.setDefaultAddress(userId, id));
    }
}
