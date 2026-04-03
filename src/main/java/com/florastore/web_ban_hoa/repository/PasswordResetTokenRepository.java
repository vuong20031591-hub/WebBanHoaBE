package com.florastore.web_ban_hoa.repository;

import com.florastore.web_ban_hoa.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    List<PasswordResetToken> findByEmailAndUsedFalseOrderByCreatedAtDesc(String email);

    Optional<PasswordResetToken> findFirstByEmailAndUsedFalseOrderByCreatedAtDesc(String email);
}
