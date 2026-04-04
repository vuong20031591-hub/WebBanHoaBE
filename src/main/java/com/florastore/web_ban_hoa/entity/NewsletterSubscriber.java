package com.florastore.web_ban_hoa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "newsletter_subscribers")
@Getter
@Setter
@NoArgsConstructor
public class NewsletterSubscriber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private LocalDateTime subscribedAt;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column
    private LocalDateTime unsubscribedAt;

    @Column(length = 50)
    private String source = "bloom_club";

    public NewsletterSubscriber(String email, String source) {
        this.email = email;
        this.source = source;
        this.isActive = true;
    }

    @PrePersist
    public void prePersist() {
        if (this.subscribedAt == null) {
            this.subscribedAt = LocalDateTime.now();
        }
    }
}
