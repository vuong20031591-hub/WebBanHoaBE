package com.florastore.web_ban_hoa.repository;

import com.florastore.web_ban_hoa.entity.SmsVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SmsVerificationTokenRepository extends JpaRepository<SmsVerificationToken, Long> {

    List<SmsVerificationToken> findByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(Long userId, String purpose);

    Optional<SmsVerificationToken> findFirstByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(Long userId, String purpose);
}
