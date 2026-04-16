package com.florastore.web_ban_hoa.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_preferences")
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

    public NotificationPreferences() {
    }

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Boolean getEmailOrderUpdates() {
        return emailOrderUpdates;
    }

    public void setEmailOrderUpdates(Boolean emailOrderUpdates) {
        this.emailOrderUpdates = emailOrderUpdates;
    }

    public Boolean getEmailPromotions() {
        return emailPromotions;
    }

    public void setEmailPromotions(Boolean emailPromotions) {
        this.emailPromotions = emailPromotions;
    }

    public Boolean getEmailNewsletter() {
        return emailNewsletter;
    }

    public void setEmailNewsletter(Boolean emailNewsletter) {
        this.emailNewsletter = emailNewsletter;
    }

    public Boolean getEmailEventReminders() {
        return emailEventReminders;
    }

    public void setEmailEventReminders(Boolean emailEventReminders) {
        this.emailEventReminders = emailEventReminders;
    }

    public Boolean getSmsOrderUpdates() {
        return smsOrderUpdates;
    }

    public void setSmsOrderUpdates(Boolean smsOrderUpdates) {
        this.smsOrderUpdates = smsOrderUpdates;
    }

    public Boolean getSmsEventReminders() {
        return smsEventReminders;
    }

    public void setSmsEventReminders(Boolean smsEventReminders) {
        this.smsEventReminders = smsEventReminders;
    }

    public Boolean getPushArtistUpdates() {
        return pushArtistUpdates;
    }

    public void setPushArtistUpdates(Boolean pushArtistUpdates) {
        this.pushArtistUpdates = pushArtistUpdates;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
