package com.florastore.web_ban_hoa.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_preferences")
public class UserPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false, length = 10)
    private String language = "vi";

    @Column(nullable = false, length = 10)
    private String currency = "VND";

    @Column(nullable = false, length = 20)
    private String theme = "light";

    @Column(nullable = false, length = 50)
    private String timezone = "Asia/Ho_Chi_Minh";

    @Column(name = "signature_wrap", nullable = false)
    private Boolean signatureWrap = true;

    @Column(name = "eco_delivery", nullable = false)
    private Boolean ecoDelivery = false;

    @Column(name = "sms_two_factor_enabled", nullable = false)
    private Boolean smsTwoFactorEnabled = false;

    @Column(name = "ribbon_color", nullable = false, length = 20)
    private String ribbonColor = "blush";

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public UserPreferences() {
    }

    public UserPreferences(Long userId) {
        this.userId = userId;
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

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Boolean getSignatureWrap() {
        return signatureWrap;
    }

    public void setSignatureWrap(Boolean signatureWrap) {
        this.signatureWrap = signatureWrap;
    }

    public Boolean getEcoDelivery() {
        return ecoDelivery;
    }

    public void setEcoDelivery(Boolean ecoDelivery) {
        this.ecoDelivery = ecoDelivery;
    }

    public Boolean getSmsTwoFactorEnabled() {
        return smsTwoFactorEnabled;
    }

    public void setSmsTwoFactorEnabled(Boolean smsTwoFactorEnabled) {
        this.smsTwoFactorEnabled = smsTwoFactorEnabled;
    }

    public String getRibbonColor() {
        return ribbonColor;
    }

    public void setRibbonColor(String ribbonColor) {
        this.ribbonColor = ribbonColor;
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
