package com.florastore.web_ban_hoa.repository;

import com.florastore.web_ban_hoa.entity.CollaboratorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollaboratorProfileRepository extends JpaRepository<CollaboratorProfile, Long> {

    interface CollaboratorUserView {
        Long getUserId();
        String getEmail();
        String getFullName();
        String getPhone();
        String getUserRole();
        String getBadge();
        String getPositionTitle();
        String getPositionDescription();
    }

    Optional<CollaboratorProfile> findByUserId(Long userId);
    void deleteByUserId(Long userId);

    @Query("select c.userId from CollaboratorProfile c")
    List<Long> findAllUserIds();

    @Query(value = """
            SELECT
                u.id AS userId,
                u.email AS email,
                u.full_name AS fullName,
                u.phone AS phone,
                u.role AS userRole,
                c.badge AS badge,
                c.position_title AS positionTitle,
                c.position_description AS positionDescription
            FROM collaborator_profiles c
            JOIN users u ON u.id = c.user_id
            ORDER BY LOWER(u.full_name) ASC, u.id ASC
            """, nativeQuery = true)
    List<CollaboratorUserView> findCollaboratorUserViews();
}
