package com.loveconnect.mongoapp.controller;

import com.loveconnect.mongoapp.dto.AuthResponse;
import com.loveconnect.mongoapp.dto.ApiMessage;
import com.loveconnect.mongoapp.dto.GoogleLoginRequest;
import com.loveconnect.mongoapp.dto.LoginRequest;
import com.loveconnect.mongoapp.dto.OtpSendRequest;
import com.loveconnect.mongoapp.dto.OtpVerifyRequest;
import com.loveconnect.mongoapp.dto.RegisterRequest;
import com.loveconnect.mongoapp.model.UserProfile;
import com.loveconnect.mongoapp.repository.UserProfileRepository;
import com.loveconnect.mongoapp.service.AppAuthService;
import com.loveconnect.mongoapp.service.ProfileService;
import com.loveconnect.mongoapp.service.SecurityContextService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.security.SecureRandom;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final SecurityContextService security;
    private final ProfileService profiles;
    private final AppAuthService appAuth;
    private final UserProfileRepository users;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthController(SecurityContextService security, ProfileService profiles, AppAuthService appAuth,
                          UserProfileRepository users, PasswordEncoder passwordEncoder) {
        this.security = security;
        this.profiles = profiles;
        this.appAuth = appAuth;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return appAuth.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return appAuth.login(request);
    }

    @PostMapping("/google")
    public AuthResponse google(@Valid @RequestBody GoogleLoginRequest request) {
        return appAuth.googleLogin(request);
    }

    @PostMapping("/send-otp")
    public ApiMessage sendOtp(@Valid @RequestBody OtpSendRequest request) {
        appAuth.sendOtp(request);
        return new ApiMessage("OTP sent to your Gmail");
    }

    @PostMapping("/verify-otp")
    public ApiMessage verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        appAuth.verifyOtp(request);
        return new ApiMessage("OTP verified successfully");
    }

    @GetMapping("/me")
    public UserProfile me() {
        return profiles.getOrCreate(security.currentUser());
    }

    @PostMapping("/presence")
    public ApiMessage presence() {
        var profile = profiles.getOrCreate(security.currentUser());
        profile.setOnline(true);
        profile.setLastSeenAt(Instant.now());
        users.save(profile);
        return new ApiMessage("Online status updated.");
    }

    @PostMapping("/logout")
    public ApiMessage logout() {
        var profile = profiles.getOrCreate(security.currentUser());
        profile.setOnline(false);
        profile.setLastSeenAt(Instant.now());
        users.save(profile);
        return new ApiMessage("Logged out.");
    }

    @PostMapping("/forgot-password")
    public Map<String, Object> forgotPassword(@RequestBody Map<String, String> request) {
        var email = request.getOrDefault("email", "").trim().toLowerCase();
        return users.findAllByEmail(email).stream().findFirst()
            .map(user -> {
                byte[] tokenBytes = new byte[32];
                secureRandom.nextBytes(tokenBytes);
                String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
                user.setPasswordResetToken(token);
                user.setPasswordResetExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
                users.save(user);
                return Map.<String, Object>of(
                    "message", "Reset token created. Use it below to set a new password.",
                    "resetToken", token
                );
            })
            .orElseGet(() -> Map.of("message", "If the email exists, a reset link will be sent."));
    }

    @PostMapping("/reset-password")
    public ApiMessage resetPassword(@RequestBody Map<String, String> request) {
        var token = request.getOrDefault("token", "");
        var user = users.findByPasswordResetToken(token)
            .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token"));
        if (user.getPasswordResetExpiresAt() == null || user.getPasswordResetExpiresAt().isBefore(Instant.now())) {
            user.setPasswordResetToken(null);
            user.setPasswordResetExpiresAt(null);
            users.save(user);
            throw new IllegalArgumentException("Invalid or expired reset token");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getOrDefault("newPassword", "")));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiresAt(null);
        users.save(user);
        return new ApiMessage("Password updated. You can login with the new password.");
    }
}
