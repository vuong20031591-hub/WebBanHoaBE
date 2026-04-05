package com.florastore.web_ban_hoa.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@floralboutique.com}")
    private String fromEmail;

    @Value("${app.mail.from-name:Floral Boutique}")
    private String fromName;

    @Value("${spring.mail.host:}")
    private String mailHost;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendZaloGroupInvitation(String toEmail) {
        try {
            logger.info("Preparing to send Zalo invitation email to: {}", toEmail);
            logger.info("From email: {}", fromEmail);

            sendPlainTextEmail(
                    toEmail,
                    "Welcome to Floral Boutique Bloom Club!",
                    "Dear Valued Customer,\n\n" +
                            "Thank you for joining our Bloom Club!\n\n" +
                            "As a member, you'll receive:\n" +
                            "- Exclusive styling tips and seasonal updates\n" +
                            "- 10% off your first order\n" +
                            "- Early access to new collections\n\n" +
                            "Join our Zalo community to stay connected:\n" +
                            "https://zalo.me/g/rredrspr8yieer3us8wr\n\n" +
                            "We look forward to sharing our passion for flowers with you!\n\n" +
                            "Best regards,\n" +
                            "Floral Boutique Team"
            );

            logger.info("Email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send email to: {}. Error: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }

    public void sendSpecialOffersSubscriptionEmail(String toEmail) {
        sendPreferenceEmailBestEffort(
                toEmail,
                "You're subscribed to Floral Boutique special offers",
                "Hello,\n\n" +
                        "You have successfully subscribed to Special offers from Floral Boutique.\n\n" +
                        "From now on, we may email you about:\n" +
                        "- Seasonal promotions\n" +
                        "- Exclusive bouquet launches\n" +
                        "- Limited-time floral campaigns\n\n" +
                        "You can update your communication preferences anytime in your account settings.\n\n" +
                        "Warm regards,\n" +
                        fromName
        );
    }

    public void sendOrderUpdatesSubscriptionEmail(String toEmail) {
        sendPreferenceEmailBestEffort(
                toEmail,
                "Order update emails are enabled",
                "Hello,\n\n" +
                        "You have enabled order update emails from Floral Boutique.\n\n" +
                        "We'll email you important updates such as:\n" +
                        "- Order confirmation\n" +
                        "- Delivery progress\n" +
                        "- Delivery completion\n\n" +
                        "You can change this preference anytime in your notification settings.\n\n" +
                        "Warm regards,\n" +
                        fromName
        );
    }

    public void sendNewsletterSubscriptionEmail(String toEmail) {
        sendPreferenceEmailBestEffort(
                toEmail,
                "You're subscribed to the Floral Boutique newsletter",
                "Hello,\n\n" +
                        "You have successfully subscribed to the Floral Boutique newsletter.\n\n" +
                        "You'll now receive occasional emails featuring:\n" +
                        "- New collections\n" +
                        "- Gifting inspiration\n" +
                        "- Boutique stories and updates\n\n" +
                        "You can unsubscribe anytime from your profile settings.\n\n" +
                        "Warm regards,\n" +
                        fromName
        );
    }

    public void sendEventRemindersSubscriptionEmail(String toEmail) {
        sendPreferenceEmailBestEffort(
                toEmail,
                "Event reminder emails are enabled",
                "Hello,\n\n" +
                        "You have enabled event reminder emails from Floral Boutique.\n\n" +
                        "We'll send reminder emails for saved dates and important occasions.\n\n" +
                        "You can change this preference anytime in your notification settings.\n\n" +
                        "Warm regards,\n" +
                        fromName
        );
    }

    private void sendPreferenceEmailBestEffort(String toEmail, String subject, String body) {
        if (!isMailConfigured()) {
            logger.warn("Skipping email send to {} because mail configuration is incomplete", toEmail);
            return;
        }

        try {
            sendPlainTextEmail(toEmail, subject, body);
            logger.info("Preference email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            logger.warn("Failed to send preference email to {}: {}", toEmail, e.getMessage());
        }
    }

    private void sendPlainTextEmail(String toEmail, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    private boolean isMailConfigured() {
        return StringUtils.hasText(mailHost) && StringUtils.hasText(fromEmail);
    }
}
