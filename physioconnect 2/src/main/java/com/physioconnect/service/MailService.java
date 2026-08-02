package com.physioconnect.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Thin wrapper around JavaMailSender. Email delivery is intentionally
 * best-effort: a transient mail outage must not break core flows such as
 * user registration, so send failures are logged instead of thrown.
 */
@Slf4j
@Service
public class MailService {

    private final JavaMailSender mailSender;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void send(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to {} (subject: {})", to, subject);
        } catch (Exception ex) {
            log.error("Failed to send email to {} (subject: {}): {}", to, subject, ex.getMessage());
        }
    }
}
