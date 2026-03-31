package com.florastore.web_ban_hoa.repository;

import com.florastore.web_ban_hoa.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserIdOrderByIsDefaultDescCreatedAtDesc(String userId);
    Optional<Address> findByIdAndUserId(Long id, String userId);
    Optional<Address> findByUserIdAndIsDefaultTrue(String userId);
}
