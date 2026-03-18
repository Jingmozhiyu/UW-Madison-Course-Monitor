package com.jing.monitor.service;

import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Email notification service for enrollment state alerts.
 */
@Service
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    /**
     * Creates mail service with Spring-managed sender.
     *
     * @param mailSender JavaMail sender bean
     */
    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends an email alert when a section becomes OPEN.
     *
     * @param recipientEmail recipient mailbox
     * @param section section id
     * @param courseInfo course display text
     */
    public void sendCourseOpenAlert(String recipientEmail, String section, String courseInfo) {
        log.info("[Mail] Preparing to send OPEN alert for section {} to {}", section, recipientEmail);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(requireRecipientEmail(recipientEmail));
            message.setSubject("🔥 Alert: Section " + section + " IS OPEN! 🔥");
            message.setText("Go to Enroll!\n\nCourse Info: " + courseInfo + "\n\n(This email is sent automatically by UW-Course-Monitor)");

            mailSender.send(message);
            log.info("[Mail] OPEN alert email sent successfully for section {} to {}", section, recipientEmail);
        } catch (Exception e) {
            log.error("[Mail] Failed to send OPEN alert email for section {} to {}", section, recipientEmail, e);
        }
    }

    /**
     * Sends an email alert when a section becomes WAITLISTED.
     *
     * @param recipientEmail recipient mailbox
     * @param section section id
     * @param courseInfo course display text
     */
    public void sendCourseWaitlistedAlert(String recipientEmail, String section, String courseInfo) {
        log.info("[Mail] Preparing to send WAITLIST alert for section {} to {}", section, recipientEmail);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(requireRecipientEmail(recipientEmail));
            message.setSubject("🔥 ALERT: Section " + section + " HAS WAITLIST SEATS! 🔥");
            message.setText("Go to Enroll!\n\nCourse Info: " + courseInfo + "\n\n(This email is sent automatically by UW-Course-Monitor)");

            mailSender.send(message);
            log.info("[Mail] WAITLIST alert email sent successfully for section {} to {}", section, recipientEmail);
        } catch (Exception e) {
            log.error("[Mail] Failed to send WAITLIST alert email for section {} to {}", section, recipientEmail, e);
        }
    }

    private String requireRecipientEmail(String recipientEmail) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new IllegalArgumentException("Recipient email is required.");
        }
        return Objects.requireNonNull(recipientEmail).trim().toLowerCase();
    }
}
