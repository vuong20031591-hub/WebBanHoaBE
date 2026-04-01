package com.florastore.web_ban_hoa.repository;

import com.florastore.web_ban_hoa.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByUserIdOrderByIsDefaultDescCreatedAtDesc(String userId);

    Optional<Address> findByIdAndUserId(Long id, String userId);

    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.userId = :userId AND a.id != :addressId")
    void unsetDefaultForUser(@Param("userId") String userId, @Param("addressId") Long addressId);

    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.userId = :userId")
    void unsetAllDefaultForUser(@Param("userId") String userId);

    Optional<Address> findByUserIdAndIsDefaultTrue(String userId);
}
