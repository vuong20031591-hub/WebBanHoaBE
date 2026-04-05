package com.florastore.web_ban_hoa;

import com.florastore.web_ban_hoa.entity.Role;
import com.florastore.web_ban_hoa.entity.User;
import com.florastore.web_ban_hoa.repository.NewsletterSubscriberRepository;
import com.florastore.web_ban_hoa.repository.NotificationPreferencesRepository;
import com.florastore.web_ban_hoa.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class NotificationPreferencesWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestJwtHelper jwtHelper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationPreferencesRepository notificationPreferencesRepository;

    @Autowired
    private NewsletterSubscriberRepository newsletterSubscriberRepository;

    @BeforeEach
    void cleanDatabase() {
        notificationPreferencesRepository.deleteAll();
        newsletterSubscriberRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void seasonalCurationsToggle_shouldControlEmailPromotionsChannel() throws Exception {
        User user = createUser("seasonal.test@example.com");
        String authorization = jwtHelper.bearer(String.valueOf(user.getId()));

        mockMvc.perform(put("/api/notification-preferences")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "emailPromotions": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailPromotions").value(false));

        mockMvc.perform(get("/api/notification-preferences")
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailPromotions").value(false));
    }

    @Test
    void boutiqueNewsToggle_shouldSynchronizeNewsletterSubscribeAndUnsubscribe() throws Exception {
        User user = createUser("boutique.news@example.com");
        String authorization = jwtHelper.bearer(String.valueOf(user.getId()));

        mockMvc.perform(put("/api/notification-preferences")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "emailNewsletter": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailNewsletter").value(true));

        var subscriberAfterSubscribe = newsletterSubscriberRepository.findByEmail(user.getEmail()).orElseThrow();
        assertThat(subscriberAfterSubscribe.getIsActive()).isTrue();
        assertThat(subscriberAfterSubscribe.getUnsubscribedAt()).isNull();
        assertThat(subscriberAfterSubscribe.getSource()).isEqualTo("profile_notifications");

        mockMvc.perform(put("/api/notification-preferences")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "emailNewsletter": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailNewsletter").value(false));

        var subscriberAfterUnsubscribe = newsletterSubscriberRepository.findByEmail(user.getEmail()).orElseThrow();
        assertThat(subscriberAfterUnsubscribe.getIsActive()).isFalse();
        assertThat(subscriberAfterUnsubscribe.getUnsubscribedAt()).isNotNull();
    }

    @Test
    void deliveryAlertsToggle_shouldControlSmsChannelOnly() throws Exception {
        User user = createUser("delivery.alerts@example.com");
        String authorization = jwtHelper.bearer(String.valueOf(user.getId()));

        mockMvc.perform(put("/api/notification-preferences")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "smsOrderUpdates": true,
                                  "pushArtistUpdates": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.smsOrderUpdates").value(true))
                .andExpect(jsonPath("$.pushArtistUpdates").value(false));

        mockMvc.perform(get("/api/notification-preferences")
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.smsOrderUpdates").value(true))
                .andExpect(jsonPath("$.pushArtistUpdates").value(false));
    }

    private User createUser(String email) {
        return userRepository.save(
                new User(email, "password", "Notification Tester", "0123456789", Role.USER)
        );
    }
}
