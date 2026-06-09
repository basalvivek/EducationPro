package com.educationpro.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String fromAddress;

    public void sendResetEmail(String toEmail, String token) {
        String link = baseUrl + "/auth/reset-password?token=" + token;
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromAddress);
        msg.setTo(toEmail);
        msg.setSubject("EducationPro — Password Reset");
        msg.setText("Click the link below to reset your password (expires in 30 minutes):\n\n" + link
                + "\n\nIf you did not request this, ignore this email.");
        try {
            mailSender.send(msg);
            log.info("Password reset email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send reset email to {}: {}", toEmail, e.getMessage());
        }
    }
}
