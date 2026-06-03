package com.loveconnect.mongoapp.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AppTokenService {
    private final String secret;

    public AppTokenService(@Value("${app.jwt.secret}") String secret) {
        this.secret = secret;
    }

    public String create(String uid) {
        var encodedUid = Base64.getUrlEncoder().withoutPadding().encodeToString(uid.getBytes(StandardCharsets.UTF_8));
        return "app:" + encodedUid + ":" + sign(encodedUid);
    }

    public String verifyAndGetUid(String token) {
        if (!token.startsWith("app:")) {
            throw new IllegalArgumentException("Invalid app token");
        }
        var parts = token.split(":", 3);
        if (parts.length != 3 || !sign(parts[1]).equals(parts[2])) {
            throw new IllegalArgumentException("Invalid app token");
        }
        return new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
    }

    private String sign(String value) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign app token", ex);
        }
    }
}
