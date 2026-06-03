package com.loveconnect.mongoapp.security;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FirebaseTokenService {
    private final boolean allowDevTokens;
    private final AppTokenService appTokens;

    public FirebaseTokenService(
        @Value("${app.firebase.service-account-json:}") String serviceAccountJson,
        @Value("${app.firebase.allow-dev-tokens:false}") boolean allowDevTokens,
        AppTokenService appTokens
    ) {
        this.allowDevTokens = allowDevTokens;
        this.appTokens = appTokens;
        if (FirebaseApp.getApps().isEmpty() && StringUtils.hasText(serviceAccountJson)) {
            try {
                var stream = new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8));
                var options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(stream))
                    .build();
                FirebaseApp.initializeApp(options);
            } catch (Exception ex) {
                throw new IllegalStateException("Unable to initialize Firebase Admin SDK", ex);
            }
        }
    }

    public FirebasePrincipal verify(String bearerToken) {
        if (bearerToken.startsWith("app:")) {
            var uid = appTokens.verifyAndGetUid(bearerToken);
            return new FirebasePrincipal(uid, null, null);
        }
        if (allowDevTokens && bearerToken.startsWith("dev:")) {
            var phone = bearerToken.substring(4);
            var uid = "dev-" + phone.replaceAll("[^0-9A-Za-z]", "");
            return new FirebasePrincipal(uid, phone, "Dev User");
        }
        if (FirebaseApp.getApps().isEmpty()) {
            throw new IllegalStateException("Firebase Admin SDK is not configured. Set FIREBASE_SERVICE_ACCOUNT_JSON.");
        }
        try {
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(bearerToken);
            var phoneNumber = decoded.getClaims().get("phone_number");
            return new FirebasePrincipal(
                decoded.getUid(),
                phoneNumber == null ? null : phoneNumber.toString(),
                decoded.getName()
            );
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid Firebase ID token", ex);
        }
    }

    public List<String> publicPaths() {
        return List.of("/api/health", "/api/auth/register", "/api/auth/login", "/swagger-ui", "/v3/api-docs", "/ws");
    }
}
