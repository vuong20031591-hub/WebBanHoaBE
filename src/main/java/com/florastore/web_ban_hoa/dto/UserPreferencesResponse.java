package com.florastore.web_ban_hoa.dto;

import com.florastore.web_ban_hoa.entity.UserPreferences;

public record UserPreferencesResponse(
        String language,
        String currency,
        String theme,
        String timezone,
        Boolean signatureWrap,
    Boolean ecoDelivery
) {
    public static UserPreferencesResponse from(UserPreferences prefs) {
        return new UserPreferencesResponse(
                prefs.getLanguage(),
                prefs.getCurrency(),
                prefs.getTheme(),
                prefs.getTimezone(),
                prefs.getSignatureWrap(),
        prefs.getEcoDelivery()
        );
    }
}
