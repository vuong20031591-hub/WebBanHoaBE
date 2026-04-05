package com.florastore.web_ban_hoa.dto;

public record UpdateNotificationPreferencesRequest(
        Boolean emailOrderUpdates,
        Boolean emailPromotions,
        Boolean emailNewsletter,
        Boolean emailEventReminders,
        Boolean smsOrderUpdates,
        Boolean smsEventReminders,
        Boolean pushArtistUpdates
) {
}
