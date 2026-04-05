package com.florastore.web_ban_hoa.service;

import com.florastore.web_ban_hoa.entity.NewsletterSubscriber;
import com.florastore.web_ban_hoa.repository.NewsletterSubscriberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class NewsletterService {

    private final NewsletterSubscriberRepository repository;
    private final EmailService emailService;

    public NewsletterService(NewsletterSubscriberRepository repository, EmailService emailService) {
        this.repository = repository;
        this.emailService = emailService;
    }

    @Transactional
    public void subscribe(String email, String source) {
        activateSubscription(email, source);
        
        try {
            emailService.sendZaloGroupInvitation(email);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to send welcome email");
        }
    }

    @Transactional
    public void unsubscribe(String email) {
        NewsletterSubscriber subscriber = repository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Email not found"));
        
        subscriber.setIsActive(false);
        subscriber.setUnsubscribedAt(LocalDateTime.now());
        repository.save(subscriber);
    }

    @Transactional
    public void syncSubscriptionPreference(String email, boolean subscribe, String source) {
        if (subscribe) {
            activateSubscription(email, source);
            return;
        }

        repository.findByEmail(email).ifPresent(subscriber -> {
            subscriber.setIsActive(false);
            subscriber.setUnsubscribedAt(LocalDateTime.now());
            repository.save(subscriber);
        });
    }

    private void activateSubscription(String email, String source) {
        var existing = repository.findByEmail(email);

        if (existing.isPresent()) {
            NewsletterSubscriber subscriber = existing.get();
            if (!subscriber.getIsActive()) {
                subscriber.setIsActive(true);
                subscriber.setUnsubscribedAt(null);
            }
            subscriber.setSource(source);
            repository.save(subscriber);
            return;
        }

        NewsletterSubscriber subscriber = new NewsletterSubscriber(email, source);
        repository.save(subscriber);
    }
}
