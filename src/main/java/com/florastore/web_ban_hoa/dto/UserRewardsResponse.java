package com.florastore.web_ban_hoa.dto;

import com.florastore.web_ban_hoa.entity.UserRewards;

public record UserRewardsResponse(
        Integer points,
        Integer lifetimePoints,
        String tier,
        Integer pointsToNextTier
) {
    public static UserRewardsResponse from(UserRewards rewards) {
        int pointsToNext = calculatePointsToNextTier(rewards.getLifetimePoints(), rewards.getTier());
        return new UserRewardsResponse(
                rewards.getPoints(),
                rewards.getLifetimePoints(),
                rewards.getTier(),
                pointsToNext
        );
    }

    private static int calculatePointsToNextTier(int lifetimePoints, String currentTier) {
        return switch (currentTier) {
            case "Bronze" -> Math.max(0, 500 - lifetimePoints);
            case "Silver" -> Math.max(0, 1500 - lifetimePoints);
            case "Gold" -> 0;
            default -> 0;
        };
    }
}
