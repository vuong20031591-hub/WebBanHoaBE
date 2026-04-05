package com.florastore.web_ban_hoa.dto;

import com.florastore.web_ban_hoa.entity.NotificationPreferences;

public record NotificationPreferencesResponse(
        Boolean emailOrderUpdates,
        Boolean emailPromotions,
        Boolean emailNewsletter,
    Boolean emailEventReminders,
        Boolean smsOrderUpdates,
    Boolean smsEventReminders,
        Boolean pushArtistUpdates
) {
    public static NotificationPreferencesResponse from(NotificationPreferences prefs) {
        return new NotificationPreferencesResponse(
                prefs.getEmailOrderUpdates(),
                prefs.getEmailPromotions(),
                prefs.getEmailNewsletter(),
        prefs.getEmailEventReminders(),
                prefs.getSmsOrderUpdates(),
        prefs.getSmsEventReminders(),
                prefs.getPushArtistUpdates()
        );
    }
}
