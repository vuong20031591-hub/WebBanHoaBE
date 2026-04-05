package com.florastore.web_ban_hoa.service;

import com.florastore.web_ban_hoa.dto.TwoFactorSmsCodeResponse;
import com.florastore.web_ban_hoa.dto.UpdateUserPreferencesRequest;
import com.florastore.web_ban_hoa.dto.UserPreferencesResponse;
import com.florastore.web_ban_hoa.dto.VerifyTwoFactorSmsCodeRequest;
import com.florastore.web_ban_hoa.entity.SmsVerificationToken;
import com.florastore.web_ban_hoa.entity.UserPreferences;
import com.florastore.web_ban_hoa.entity.User;
import com.florastore.web_ban_hoa.repository.SmsVerificationTokenRepository;
import com.florastore.web_ban_hoa.repository.UserRepository;
import com.florastore.web_ban_hoa.repository.UserPreferencesRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class UserPreferencesService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String PURPOSE_SMS_TWO_FACTOR_ENABLE = "SMS_TWO_FACTOR_ENABLE";

    private final UserPreferencesRepository repository;
    private final UserRepository userRepository;
    private final SmsVerificationTokenRepository smsVerificationTokenRepository;
    private final TextBeeSmsService textBeeSmsService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final int otpExpiresInSeconds;
    private final int otpMaxAttempts;

    public UserPreferencesService(
            UserPreferencesRepository repository,
            UserRepository userRepository,
            SmsVerificationTokenRepository smsVerificationTokenRepository,
            TextBeeSmsService textBeeSmsService,
            @Value("${sms.otp.expires-in-seconds:300}") int otpExpiresInSeconds,
            @Value("${sms.otp.max-attempts:3}") int otpMaxAttempts
    ) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.smsVerificationTokenRepository = smsVerificationTokenRepository;
        this.textBeeSmsService = textBeeSmsService;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.otpExpiresInSeconds = otpExpiresInSeconds;
        this.otpMaxAttempts = otpMaxAttempts;
    }

    public UserPreferencesResponse getPreferences(String userId) {
        Long userIdLong = parseUserId(userId);
        UserPreferences prefs = repository.findByUserId(userIdLong)
                .orElseGet(() -> createDefaultPreferences(userIdLong));
        return UserPreferencesResponse.from(prefs);
    }

    @Transactional
    public UserPreferencesResponse updatePreferences(String userId, UpdateUserPreferencesRequest request) {
        Long userIdLong = parseUserId(userId);
        UserPreferences prefs = repository.findByUserId(userIdLong)
                .orElseGet(() -> createDefaultPreferences(userIdLong));

        if (request.language() != null && !request.language().isBlank()) {
            prefs.setLanguage(request.language());
        }
        if (request.currency() != null && !request.currency().isBlank()) {
            prefs.setCurrency(request.currency());
        }
        if (request.theme() != null && !request.theme().isBlank()) {
            prefs.setTheme(request.theme());
        }
        if (request.timezone() != null && !request.timezone().isBlank()) {
            prefs.setTimezone(request.timezone());
        }
        if (request.signatureWrap() != null) {
            prefs.setSignatureWrap(request.signatureWrap());
        }
        if (request.ecoDelivery() != null) {
            prefs.setEcoDelivery(request.ecoDelivery());
        }
        if (request.smsTwoFactorEnabled() != null) {
            if (request.smsTwoFactorEnabled() && !Boolean.TRUE.equals(prefs.getSmsTwoFactorEnabled())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "SMS 2FA can only be enabled after verifying the OTP code."
                );
            }

            prefs.setSmsTwoFactorEnabled(request.smsTwoFactorEnabled());
            if (!request.smsTwoFactorEnabled()) {
                invalidateActiveTwoFactorTokens(userIdLong, LocalDateTime.now());
            }
        }

        UserPreferences saved = repository.save(prefs);
        return UserPreferencesResponse.from(saved);
    }

    @Transactional
    public TwoFactorSmsCodeResponse requestSmsTwoFactorCode(String userId) {
        Long userIdLong = parseUserId(userId);
        User user = userRepository.findById(userIdLong)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String phone = validateTwoFactorPhone(user.getPhone());
        LocalDateTime now = LocalDateTime.now();
        invalidateActiveTwoFactorTokens(userIdLong, now);

        String code = generateCode();
        SmsVerificationToken token = new SmsVerificationToken(
                userIdLong,
                phone,
                PURPOSE_SMS_TWO_FACTOR_ENABLE,
                passwordEncoder.encode(code),
                now.plusSeconds(otpExpiresInSeconds),
                otpMaxAttempts
        );
        smsVerificationTokenRepository.save(token);

        textBeeSmsService.sendTwoFactorCode(phone, code);

        return new TwoFactorSmsCodeResponse(
                "sms",
                "Verification code sent to your saved phone number.",
                maskPhone(phone)
        );
    }

    @Transactional
    public UserPreferencesResponse verifySmsTwoFactorCode(String userId, VerifyTwoFactorSmsCodeRequest request) {
        Long userIdLong = parseUserId(userId);
        LocalDateTime now = LocalDateTime.now();

        SmsVerificationToken token = smsVerificationTokenRepository
                .findFirstByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(userIdLong, PURPOSE_SMS_TWO_FACTOR_ENABLE)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "No active SMS verification code. Please request a new code."
                ));

        if (token.isExpired(now)) {
            token.markUsed(now);
            smsVerificationTokenRepository.save(token);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification code is invalid or expired");
        }

        if (token.getAttempts() >= token.getMaxAttempts()) {
            token.markUsed(now);
            smsVerificationTokenRepository.save(token);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification code is invalid or expired");
        }

        if (!passwordEncoder.matches(request.code(), token.getCodeHash())) {
            token.incrementAttempts();
            if (token.getAttempts() >= token.getMaxAttempts()) {
                token.markUsed(now);
            }
            smsVerificationTokenRepository.save(token);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification code is invalid or expired");
        }

        token.markUsed(now);
        smsVerificationTokenRepository.save(token);

        UserPreferences prefs = repository.findByUserId(userIdLong)
                .orElseGet(() -> createDefaultPreferences(userIdLong));
        prefs.setSmsTwoFactorEnabled(true);

        UserPreferences saved = repository.save(prefs);
        return UserPreferencesResponse.from(saved);
    }

    @Transactional
    protected UserPreferences createDefaultPreferences(Long userId) {
        UserPreferences prefs = new UserPreferences(userId);
        return repository.save(prefs);
    }

    private void invalidateActiveTwoFactorTokens(Long userId, LocalDateTime now) {
        List<SmsVerificationToken> tokens = smsVerificationTokenRepository
                .findByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(userId, PURPOSE_SMS_TWO_FACTOR_ENABLE);

        for (SmsVerificationToken token : tokens) {
            token.markUsed(now);
        }

        if (!tokens.isEmpty()) {
            smsVerificationTokenRepository.saveAll(tokens);
        }
    }

    private String validateTwoFactorPhone(String phone) {
        String normalized = phone == null ? "" : phone.trim();
        if (normalized.isBlank() || "0000000000".equals(normalized)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Add and save a valid phone number before enabling SMS 2FA."
            );
        }

        return normalized;
    }

    private static Long parseUserId(String userId) {
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid user ID format");
        }
    }

    private static String generateCode() {
        int value = SECURE_RANDOM.nextInt(1_000_000);
        return String.format(Locale.ROOT, "%06d", value);
    }

    private static String maskPhone(String phone) {
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() <= 4) {
            return digits;
        }

        return "*".repeat(digits.length() - 4) + digits.substring(digits.length() - 4);
    }
}
