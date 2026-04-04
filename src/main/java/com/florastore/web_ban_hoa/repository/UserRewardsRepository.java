package com.florastore.web_ban_hoa.repository;

import com.florastore.web_ban_hoa.entity.UserRewards;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRewardsRepository extends JpaRepository<UserRewards, Long> {
    Optional<UserRewards> findByUserId(Long userId);
}
