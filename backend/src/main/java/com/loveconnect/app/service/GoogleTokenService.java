package com.loveconnect.app.service;

import com.loveconnect.app.exception.BadRequestException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class GoogleTokenService {
    private final String googleClientId;
    private final RestTemplate restTemplate = new RestTemplate();

    public GoogleTokenService(@Value("${app.google.client-id:}") String googleClientId) {
        this.googleClientId = googleClientId;
    }

    public GoogleProfile verify(String idToken) {
        if (!StringUtils.hasText(googleClientId)) {
            throw new BadRequestException("Google login is not configured");
        }
        String url = UriComponentsBuilder.fromHttpUrl("https://oauth2.googleapis.com/tokeninfo")
                .queryParam("id_token", idToken)
                .toUriString();
        Map<?, ?> payload;
        try {
            payload = restTemplate.getForObject(url, Map.class);
        } catch (Exception ex) {
            throw new BadRequestException("Invalid Google token");
        }
        if (payload == null || !googleClientId.equals(String.valueOf(payload.get("aud")))) {
            throw new BadRequestException("Invalid Google token");
        }
        String email = String.valueOf(payload.get("email"));
        if (!StringUtils.hasText(email) || !"true".equals(String.valueOf(payload.get("email_verified")))) {
            throw new BadRequestException("Google email is not verified");
        }
        String rawName = payload.get("name") == null ? "" : String.valueOf(payload.get("name"));
        String name = StringUtils.hasText(rawName) && !"null".equalsIgnoreCase(rawName)
                ? rawName
                : email.substring(0, email.indexOf('@'));
        String picture = payload.get("picture") == null ? "" : String.valueOf(payload.get("picture"));
        return new GoogleProfile(email.toLowerCase(), name, picture);
    }

    public static class GoogleProfile {
        private final String email;
        private final String name;
        private final String picture;

        GoogleProfile(String email, String name, String picture) {
            this.email = email;
            this.name = name;
            this.picture = picture;
        }

        public String getEmail() { return email; }
        public String getName() { return name; }
        public String getPicture() { return picture; }
    }
}
