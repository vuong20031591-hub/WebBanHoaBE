package com.florastore.web_ban_hoa;

import com.florastore.web_ban_hoa.entity.Role;
import com.florastore.web_ban_hoa.entity.User;
import com.florastore.web_ban_hoa.repository.NewsletterSubscriberRepository;
import com.florastore.web_ban_hoa.repository.NotificationPreferencesRepository;
import com.florastore.web_ban_hoa.repository.UserRepository;
import com.florastore.web_ban_hoa.service.TextBeeSmsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.mail.host=test.smtp.local",
        "app.mail.from=noreply@floralboutique.test"
})
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

    @MockitoBean
    private JavaMailSender mailSender;

    @MockitoBean
    private TextBeeSmsService textBeeSmsService;

    @BeforeEach
    void cleanDatabase() {
        notificationPreferencesRepository.deleteAll();
        newsletterSubscriberRepository.deleteAll();
        userRepository.deleteAll();
        reset(mailSender);
        reset(textBeeSmsService);
    }

    @AfterEach
    void resetMocks() {
        reset(mailSender);
        reset(textBeeSmsService);
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

        reset(mailSender);

        mockMvc.perform(put("/api/notification-preferences")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "emailPromotions": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailPromotions").value(true));

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));

        mockMvc.perform(get("/api/notification-preferences")
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailPromotions").value(true));
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

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));

        var subscriberAfterSubscribe = newsletterSubscriberRepository.findByEmail(user.getEmail()).orElseThrow();
        assertThat(subscriberAfterSubscribe.getIsActive()).isTrue();
        assertThat(subscriberAfterSubscribe.getUnsubscribedAt()).isNull();
        assertThat(subscriberAfterSubscribe.getSource()).isEqualTo("profile_notifications");

        reset(mailSender);

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
        user.setPhone("0355999141");
        user = userRepository.save(user);
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

        verify(textBeeSmsService, times(1)).sendSmsNotificationsEnabledMessage("0355999141");

        mockMvc.perform(get("/api/notification-preferences")
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.smsOrderUpdates").value(true))
                .andExpect(jsonPath("$.pushArtistUpdates").value(false));
    }

    @Test
    void enablingAllSupportedChannels_shouldSendFourEmailsAndTwoSms() throws Exception {
        User user = createUser("all.channels@example.com");
        user.setPhone("0355999141");
        user = userRepository.save(user);
        String authorization = jwtHelper.bearer(String.valueOf(user.getId()));

        mockMvc.perform(put("/api/notification-preferences")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "emailOrderUpdates": false,
                                  "emailPromotions": false,
                                  "emailNewsletter": false,
                                  "emailEventReminders": false,
                                  "smsOrderUpdates": false,
                                  "smsEventReminders": false
                                }
                                """))
                .andExpect(status().isOk());

        reset(mailSender);
        reset(textBeeSmsService);

        mockMvc.perform(put("/api/notification-preferences")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "emailOrderUpdates": true,
                                  "emailPromotions": true,
                                  "emailNewsletter": true,
                                  "emailEventReminders": true,
                                  "smsOrderUpdates": true,
                                  "smsEventReminders": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailOrderUpdates").value(true))
                .andExpect(jsonPath("$.emailPromotions").value(true))
                .andExpect(jsonPath("$.emailNewsletter").value(true))
                .andExpect(jsonPath("$.emailEventReminders").value(true))
                .andExpect(jsonPath("$.smsOrderUpdates").value(true))
                .andExpect(jsonPath("$.smsEventReminders").value(true));

        verify(mailSender, times(4)).send(any(SimpleMailMessage.class));
        verify(textBeeSmsService, times(1)).sendSmsNotificationsEnabledMessage("0355999141");
        verify(textBeeSmsService, times(1)).sendSmsEventRemindersEnabledMessage("0355999141");
    }

    private User createUser(String email) {
        return userRepository.save(
                new User(email, "password", "Notification Tester", "0123456789", Role.USER)
        );
    }
}
