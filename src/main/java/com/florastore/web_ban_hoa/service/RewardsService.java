package com.florastore.web_ban_hoa.service;

import com.florastore.web_ban_hoa.dto.RewardsTransactionResponse;
import com.florastore.web_ban_hoa.dto.UserRewardsResponse;
import com.florastore.web_ban_hoa.entity.RewardsTransaction;
import com.florastore.web_ban_hoa.entity.UserRewards;
import com.florastore.web_ban_hoa.repository.RewardsTransactionRepository;
import com.florastore.web_ban_hoa.repository.UserRewardsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RewardsService {

    private final UserRewardsRepository userRewardsRepository;
    private final RewardsTransactionRepository transactionRepository;

    public RewardsService(UserRewardsRepository userRewardsRepository,
                         RewardsTransactionRepository transactionRepository) {
        this.userRewardsRepository = userRewardsRepository;
        this.transactionRepository = transactionRepository;
    }

    public UserRewardsResponse getUserRewards(String userId) {
        Long userIdLong = Long.parseLong(userId);
        UserRewards rewards = userRewardsRepository.findByUserId(userIdLong)
                .orElseGet(() -> createDefaultRewards(userIdLong));
        return UserRewardsResponse.from(rewards);
    }

    public Page<RewardsTransactionResponse> getTransactionHistory(String userId, int page, int size) {
        Long userIdLong = Long.parseLong(userId);
        Pageable pageable = PageRequest.of(page, size);
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userIdLong, pageable)
                .map(RewardsTransactionResponse::from);
    }

    @Transactional
    public void addPoints(Long userId, int points, String type, String description, Long orderId) {
        UserRewards rewards = userRewardsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultRewards(userId));

        rewards.setPoints(rewards.getPoints() + points);
        rewards.setLifetimePoints(rewards.getLifetimePoints() + points);
        rewards.setTier(calculateTier(rewards.getLifetimePoints()));
        userRewardsRepository.save(rewards);

        RewardsTransaction transaction = new RewardsTransaction();
        transaction.setUserId(userId);
        transaction.setPoints(points);
        transaction.setType(type);
        transaction.setDescription(description);
        transaction.setOrderId(orderId);
        transactionRepository.save(transaction);
    }

    @Transactional
    public void deductPoints(Long userId, int points, String description) {
        UserRewards rewards = userRewardsRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User rewards not found"));

        if (rewards.getPoints() < points) {
            throw new RuntimeException("Insufficient points");
        }

        rewards.setPoints(rewards.getPoints() - points);
        userRewardsRepository.save(rewards);

        RewardsTransaction transaction = new RewardsTransaction();
        transaction.setUserId(userId);
        transaction.setPoints(-points);
        transaction.setType("REDEEM");
        transaction.setDescription(description);
        transactionRepository.save(transaction);
    }

    @Transactional
    protected UserRewards createDefaultRewards(Long userId) {
        UserRewards rewards = new UserRewards(userId);
        rewards.setPoints(450);
        rewards.setLifetimePoints(450);
        rewards.setTier("Bronze");
        return userRewardsRepository.save(rewards);
    }

    private String calculateTier(int lifetimePoints) {
        if (lifetimePoints >= 1500) {
            return "Gold";
        } else if (lifetimePoints >= 500) {
            return "Silver";
        } else {
            return "Bronze";
        }
    }
}
