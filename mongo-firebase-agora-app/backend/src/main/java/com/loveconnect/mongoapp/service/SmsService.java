package com.loveconnect.mongoapp.service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Service
public class SmsService {
    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final String provider;
    private final String apiKey;
    private final String senderId;
    private final String templateId;
    private final String fast2SmsRoute;

    public SmsService(
        @Value("${app.sms.provider:CONSOLE}") String provider,
        @Value("${app.sms.api-key:}") String apiKey,
        @Value("${app.sms.sender-id:}") String senderId,
        @Value("${app.sms.template-id:}") String templateId,
        @Value("${app.sms.fast2sms-route:OTP}") String fast2SmsRoute
    ) {
        this.provider = provider == null ? "CONSOLE" : provider.trim().toUpperCase(Locale.US);
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.senderId = senderId == null ? "" : senderId.trim();
        this.templateId = templateId == null ? "" : templateId.trim();
        this.fast2SmsRoute = fast2SmsRoute == null ? "OTP" : fast2SmsRoute.trim().toUpperCase(Locale.US);
    }

    public void sendOtp(String mobileNumber, String otp, String purpose) {
        if ("CONSOLE".equals(provider) || provider.isBlank()) {
            log.warn("LoveConnect {} mobile OTP for {} is {}. Configure SMS_PROVIDER for production.", purpose, mobileNumber, otp);
            return;
        }
        if (apiKey.isBlank()) {
            throw new IllegalArgumentException("SMS is not configured");
        }
        switch (provider) {
            case "2FACTOR" -> send2Factor(mobileNumber, otp);
            case "FAST2SMS" -> sendFast2Sms(mobileNumber, otp);
            case "MSG91" -> sendMsg91(mobileNumber, otp);
            default -> throw new IllegalArgumentException("Unsupported SMS provider: " + provider);
        }
    }

    private void send2Factor(String mobileNumber, String otp) {
        var template = templateId.isBlank() ? "OTP1" : templateId;
        var url = "https://2factor.in/API/V1/%s/SMS/%s/%s/%s".formatted(
            encode(apiKey), encode(mobileNumber), encode(otp), encode(template));
        restTemplate.getForObject(URI.create(url), String.class);
    }

    private void sendFast2Sms(String mobileNumber, String otp) {
        var headers = new HttpHeaders();
        headers.set("authorization", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        var body = "QUICK".equals(fast2SmsRoute)
            ? Map.of(
                "route", "q",
                "message", otp + " is your LoveConnect verification code. Do not share it.",
                "language", "english",
                "flash", "0",
                "numbers", fast2SmsNumber(mobileNumber)
            )
            : Map.of(
                "route", "otp",
                "variables_values", otp,
                "numbers", fast2SmsNumber(mobileNumber)
            );
        try {
            restTemplate.postForObject("https://www.fast2sms.com/dev/bulkV2", new HttpEntity<>(body, headers), String.class);
        } catch (RestClientResponseException ex) {
            throw new IllegalArgumentException("Fast2SMS error: " + ex.getResponseBodyAsString());
        }
    }

    private void sendMsg91(String mobileNumber, String otp) {
        if (templateId.isBlank()) {
            throw new IllegalArgumentException("SMS_TEMPLATE_ID is required for MSG91");
        }
        var headers = new HttpHeaders();
        headers.set("authkey", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        var body = Map.of(
            "template_id", templateId,
            "sender", senderId,
            "short_url", "0",
            "mobiles", "91" + mobileNumber,
            "otp", otp
        );
        restTemplate.postForObject("https://control.msg91.com/api/v5/otp", new HttpEntity<>(body, headers), String.class);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String fast2SmsNumber(String mobileNumber) {
        var digits = mobileNumber == null ? "" : mobileNumber.replaceAll("[^0-9]", "");
        if (digits.startsWith("91") && digits.length() == 12) {
            return digits.substring(2);
        }
        if (digits.length() > 10) {
            return digits.substring(digits.length() - 10);
        }
        return digits;
    }
}
