package com.florastore.web_ban_hoa.repository;

import com.florastore.web_ban_hoa.entity.CollaboratorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollaboratorProfileRepository extends JpaRepository<CollaboratorProfile, Long> {

    Optional<CollaboratorProfile> findByUserId(Long userId);
    void deleteByUserId(Long userId);

    @Query("select c.userId from CollaboratorProfile c")
    List<Long> findAllUserIds();
}
