package com.jing.monitor.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
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
        System.out.println("[Mail] Preparing to send alert for section: " + section);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("🔥 Alert: Section " + section + " IS OPEN! 🔥");
            message.setText("Go to Enroll!\n\nCourse Info: " + courseInfo + "\n\n(This email is sent automatically by UW-Course-Monitor)");

            mailSender.send(message);
            System.out.println("[Mail] Email sent successfully!");
        } catch (Exception e) {
            System.err.println("[Mail] Failed to send email: " + e.getMessage());
        }
    }

    public void sendCourseWaitlistedAlert(String section, String courseInfo) {
        System.out.println("[Mail] Preparing to send alert for section: " + section);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("🔥 ALERT: Section " + section + " HAS WAITLIST SEATS! 🔥");
            message.setText("Go to Enroll!\n\nCourse Info: " + courseInfo + "\n\n(This email is sent automatically by UW-Course-Monitor)");

            mailSender.send(message);
            System.out.println("[Mail] Email sent successfully!");
        } catch (Exception e) {
            System.err.println("[Mail] Failed to send email: " + e.getMessage());
        }
    }
}