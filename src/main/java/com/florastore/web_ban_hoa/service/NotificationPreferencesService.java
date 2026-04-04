package com.florastore.web_ban_hoa.service;

import com.florastore.web_ban_hoa.dto.NotificationPreferencesResponse;
import com.florastore.web_ban_hoa.dto.UpdateNotificationPreferencesRequest;
import com.florastore.web_ban_hoa.entity.NotificationPreferences;
import com.florastore.web_ban_hoa.repository.NotificationPreferencesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationPreferencesService {

    private final NotificationPreferencesRepository repository;

    public NotificationPreferencesService(NotificationPreferencesRepository repository) {
        this.repository = repository;
    }

    public NotificationPreferencesResponse getPreferences(String userId) {
        Long userIdLong = Long.parseLong(userId);
        NotificationPreferences prefs = repository.findByUserId(userIdLong)
                .orElseGet(() -> createDefaultPreferences(userIdLong));
        return NotificationPreferencesResponse.from(prefs);
    }

    @Transactional
    public NotificationPreferencesResponse updatePreferences(String userId, UpdateNotificationPreferencesRequest request) {
        Long userIdLong = Long.parseLong(userId);
        NotificationPreferences prefs = repository.findByUserId(userIdLong)
                .orElseGet(() -> createDefaultPreferences(userIdLong));

        if (request.emailOrderUpdates() != null) {
            prefs.setEmailOrderUpdates(request.emailOrderUpdates());
        }
        if (request.emailPromotions() != null) {
            prefs.setEmailPromotions(request.emailPromotions());
        }
        if (request.emailNewsletter() != null) {
            prefs.setEmailNewsletter(request.emailNewsletter());
        }
        if (request.smsOrderUpdates() != null) {
            prefs.setSmsOrderUpdates(request.smsOrderUpdates());
        }
        if (request.pushArtistUpdates() != null) {
            prefs.setPushArtistUpdates(request.pushArtistUpdates());
        }

        NotificationPreferences saved = repository.save(prefs);
        return NotificationPreferencesResponse.from(saved);
    }

    @Transactional
    protected NotificationPreferences createDefaultPreferences(Long userId) {
        NotificationPreferences prefs = new NotificationPreferences(userId);
        return repository.save(prefs);
    }
}
