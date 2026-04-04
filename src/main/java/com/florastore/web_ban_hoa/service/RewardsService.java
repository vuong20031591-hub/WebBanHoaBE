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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class RewardsService {

    public static final BigDecimal REDEEM_POINT_VALUE_VND = BigDecimal.valueOf(1000);
    public static final BigDecimal EARN_POINT_STEP_VND = BigDecimal.valueOf(1000);
    public static final String REWARD_TYPE_ORDER_EARN = "EARN_ORDER";
    public static final String REWARD_TYPE_ORDER_REDEEM = "REDEEM_ORDER";
    public static final String REWARD_TYPE_ORDER_REDEEM_REFUND = "REFUND_REDEEM_ORDER";
    public static final String REWARD_TYPE_ORDER_EARN_REVERSE = "REVERSE_EARN_ORDER";

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

    public int getApplicableRedeemPoints(Long userId, BigDecimal orderSubtotal, Integer requestedPoints) {
        if (requestedPoints == null || requestedPoints <= 0 || orderSubtotal == null || orderSubtotal.signum() <= 0) {
            return 0;
        }

        UserRewards rewards = userRewardsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultRewards(userId));

        int maxByAmount = orderSubtotal.divide(REDEEM_POINT_VALUE_VND, 0, RoundingMode.DOWN).intValue();
        return Math.max(0, Math.min(requestedPoints, Math.min(rewards.getPoints(), maxByAmount)));
    }

    public BigDecimal calculateDiscountForPoints(int points) {
        if (points <= 0) {
            return BigDecimal.ZERO;
        }
        return REDEEM_POINT_VALUE_VND.multiply(BigDecimal.valueOf(points));
    }

    @Transactional
    public void redeemExactPointsForOrder(Long userId, int points, Long orderId) {
        if (points <= 0) {
            return;
        }

        if (transactionRepository.existsByUserIdAndTypeAndOrderId(userId, REWARD_TYPE_ORDER_REDEEM, orderId)) {
            return;
        }

        UserRewards rewards = userRewardsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultRewards(userId));

        if (rewards.getPoints() < points) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient points to redeem");
        }

        rewards.setPoints(rewards.getPoints() - points);
        userRewardsRepository.save(rewards);

        RewardsTransaction transaction = new RewardsTransaction();
        transaction.setUserId(userId);
        transaction.setPoints(-points);
        transaction.setType(REWARD_TYPE_ORDER_REDEEM);
        transaction.setDescription("Redeemed points for order #" + orderId);
        transaction.setOrderId(orderId);
        transactionRepository.save(transaction);
    }

    @Transactional
    public int awardPointsForOrder(Long userId, BigDecimal paidAmount, Long orderId) {
        if (paidAmount == null || paidAmount.signum() <= 0) {
            return 0;
        }

        if (transactionRepository.existsByUserIdAndTypeAndOrderId(userId, REWARD_TYPE_ORDER_EARN, orderId)) {
            return 0;
        }

        int earnedPoints = paidAmount.divide(EARN_POINT_STEP_VND, 0, RoundingMode.DOWN).intValue();
        if (earnedPoints <= 0) {
            return 0;
        }

        UserRewards rewards = userRewardsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultRewards(userId));

        rewards.setPoints(rewards.getPoints() + earnedPoints);
        rewards.setLifetimePoints(rewards.getLifetimePoints() + earnedPoints);
        rewards.setTier(calculateTier(rewards.getLifetimePoints()));
        userRewardsRepository.save(rewards);

        RewardsTransaction transaction = new RewardsTransaction();
        transaction.setUserId(userId);
        transaction.setPoints(earnedPoints);
        transaction.setType(REWARD_TYPE_ORDER_EARN);
        transaction.setDescription("Earned points from order #" + orderId);
        transaction.setOrderId(orderId);
        transactionRepository.save(transaction);

        return earnedPoints;
    }

    @Transactional
    public void rollbackRewardsForCancelledOrder(
            Long userId,
            Long orderId,
            Integer redeemedPoints,
            BigDecimal paidAmount
    ) {
        refundRedeemedPointsForOrder(userId, orderId, redeemedPoints);
        reverseEarnedPointsForOrder(userId, orderId, paidAmount);
    }

    private void refundRedeemedPointsForOrder(Long userId, Long orderId, Integer redeemedPoints) {
        if (redeemedPoints == null || redeemedPoints <= 0) {
            return;
        }

        if (!transactionRepository.existsByUserIdAndTypeAndOrderId(userId, REWARD_TYPE_ORDER_REDEEM, orderId)) {
            return;
        }

        if (transactionRepository.existsByUserIdAndTypeAndOrderId(userId, REWARD_TYPE_ORDER_REDEEM_REFUND, orderId)) {
            return;
        }

        UserRewards rewards = userRewardsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultRewards(userId));

        rewards.setPoints(rewards.getPoints() + redeemedPoints);
        userRewardsRepository.save(rewards);

        RewardsTransaction transaction = new RewardsTransaction();
        transaction.setUserId(userId);
        transaction.setPoints(redeemedPoints);
        transaction.setType(REWARD_TYPE_ORDER_REDEEM_REFUND);
        transaction.setDescription("Refunded redeemed points for cancelled order #" + orderId);
        transaction.setOrderId(orderId);
        transactionRepository.save(transaction);
    }

    private void reverseEarnedPointsForOrder(Long userId, Long orderId, BigDecimal paidAmount) {
        if (transactionRepository.existsByUserIdAndTypeAndOrderId(userId, REWARD_TYPE_ORDER_EARN_REVERSE, orderId)) {
            return;
        }

        if (!transactionRepository.existsByUserIdAndTypeAndOrderId(userId, REWARD_TYPE_ORDER_EARN, orderId)) {
            return;
        }

        if (paidAmount == null || paidAmount.signum() <= 0) {
            return;
        }

        int earnedPoints = paidAmount.divide(EARN_POINT_STEP_VND, 0, RoundingMode.DOWN).intValue();
        if (earnedPoints <= 0) {
            return;
        }
        UserRewards rewards = userRewardsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultRewards(userId));

        if (rewards.getPoints() < earnedPoints) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot cancel order because earned points from this order were already spent"
            );
        }

        rewards.setPoints(rewards.getPoints() - earnedPoints);
        rewards.setLifetimePoints(Math.max(0, rewards.getLifetimePoints() - earnedPoints));
        rewards.setTier(calculateTier(rewards.getLifetimePoints()));
        userRewardsRepository.save(rewards);

        RewardsTransaction transaction = new RewardsTransaction();
        transaction.setUserId(userId);
        transaction.setPoints(-earnedPoints);
        transaction.setType(REWARD_TYPE_ORDER_EARN_REVERSE);
        transaction.setDescription("Reversed earned points for cancelled order #" + orderId);
        transaction.setOrderId(orderId);
        transactionRepository.save(transaction);
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
