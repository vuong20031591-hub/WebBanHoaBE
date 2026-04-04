package com.florastore.web_ban_hoa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_preferences")
@Getter
@Setter
@NoArgsConstructor
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

    @Column(name = "ribbon_color", nullable = false, length = 20)
    private String ribbonColor = "blush";

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

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
}
