package com.florastore.web_ban_hoa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
@NoArgsConstructor
public class NotificationPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "email_order_updates", nullable = false)
    private Boolean emailOrderUpdates = true;

    @Column(name = "email_promotions", nullable = false)
    private Boolean emailPromotions = true;

    @Column(name = "email_newsletter", nullable = false)
    private Boolean emailNewsletter = false;

    @Column(name = "email_event_reminders", nullable = false)
    private Boolean emailEventReminders = false;

    @Column(name = "sms_order_updates", nullable = false)
    private Boolean smsOrderUpdates = false;

    @Column(name = "sms_event_reminders", nullable = false)
    private Boolean smsEventReminders = false;

    @Column(name = "push_artist_updates", nullable = false)
    private Boolean pushArtistUpdates = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public NotificationPreferences(Long userId) {
        this.userId = userId;
        this.emailOrderUpdates = true;
        this.emailPromotions = true;
        this.emailNewsletter = false;
        this.emailEventReminders = false;
        this.smsOrderUpdates = false;
        this.smsEventReminders = false;
        this.pushArtistUpdates = false;
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
