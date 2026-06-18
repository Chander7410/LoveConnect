package com.loveconnect.mongoapp.service;

import com.loveconnect.mongoapp.dto.AuthResponse;
import com.loveconnect.mongoapp.dto.GoogleLoginRequest;
import com.loveconnect.mongoapp.dto.LoginRequest;
import com.loveconnect.mongoapp.dto.OtpSendRequest;
import com.loveconnect.mongoapp.dto.OtpVerifyRequest;
import com.loveconnect.mongoapp.dto.RegisterRequest;
import com.loveconnect.mongoapp.dto.UserResponse;
import com.loveconnect.mongoapp.model.EmailOtp;
import com.loveconnect.mongoapp.model.UserProfile;
import com.loveconnect.mongoapp.repository.EmailOtpRepository;
import com.loveconnect.mongoapp.repository.UserProfileRepository;
import com.loveconnect.mongoapp.security.AppTokenService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AppAuthService {
    private final UserProfileRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AppTokenService tokens;
    private final EmailOtpRepository otps;
    private final EmailService emails;
    private final GoogleTokenService googleTokens;
    private final SecureRandom secureRandom = new SecureRandom();

    public AppAuthService(UserProfileRepository users, PasswordEncoder passwordEncoder, AppTokenService tokens,
                          EmailOtpRepository otps, EmailService emails, GoogleTokenService googleTokens) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.otps = otps;
        this.emails = emails;
        this.googleTokens = googleTokens;
    }

    public AuthResponse register(RegisterRequest request) {
        var email = request.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }
        if (users.existsByPhoneNumber(request.mobileNumber())) {
            throw new IllegalArgumentException("Mobile number is already registered");
        }

        var user = new UserProfile();
        user.setFirebaseUid("app-" + email.replaceAll("[^a-z0-9]", ""));
        user.setDisplayName(request.name().trim());
        user.setEmail(email);
        user.setPhoneNumber(request.mobileNumber().trim());
        user.setGender(request.gender().trim().toUpperCase());
        user.setAge(request.age());
        user.setLocation(request.location().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setOnline(true);
        user.setVerified(true);
        user.setProvider("LOCAL");
        user.setEmailVerified(false);
        user.setLastSeenAt(Instant.now());
        user.setRole("USER");
        var saved = users.save(user);
        return new AuthResponse(tokens.create(saved.getFirebaseUid()), UserResponse.from(saved));
    }

    public AuthResponse login(LoginRequest request) {
        var candidates = users.findAllByEmail(request.email().trim().toLowerCase());
        var user = newestFirst(candidates).stream()
            .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if ("LOCAL".equalsIgnoreCase(user.getProvider()) && !user.isEmailVerified()) {
            throw new IllegalArgumentException("EMAIL_NOT_VERIFIED");
        }
        user.setOnline(true);
        user.setLastSeenAt(Instant.now());
        var saved = users.save(user);
        return new AuthResponse(tokens.create(saved.getFirebaseUid()), UserResponse.from(saved));
    }

    public AuthResponse googleLogin(GoogleLoginRequest request) {
        var googleProfile = googleTokens.verify(request.idToken());
        var user = users.findAllByEmail(googleProfile.email()).stream()
            .findFirst()
            .map(existing -> {
                if (existing.getProvider() == null || existing.getProvider().isBlank()) {
                    existing.setProvider("GOOGLE");
                }
                if (googleProfile.picture() != null && !googleProfile.picture().isBlank()) {
                    existing.setPhotoUrl(googleProfile.picture());
                }
                return existing;
            })
            .orElseGet(() -> createGoogleUser(googleProfile));
        user.setProvider("GOOGLE");
        user.setEmailVerified(false);
        users.save(user);
        throw new IllegalArgumentException("EMAIL_NOT_VERIFIED");
    }

    public void sendOtp(OtpSendRequest request) {
        var email = normalizeEmail(request.email());
        if (otps.countByEmailAndCreatedAtAfter(email, Instant.now().minus(1, ChronoUnit.MINUTES)) >= 1) {
            throw new IllegalArgumentException("Please wait before requesting another OTP");
        }
        var otp = String.format(Locale.US, "%06d", secureRandom.nextInt(1_000_000));
        var emailOtp = new EmailOtp();
        emailOtp.setEmail(email);
        emailOtp.setOtpHash(hashOtp(email, otp));
        emailOtp.setExpiryTime(Instant.now().plus(5, ChronoUnit.MINUTES));
        otps.save(emailOtp);
        emails.sendOtp(email, otp);
    }

    public AuthResponse verifyOtp(OtpVerifyRequest request) {
        var email = normalizeEmail(request.email());
        var emailOtp = otps.findTopByEmailOrderByCreatedAtDesc(email)
            .orElseThrow(() -> new IllegalArgumentException("Invalid or expired OTP"));
        if (emailOtp.isVerified() || emailOtp.getExpiryTime().isBefore(Instant.now()) || emailOtp.getAttempts() >= 5) {
            throw new IllegalArgumentException("Invalid or expired OTP");
        }
        emailOtp.setAttempts(emailOtp.getAttempts() + 1);
        if (!hashOtp(email, request.otp()).equals(emailOtp.getOtpHash())) {
            otps.save(emailOtp);
            throw new IllegalArgumentException("Invalid or expired OTP");
        }
        emailOtp.setVerified(true);
        otps.save(emailOtp);
        var user = newestFirst(users.findAllByEmail(email)).stream()
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("User not found for verified email"));
        user.setEmailVerified(true);
        user.setOnline(true);
        user.setLastSeenAt(Instant.now());
        var saved = users.save(user);
        return new AuthResponse(tokens.create(saved.getFirebaseUid()), UserResponse.from(saved));
    }

    private List<UserProfile> newestFirst(List<UserProfile> profiles) {
        return profiles.stream()
            .sorted(Comparator.comparing(
                UserProfile::getUpdatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())
            ))
            .toList();
    }

    private UserProfile createGoogleUser(GoogleTokenService.GoogleProfile googleProfile) {
        var user = new UserProfile();
        user.setFirebaseUid("google-" + googleProfile.email().replaceAll("[^a-z0-9]", ""));
        user.setDisplayName(googleProfile.name());
        user.setEmail(googleProfile.email());
        user.setPhoneNumber(uniqueGooglePhone());
        user.setPasswordHash(passwordEncoder.encode(randomToken()));
        user.setGender("OTHER");
        user.setAge(18);
        user.setLocation("USA");
        user.setRole("USER");
        user.setProvider("GOOGLE");
        user.setEmailVerified(false);
        user.setVerified(true);
        user.setPhotoUrl(googleProfile.picture());
        return user;
    }

    private String uniqueGooglePhone() {
        String phone;
        do {
            phone = "G" + String.format(Locale.US, "%019d", Math.abs(secureRandom.nextLong()));
            if (phone.length() > 20) {
                phone = phone.substring(0, 20);
            }
        } while (users.existsByPhoneNumber(phone));
        return phone;
    }

    private String randomToken() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.US);
    }

    private String hashOtp(String email, String otp) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var hash = digest.digest((normalizeEmail(email) + ":" + otp).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Could not process OTP");
        }
    }
}
