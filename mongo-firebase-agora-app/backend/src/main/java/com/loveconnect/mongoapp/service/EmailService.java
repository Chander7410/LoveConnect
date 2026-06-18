package com.loveconnect.mongoapp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmailService {
    private final JavaMailSender mailSender;
    private final String fromEmail;

    public EmailService(JavaMailSender mailSender, @Value("${app.smtp.from-email:}") String fromEmail) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
    }

    public void sendOtp(String to, String otp) {
        if (!StringUtils.hasText(fromEmail)) {
            throw new IllegalArgumentException("SMTP is not configured");
        }
        var message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("LoveConnect email verification OTP");
        message.setText("Your LoveConnect OTP is " + otp + ". It expires in 5 minutes.");
        try {
            mailSender.send(message);
        } catch (MailException ex) {
            throw new IllegalArgumentException("Could not send OTP email. Please check SMTP settings.");
        }
    }
}
