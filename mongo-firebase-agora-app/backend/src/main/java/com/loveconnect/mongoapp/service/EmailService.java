package com.loveconnect.mongoapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final String smtpUsername;
    private final String smtpPassword;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.smtp.from-email:}") String fromEmail,
                        @Value("${spring.mail.username:}") String smtpUsername,
                        @Value("${spring.mail.password:}") String smtpPassword) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
        this.smtpUsername = smtpUsername;
        this.smtpPassword = smtpPassword;
    }

    public void sendOtp(String to, String otp) {
        if (!StringUtils.hasText(fromEmail) || !StringUtils.hasText(smtpUsername) || !StringUtils.hasText(smtpPassword)) {
            throw new IllegalArgumentException("SMTP is not configured");
        }
        var message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("LoveConnect Verification Code");
        message.setText("""
            Hello,

            Your LoveConnect OTP is: %s

            This OTP is valid for 5 minutes.

            Do not share this code with anyone.

            Thanks,
            LoveConnect Team
            """.formatted(otp));
        try {
            mailSender.send(message);
        } catch (MailException ex) {
            log.warn("LoveConnect OTP email send failed. from={}, to={}, errorType={}, error={}",
                    fromEmail, to, ex.getClass().getSimpleName(), sanitizeMailError(ex));
            throw new IllegalArgumentException("Could not send OTP email. Please check SMTP settings.");
        }
    }

    private String sanitizeMailError(Exception ex) {
        var message = ex.getMessage();
        if (!StringUtils.hasText(message)) {
            return "No provider message";
        }
        if (StringUtils.hasText(smtpPassword)) {
            message = message.replace(smtpPassword, "[redacted]");
        }
        if (StringUtils.hasText(smtpUsername)) {
            message = message.replace(smtpUsername, "[smtp-username]");
        }
        return message;
    }
}
