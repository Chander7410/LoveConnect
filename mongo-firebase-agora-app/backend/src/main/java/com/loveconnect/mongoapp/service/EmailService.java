package com.loveconnect.mongoapp.service;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String BREVO_SEND_EMAIL_URL = "https://api.brevo.com/v3/smtp/email";

    private final JavaMailSender mailSender;
    private final RestTemplate restTemplate = new RestTemplate();
    private final String fromEmail;
    private final String smtpUsername;
    private final String smtpPassword;
    private final String brevoApiKey;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.smtp.from-email:}") String fromEmail,
                        @Value("${spring.mail.username:}") String smtpUsername,
                        @Value("${spring.mail.password:}") String smtpPassword,
                        @Value("${app.brevo.api-key:}") String brevoApiKey) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
        this.smtpUsername = smtpUsername;
        this.smtpPassword = smtpPassword;
        this.brevoApiKey = brevoApiKey;
    }

    public void sendOtp(String to, String otp) {
        if (!StringUtils.hasText(fromEmail)) {
            throw new IllegalArgumentException("Email sender is not configured");
        }
        if (StringUtils.hasText(brevoApiKey)) {
            sendWithBrevoApi(to, otp);
            return;
        }
        if (!StringUtils.hasText(smtpUsername) || !StringUtils.hasText(smtpPassword)) {
            throw new IllegalArgumentException("SMTP is not configured");
        }
        sendWithSmtp(to, otp);
    }

    private void sendWithSmtp(String to, String otp) {
        var message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("LoveConnect Verification Code");
        message.setText(otpBody(otp));
        try {
            mailSender.send(message);
        } catch (MailException ex) {
            log.warn("LoveConnect OTP email send failed. from={}, to={}, errorType={}, error={}",
                    fromEmail, to, ex.getClass().getSimpleName(), sanitizeMailError(ex));
            throw new IllegalArgumentException("Could not send OTP email. Please check SMTP settings.");
        }
    }

    private void sendWithBrevoApi(String to, String otp) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", brevoApiKey);
        var body = Map.of(
            "sender", Map.of("name", "LoveConnect", "email", fromEmail),
            "to", List.of(Map.of("email", to)),
            "subject", "LoveConnect Verification Code",
            "textContent", otpBody(otp)
        );
        try {
            restTemplate.postForObject(BREVO_SEND_EMAIL_URL, new HttpEntity<>(body, headers), String.class);
        } catch (RestClientException ex) {
            log.warn("LoveConnect Brevo API OTP email send failed. from={}, to={}, errorType={}, error={}",
                    fromEmail, to, ex.getClass().getSimpleName(), sanitizeMailError(ex));
            throw new IllegalArgumentException("Could not send OTP email. Please check Brevo settings.");
        }
    }

    private String otpBody(String otp) {
        return """
            Hello,

            Your LoveConnect OTP is: %s

            This OTP is valid for 5 minutes.

            Do not share this code with anyone.

            Thanks,
            LoveConnect Team
            """.formatted(otp);
    }

    private String sanitizeMailError(Exception ex) {
        var message = ex.getMessage();
        if (!StringUtils.hasText(message)) {
            return "No provider message";
        }
        if (StringUtils.hasText(brevoApiKey)) {
            message = message.replace(brevoApiKey, "[brevo-api-key]");
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
