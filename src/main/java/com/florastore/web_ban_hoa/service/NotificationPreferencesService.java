package com.florastore.web_ban_hoa.service;

import com.florastore.web_ban_hoa.dto.NotificationPreferencesResponse;
import com.florastore.web_ban_hoa.dto.SmsTestResponse;
import com.florastore.web_ban_hoa.dto.UpdateNotificationPreferencesRequest;
import com.florastore.web_ban_hoa.entity.NotificationPreferences;
import com.florastore.web_ban_hoa.entity.User;
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
    private final EmailService emailService;
    private final TextBeeSmsService textBeeSmsService;

    public NotificationPreferencesService(
            NotificationPreferencesRepository repository,
            UserRepository userRepository,
            NewsletterService newsletterService,
            EmailService emailService,
            TextBeeSmsService textBeeSmsService
    ) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.newsletterService = newsletterService;
        this.emailService = emailService;
        this.textBeeSmsService = textBeeSmsService;
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
            notifyOrderUpdatesIfEnabled(userIdLong, prefs, request.emailOrderUpdates());
            prefs.setEmailOrderUpdates(request.emailOrderUpdates());
        }
        if (request.emailPromotions() != null) {
            notifySpecialOffersIfEnabled(userIdLong, prefs, request.emailPromotions());
            prefs.setEmailPromotions(request.emailPromotions());
        }
        if (request.emailNewsletter() != null) {
            syncNewsletterPreferenceIfChanged(userIdLong, prefs, request.emailNewsletter());
            prefs.setEmailNewsletter(request.emailNewsletter());
        }
        if (request.emailEventReminders() != null) {
            notifyEventRemindersEmailIfEnabled(userIdLong, prefs, request.emailEventReminders());
            prefs.setEmailEventReminders(request.emailEventReminders());
        }
        if (request.smsOrderUpdates() != null) {
            notifySmsOrderUpdatesIfEnabled(userIdLong, prefs, request.smsOrderUpdates());
            prefs.setSmsOrderUpdates(request.smsOrderUpdates());
        }
        if (request.smsEventReminders() != null) {
            notifySmsEventRemindersIfEnabled(userIdLong, prefs, request.smsEventReminders());
            prefs.setSmsEventReminders(request.smsEventReminders());
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

    public SmsTestResponse sendTestSms(String userId) {
        User user = getUserOrThrow(Long.parseLong(userId));
        String phone = getUserPhoneOrThrow(user);
        TextBeeSmsService.SmsSendResult result = textBeeSmsService.sendTestSms(phone);
        return new SmsTestResponse(
                "sent",
                result.normalizedPhone(),
                "TextBee",
                result.providerDetail()
        );
    }

    private void syncNewsletterPreferenceIfChanged(
            Long userId,
            NotificationPreferences preferences,
            boolean nextValue
    ) {
        if (Boolean.valueOf(nextValue).equals(preferences.getEmailNewsletter())) {
            return;
        }

        newsletterService.syncSubscriptionPreference(getUserEmailOrThrow(userId), nextValue, "profile_notifications");
    }

    private void notifySpecialOffersIfEnabled(
            Long userId,
            NotificationPreferences preferences,
            boolean nextValue
    ) {
        if (!nextValue || Boolean.valueOf(nextValue).equals(preferences.getEmailPromotions())) {
            return;
        }

        emailService.sendSpecialOffersSubscriptionEmail(getUserEmailOrThrow(userId));
    }

    private void notifyOrderUpdatesIfEnabled(
            Long userId,
            NotificationPreferences preferences,
            boolean nextValue
    ) {
        if (!nextValue || Boolean.valueOf(nextValue).equals(preferences.getEmailOrderUpdates())) {
            return;
        }

        emailService.sendOrderUpdatesSubscriptionEmail(getUserEmailOrThrow(userId));
    }

    private void notifyEventRemindersEmailIfEnabled(
            Long userId,
            NotificationPreferences preferences,
            boolean nextValue
    ) {
        if (!nextValue || Boolean.valueOf(nextValue).equals(preferences.getEmailEventReminders())) {
            return;
        }

        emailService.sendEventRemindersSubscriptionEmail(getUserEmailOrThrow(userId));
    }

    private void notifySmsOrderUpdatesIfEnabled(
            Long userId,
            NotificationPreferences preferences,
            boolean nextValue
    ) {
        if (!nextValue || Boolean.valueOf(nextValue).equals(preferences.getSmsOrderUpdates())) {
            return;
        }

        User user = getUserOrThrow(userId);
        String phone = getUserPhoneOrThrow(user);
        textBeeSmsService.sendSmsNotificationsEnabledMessage(phone);
    }

    private void notifySmsEventRemindersIfEnabled(
            Long userId,
            NotificationPreferences preferences,
            boolean nextValue
    ) {
        if (!nextValue || Boolean.valueOf(nextValue).equals(preferences.getSmsEventReminders())) {
            return;
        }

        User user = getUserOrThrow(userId);
        String phone = getUserPhoneOrThrow(user);
        textBeeSmsService.sendSmsEventRemindersEnabledMessage(phone);
    }

    private String getUserEmailOrThrow(Long userId) {
        return getUserOrThrow(userId).getEmail();
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private String getUserPhoneOrThrow(User user) {
        String phone = user.getPhone();
        if (phone == null || phone.trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Add and save a phone number before enabling SMS delivery."
            );
        }
        return phone;
    }
}
