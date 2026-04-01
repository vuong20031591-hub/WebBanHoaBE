package com.florastore.web_ban_hoa.dto;

public record AdminOrderStatsResponse(
        Long totalOrders,
        Long pendingOrders,
        Long confirmedOrders,
        Long cancelledOrders
) {
}
