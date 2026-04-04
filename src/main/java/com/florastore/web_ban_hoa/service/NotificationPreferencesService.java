package com.florastore.web_ban_hoa.service;

import com.florastore.web_ban_hoa.dto.NotificationPreferencesResponse;
import com.florastore.web_ban_hoa.dto.UpdateNotificationPreferencesRequest;
import com.florastore.web_ban_hoa.entity.NotificationPreferences;
import com.florastore.web_ban_hoa.repository.NotificationPreferencesRepository;
import com.florastore.web_ban_hoa.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationPreferencesService {

    private final NotificationPreferencesRepository repository;
    private final UserRepository userRepository;
    private final NewsletterService newsletterService;

    public NotificationPreferencesService(
            NotificationPreferencesRepository repository,
            UserRepository userRepository,
            NewsletterService newsletterService
    ) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.newsletterService = newsletterService;
    }

    public NotificationPreferencesResponse getPreferences(String userId) {
        Long userIdLong = Long.parseLong(userId);
        NotificationPreferences prefs = repository.findByUserId(userIdLong)
                .orElseGet(() -> createDefaultPreferences(userIdLong));

        if (Boolean.TRUE.equals(prefs.getPushArtistUpdates())) {
            prefs.setPushArtistUpdates(false);
            prefs = repository.save(prefs);
        }

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
            syncNewsletterPreferenceIfChanged(userIdLong, prefs, request.emailNewsletter());
            prefs.setEmailNewsletter(request.emailNewsletter());
        }
        if (request.smsOrderUpdates() != null) {
            prefs.setSmsOrderUpdates(request.smsOrderUpdates());
        }
        if (request.pushArtistUpdates() != null) {
            // Push workflow is not integrated yet. Keep persisted value disabled.
            prefs.setPushArtistUpdates(false);
        }

        NotificationPreferences saved = repository.save(prefs);
        return NotificationPreferencesResponse.from(saved);
    }

    @Transactional
    protected NotificationPreferences createDefaultPreferences(Long userId) {
        NotificationPreferences prefs = new NotificationPreferences(userId);
        return repository.save(prefs);
    }

    private void syncNewsletterPreferenceIfChanged(
            Long userId,
            NotificationPreferences preferences,
            boolean nextValue
    ) {
        if (Boolean.valueOf(nextValue).equals(preferences.getEmailNewsletter())) {
            return;
        }

        String email = userRepository.findById(userId)
                .map(user -> user.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        newsletterService.syncSubscriptionPreference(email, nextValue, "profile_notifications");
    }
}
