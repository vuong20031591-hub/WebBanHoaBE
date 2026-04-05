package com.florastore.web_ban_hoa.service;

import com.florastore.web_ban_hoa.dto.EventInquiryRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EventInquiryService {

    private final EmailService emailService;

    public EventInquiryService(EmailService emailService) {
        this.emailService = emailService;
    }

    public void submitInquiry(EventInquiryRequest request) {
        try {
            emailService.sendEventInquiryNotification(
                    request.fullName().trim(),
                    request.email().trim(),
                    request.eventType().trim(),
                    request.eventDate(),
                    request.vision().trim()
            );
        } catch (ResponseStatusException statusException) {
            throw statusException;
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to send inquiry right now"
            );
        }
    }
}
