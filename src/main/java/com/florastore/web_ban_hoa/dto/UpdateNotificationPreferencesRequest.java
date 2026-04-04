package com.florastore.web_ban_hoa.dto;

public record UpdateNotificationPreferencesRequest(
        Boolean emailOrderUpdates,
        Boolean emailPromotions,
        Boolean emailNewsletter,
        Boolean smsOrderUpdates,
        Boolean pushArtistUpdates
) {
}
