package com.florastore.web_ban_hoa.controller;

import com.florastore.web_ban_hoa.dto.RewardsTransactionResponse;
import com.florastore.web_ban_hoa.dto.UserRewardsResponse;
import com.florastore.web_ban_hoa.security.JwtSubjectResolver;
import com.florastore.web_ban_hoa.service.RewardsService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rewards")
public class RewardsController {

    private final RewardsService rewardsService;
    private final JwtSubjectResolver jwtSubjectResolver;

    public RewardsController(RewardsService rewardsService, JwtSubjectResolver jwtSubjectResolver) {
        this.rewardsService = rewardsService;
        this.jwtSubjectResolver = jwtSubjectResolver;
    }

    @GetMapping
    public ResponseEntity<UserRewardsResponse> getUserRewards(
            @RequestHeader(name = "Authorization", required = false) String authorization) {
        String userId = jwtSubjectResolver.resolveUserId(authorization);
        UserRewardsResponse rewards = rewardsService.getUserRewards(userId);
        return ResponseEntity.ok(rewards);
    }

    @GetMapping("/history")
    public ResponseEntity<Page<RewardsTransactionResponse>> getTransactionHistory(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String userId = jwtSubjectResolver.resolveUserId(authorization);
        Page<RewardsTransactionResponse> history = rewardsService.getTransactionHistory(userId, page, size);
        return ResponseEntity.ok(history);
    }
}
