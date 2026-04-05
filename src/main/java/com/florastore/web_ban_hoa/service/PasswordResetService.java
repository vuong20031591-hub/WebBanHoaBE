package com.florastore.web_ban_hoa.service;

import com.florastore.web_ban_hoa.dto.ForgotPasswordResponse;
import com.florastore.web_ban_hoa.entity.PasswordResetToken;
import com.florastore.web_ban_hoa.entity.User;
import com.florastore.web_ban_hoa.repository.PasswordResetTokenRepository;
import com.florastore.web_ban_hoa.repository.UserRepository;
import com.florastore.web_ban_hoa.validation.AuthValidationRules;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class PasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);
    private static final String PASSWORD_RESET_SENT_MESSAGE =
            "If the email exists, a verification code has been prepared.";
    private static final String EMAIL_NOT_FOUND_MESSAGE = "Email này không tồn tại.";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final JavaMailSender mailSender;
    private final BCryptPasswordEncoder passwordEncoder;
    private final String mailFrom;
    private final String mailFromName;
    private final int codeExpiryMinutes;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            JavaMailSender mailSender,
            @Value("${app.mail.from:}") String mailFrom,
            @Value("${app.mail.from-name:}") String mailFromName,
            @Value("${auth.password-reset.code-expiry-minutes:10}") int codeExpiryMinutes
    ) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.mailSender = mailSender;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.mailFrom = normalizeValue(mailFrom);
        this.mailFromName = normalizeValue(mailFromName);
        this.codeExpiryMinutes = codeExpiryMinutes;
    }

    @Transactional
    public ForgotPasswordResponse requestResetCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (user == null) {
            return new ForgotPasswordResponse("none", EMAIL_NOT_FOUND_MESSAGE, null);
        }

        invalidateActiveTokens(normalizedEmail, LocalDateTime.now());

        String code = generateCode();
        PasswordResetToken token = new PasswordResetToken(
                user,
                normalizedEmail,
                passwordEncoder.encode(code),
                LocalDateTime.now().plusMinutes(codeExpiryMinutes)
        );
        passwordResetTokenRepository.save(token);

        try {
            sendResetCodeEmail(normalizedEmail, code);
            return new ForgotPasswordResponse("email", PASSWORD_RESET_SENT_MESSAGE, null);
        } catch (ResponseStatusException ex) {
            logger.warn("Password reset email delivery failed for {}. Reason: {}",
                    normalizedEmail, ex.getReason());
            throw ex;
        }
    }

    @Transactional
    public void resetPasswordWithCode(String email, String code, String newPassword) {
        String normalizedEmail = normalizeEmail(email);
        LocalDateTime now = LocalDateTime.now();

        try {
            AuthValidationRules.validatePassword(newPassword);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }

        PasswordResetToken token = passwordResetTokenRepository
                .findFirstByEmailAndUsedFalseOrderByCreatedAtDesc(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification code is invalid or expired"));

        if (token.isExpired(now)) {
            token.markUsed(now);
            passwordResetTokenRepository.save(token);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification code is invalid or expired");
        }

        if (!passwordEncoder.matches(code, token.getCodeHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification code is invalid or expired");
        }

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        invalidateActiveTokens(normalizedEmail, now);
    }

    private void invalidateActiveTokens(String email, LocalDateTime now) {
        List<PasswordResetToken> tokens = passwordResetTokenRepository
                .findByEmailAndUsedFalseOrderByCreatedAtDesc(email);

        for (PasswordResetToken token : tokens) {
            token.markUsed(now);
        }

        if (!tokens.isEmpty()) {
            passwordResetTokenRepository.saveAll(tokens);
        }
    }

    private void sendResetCodeEmail(String email, String code) {
        try {
            sendResetCodeEmail(mailSender, email, code);
        } catch (MailAuthenticationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Gmail rejected SMTP login. Recreate the Gmail App Password for MAIL_USERNAME and paste it into MAIL_PASSWORD without spaces.",
                    ex
            );
        } catch (MailSendException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to send password reset email. Check Gmail SMTP host, port, and TLS settings.",
                    ex
            );
        } catch (MessagingException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to send password reset email. Check Gmail SMTP configuration.",
                    ex
            );
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to send password reset email. Check Gmail SMTP configuration.",
                    ex
            );
        }
    }

    private void sendResetCodeEmail(JavaMailSender sender, String email, String code) throws Exception {
        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

        if (!mailFrom.isBlank()) {
            if (!mailFromName.isBlank()) {
                helper.setFrom(mailFrom, mailFromName);
            } else {
                helper.setFrom(mailFrom);
            }
        }

        helper.setTo(email);
        helper.setSubject("Floral Boutique password reset code");
        helper.setText(buildHtmlBody(code), true);

        sender.send(message);
    }

    private static String normalizeValue(String value) {
        return value == null ? "" : value.trim();
    }

    private String buildHtmlBody(String code) {
        return """
                <div style="font-family:Arial,sans-serif;background:#f8f3ee;padding:32px;color:#3e342d;">
                  <div style="max-width:520px;margin:0 auto;background:#ffffff;border-radius:20px;padding:32px;border:1px solid #f0e3da;">
                    <p style="margin:0 0 8px;font-size:12px;letter-spacing:0.3em;text-transform:uppercase;color:#d0b59c;">Floral Boutique</p>
                    <h1 style="margin:0 0 16px;font-size:32px;font-weight:600;">Reset your password</h1>
                    <p style="margin:0 0 20px;font-size:15px;line-height:1.8;color:#7c6d64;">
                      Use the verification code below to reset your password. The code expires in %d minutes.
                    </p>
                    <div style="margin:24px 0;padding:18px 20px;border-radius:16px;background:#fff7f1;text-align:center;">
                      <span style="font-size:32px;letter-spacing:0.28em;font-weight:700;color:#8a6d5d;">%s</span>
                    </div>
                    <p style="margin:0;font-size:13px;line-height:1.7;color:#9b8a7f;">
                      If you did not request this change, you can ignore this email safely.
                    </p>
                  </div>
                </div>
                """.formatted(codeExpiryMinutes, code);
    }

    private static String generateCode() {
        int value = SECURE_RANDOM.nextInt(1_000_000);
        return String.format(Locale.ROOT, "%06d", value);
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
