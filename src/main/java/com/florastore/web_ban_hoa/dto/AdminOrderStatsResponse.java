package com.florastore.web_ban_hoa.dto;

public record AdminOrderStatsResponse(
        long pendingCount,
        long confirmedCount,
        long cancelledCount,
        long totalCount
) {
}
