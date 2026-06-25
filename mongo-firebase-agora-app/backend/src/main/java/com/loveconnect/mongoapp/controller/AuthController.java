package com.loveconnect.mongoapp.controller;

import com.loveconnect.mongoapp.dto.AuthResponse;
import com.loveconnect.mongoapp.dto.ApiMessage;
import com.loveconnect.mongoapp.dto.CompleteSignupRequest;
import com.loveconnect.mongoapp.dto.ForgotPasswordOtpRequest;
import com.loveconnect.mongoapp.dto.ForgotPasswordOtpVerifyRequest;
import com.loveconnect.mongoapp.dto.ForgotPasswordResetOtpRequest;
import com.loveconnect.mongoapp.dto.GoogleLoginRequest;
import com.loveconnect.mongoapp.dto.LoginOtpVerifyRequest;
import com.loveconnect.mongoapp.dto.LoginRequest;
import com.loveconnect.mongoapp.dto.MobileOtpSendRequest;
import com.loveconnect.mongoapp.dto.MobileOtpVerifyRequest;
import com.loveconnect.mongoapp.dto.OtpSendRequest;
import com.loveconnect.mongoapp.dto.OtpVerifyRequest;
import com.loveconnect.mongoapp.dto.ResendOtpRequest;
import com.loveconnect.mongoapp.dto.SignupEmailOtpRequest;
import com.loveconnect.mongoapp.dto.SignupEmailOtpVerifyRequest;
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
    public ApiMessage register(@RequestBody SignupEmailOtpRequest request) {
        appAuth.sendSignupEmailOtp(request);
        return new ApiMessage("OTP sent to your Email ID");
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return appAuth.login(request);
    }

    @PostMapping("/verify-login-otp")
    public AuthResponse verifyLoginOtp(@Valid @RequestBody LoginOtpVerifyRequest request) {
        return appAuth.verifyLoginOtp(request);
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
    public AuthResponse verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        return appAuth.verifyOtp(request);
    }

    @PostMapping("/send-signup-otp")
    public ApiMessage sendSignupOtp(@Valid @RequestBody MobileOtpSendRequest request) {
        appAuth.sendSignupOtp(request);
        return new ApiMessage("OTP sent to your mobile number");
    }

    @PostMapping("/signup/send-otp")
    public ApiMessage signupSendOtp(@RequestBody SignupEmailOtpRequest request) {
        appAuth.sendSignupEmailOtp(request);
        return new ApiMessage("OTP sent to your Email ID");
    }

    @PostMapping("/signup/verify-otp")
    public ApiMessage signupVerifyOtp(@Valid @RequestBody SignupEmailOtpVerifyRequest request) {
        appAuth.verifySignupEmailOtp(request);
        return new ApiMessage("Signup completed successfully. Please login.");
    }

    @PostMapping("/verify-signup-otp")
    public ApiMessage verifySignupOtp(@RequestBody Map<String, String> request) {
        var email = request.getOrDefault("email", "");
        var otp = request.getOrDefault("otp", "");
        if (!email.isBlank()) {
            appAuth.verifySignupEmailOtp(new SignupEmailOtpVerifyRequest(email, otp));
            return new ApiMessage("Signup completed successfully. Please login.");
        }
        appAuth.verifySignupOtp(new MobileOtpVerifyRequest(request.getOrDefault("mobileNumber", ""), otp));
        return new ApiMessage("OTP verified successfully");
    }

    @PostMapping("/resend-signup-otp")
    public ApiMessage resendSignupOtp(@RequestBody Map<String, String> request) {
        appAuth.resendSignupEmailOtp(request.getOrDefault("email", ""));
        return new ApiMessage("OTP sent again");
    }

    @PostMapping("/complete-signup")
    public AuthResponse completeSignup(@Valid @RequestBody CompleteSignupRequest request) {
        return appAuth.completeSignup(request);
    }

    @PostMapping("/forgot-password/send-otp")
    public ApiMessage forgotPasswordSendOtp(@Valid @RequestBody ForgotPasswordOtpRequest request) {
        appAuth.sendForgotPasswordOtp(request);
        return new ApiMessage("OTP sent to registered Email ID");
    }

    @PostMapping("/forgot-password/verify-otp")
    public ApiMessage forgotPasswordVerifyOtp(@Valid @RequestBody ForgotPasswordOtpVerifyRequest request) {
        appAuth.verifyForgotPasswordOtp(request);
        return new ApiMessage("OTP verified successfully");
    }

    @PostMapping("/forgot-password/reset")
    public ApiMessage forgotPasswordReset(@Valid @RequestBody ForgotPasswordResetOtpRequest request) {
        appAuth.resetForgotPassword(request);
        return new ApiMessage("Password updated. You can login with the new password.");
    }

    @PostMapping("/resend-otp")
    public ApiMessage resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        appAuth.resendMobileOtp(request);
        return new ApiMessage("OTP sent again");
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
