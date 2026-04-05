package com.florastore.web_ban_hoa.controller;

import com.florastore.web_ban_hoa.dto.EventInquiryRequest;
import com.florastore.web_ban_hoa.service.EventInquiryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inquiries")
public class InquiryController {

    private final EventInquiryService eventInquiryService;

    public InquiryController(EventInquiryService eventInquiryService) {
        this.eventInquiryService = eventInquiryService;
    }

    @PostMapping("/events")
    public ResponseEntity<Void> submitEventInquiry(@Valid @RequestBody EventInquiryRequest request) {
        eventInquiryService.submitInquiry(request);
        return ResponseEntity.accepted().build();
    }
}
