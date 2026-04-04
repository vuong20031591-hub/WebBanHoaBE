package com.florastore.web_ban_hoa.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    private final JavaMailSender mailSender;
    
    @Value("${app.mail.from:noreply@floralboutique.com}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendZaloGroupInvitation(String toEmail) {
        try {
            logger.info("Preparing to send Zalo invitation email to: {}", toEmail);
            logger.info("From email: {}", fromEmail);
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Welcome to Floral Boutique Bloom Club! 🌸");
            message.setText(
                "Dear Valued Customer,\n\n" +
                "Thank you for joining our Bloom Club!\n\n" +
                "As a member, you'll receive:\n" +
                "• Exclusive styling tips and seasonal updates\n" +
                "• 10% off your first order\n" +
                "• Early access to new collections\n\n" +
                "Join our Zalo community to stay connected:\n" +
                "https://zalo.me/g/rredrspr8yieer3us8wr\n\n" +
                "We look forward to sharing our passion for flowers with you!\n\n" +
                "Best regards,\n" +
                "Floral Boutique Team"
            );
            
            mailSender.send(message);
            logger.info("Email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send email to: {}. Error: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }
}
