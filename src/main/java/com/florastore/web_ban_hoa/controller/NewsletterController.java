package com.florastore.web_ban_hoa.controller;

import com.florastore.web_ban_hoa.dto.NewsletterSubscribeRequest;
import com.florastore.web_ban_hoa.service.NewsletterService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/newsletter")
public class NewsletterController {

    private final NewsletterService newsletterService;

    public NewsletterController(NewsletterService newsletterService) {
        this.newsletterService = newsletterService;
    }

    @PostMapping("/subscribe")
    public ResponseEntity<Void> subscribe(@Valid @RequestBody NewsletterSubscribeRequest request) {
        newsletterService.subscribe(request.email(), "bloom_club");
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<Void> unsubscribe(@Valid @RequestBody NewsletterSubscribeRequest request) {
        newsletterService.unsubscribe(request.email());
        return ResponseEntity.noContent().build();
    }
}
