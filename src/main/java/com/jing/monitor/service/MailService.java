package com.jing.monitor.service;

import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.to}")
    private String toEmail;

    // Construct function injects JavaMailSender
    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sending text mail
     */
    public void sendCourseOpenAlert(String section, String courseInfo) {
        log.info("[Mail] Preparing to send OPEN alert for section: {}", section);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("🔥 Alert: Section " + section + " IS OPEN! 🔥");
            message.setText("Go to Enroll!\n\nCourse Info: " + courseInfo + "\n\n(This email is sent automatically by UW-Course-Monitor)");

            mailSender.send(message);
            log.info("[Mail] OPEN alert email sent successfully for section: {}", section);
        } catch (Exception e) {
            log.error("[Mail] Failed to send OPEN alert email for section: {}", section, e);
        }
    }

    public void sendCourseWaitlistedAlert(String section, String courseInfo) {
        log.info("[Mail] Preparing to send WAITLIST alert for section: {}", section);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("🔥 ALERT: Section " + section + " HAS WAITLIST SEATS! 🔥");
            message.setText("Go to Enroll!\n\nCourse Info: " + courseInfo + "\n\n(This email is sent automatically by UW-Course-Monitor)");

            mailSender.send(message);
            log.info("[Mail] WAITLIST alert email sent successfully for section: {}", section);
        } catch (Exception e) {
            log.error("[Mail] Failed to send WAITLIST alert email for section: {}", section, e);
        }
    }
}
