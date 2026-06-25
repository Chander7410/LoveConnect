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
        return List.of("/api/health", "/api/auth/register", "/api/auth/login", "/api/auth/google",
            "/api/auth/send-otp", "/api/auth/verify-otp", "/api/auth/forgot-password",
            "/api/auth/reset-password", "/api/auth/send-signup-otp", "/api/auth/verify-signup-otp",
            "/api/auth/complete-signup", "/api/auth/resend-signup-otp",
            "/api/auth/signup/send-otp", "/api/auth/signup/verify-otp",
            "/api/auth/verify-login-otp",
            "/api/auth/forgot-password/send-otp", "/api/auth/forgot-password/verify-otp",
            "/api/auth/forgot-password/reset", "/api/auth/resend-otp",
            "/swagger-ui", "/v3/api-docs", "/ws");
    }
}
