package com.florastore.web_ban_hoa.controller;

import com.florastore.web_ban_hoa.dto.AddressResponse;
import com.florastore.web_ban_hoa.dto.CreateAddressRequest;
import com.florastore.web_ban_hoa.dto.UpdateAddressRequest;
import com.florastore.web_ban_hoa.security.JwtSubjectResolver;
import com.florastore.web_ban_hoa.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<List<AddressResponse>> getUserAddresses(
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        String userId = jwtSubjectResolver.resolveUserId(authorization);
        return ResponseEntity.ok(addressService.getUserAddresses(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AddressResponse> getAddress(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable Long id
    ) {
        String userId = jwtSubjectResolver.resolveUserId(authorization);
        return ResponseEntity.ok(addressService.getAddress(userId, id));
    }

    @PostMapping
    public ResponseEntity<AddressResponse> createAddress(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @Valid @RequestBody CreateAddressRequest request
    ) {
        String userId = jwtSubjectResolver.resolveUserId(authorization);
        return ResponseEntity.status(HttpStatus.CREATED).body(addressService.createAddress(userId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddressResponse> updateAddress(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @Valid @RequestBody UpdateAddressRequest request
    ) {
        String userId = jwtSubjectResolver.resolveUserId(authorization);
        return ResponseEntity.ok(addressService.updateAddress(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAddress(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable Long id
    ) {
        String userId = jwtSubjectResolver.resolveUserId(authorization);
        addressService.deleteAddress(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/set-primary")
    public ResponseEntity<AddressResponse> setPrimaryAddress(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable Long id
    ) {
        String userId = jwtSubjectResolver.resolveUserId(authorization);
        return ResponseEntity.ok(addressService.setPrimaryAddress(userId, id));
    }
}
