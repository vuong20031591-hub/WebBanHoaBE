package com.florastore.web_ban_hoa.repository;

import com.florastore.web_ban_hoa.entity.RewardsTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RewardsTransactionRepository extends JpaRepository<RewardsTransaction, Long> {
    Page<RewardsTransaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    boolean existsByUserIdAndTypeAndOrderId(Long userId, String type, Long orderId);
}
