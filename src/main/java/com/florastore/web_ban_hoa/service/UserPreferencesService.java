package com.florastore.web_ban_hoa.service;

import com.florastore.web_ban_hoa.dto.UpdateUserPreferencesRequest;
import com.florastore.web_ban_hoa.dto.UserPreferencesResponse;
import com.florastore.web_ban_hoa.entity.UserPreferences;
import com.florastore.web_ban_hoa.repository.UserPreferencesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserPreferencesService {

    private final UserPreferencesRepository repository;

    public UserPreferencesService(UserPreferencesRepository repository) {
        this.repository = repository;
    }

    public UserPreferencesResponse getPreferences(String userId) {
        Long userIdLong = Long.parseLong(userId);
        UserPreferences prefs = repository.findByUserId(userIdLong)
                .orElseGet(() -> createDefaultPreferences(userIdLong));
        return UserPreferencesResponse.from(prefs);
    }

    @Transactional
    public UserPreferencesResponse updatePreferences(String userId, UpdateUserPreferencesRequest request) {
        Long userIdLong = Long.parseLong(userId);
        UserPreferences prefs = repository.findByUserId(userIdLong)
                .orElseGet(() -> createDefaultPreferences(userIdLong));

        if (request.language() != null && !request.language().isBlank()) {
            prefs.setLanguage(request.language());
        }
        if (request.currency() != null && !request.currency().isBlank()) {
            prefs.setCurrency(request.currency());
        }
        if (request.theme() != null && !request.theme().isBlank()) {
            prefs.setTheme(request.theme());
        }
        if (request.timezone() != null && !request.timezone().isBlank()) {
            prefs.setTimezone(request.timezone());
        }
        if (request.signatureWrap() != null) {
            prefs.setSignatureWrap(request.signatureWrap());
        }
        if (request.ecoDelivery() != null) {
            prefs.setEcoDelivery(request.ecoDelivery());
        }

        UserPreferences saved = repository.save(prefs);
        return UserPreferencesResponse.from(saved);
    }

    @Transactional
    protected UserPreferences createDefaultPreferences(Long userId) {
        UserPreferences prefs = new UserPreferences(userId);
        return repository.save(prefs);
    }
}
