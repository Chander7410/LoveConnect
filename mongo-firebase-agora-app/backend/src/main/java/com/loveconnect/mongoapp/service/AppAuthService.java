package com.loveconnect.mongoapp.service;

import com.loveconnect.mongoapp.dto.AuthResponse;
import com.loveconnect.mongoapp.dto.LoginRequest;
import com.loveconnect.mongoapp.dto.RegisterRequest;
import com.loveconnect.mongoapp.dto.UserResponse;
import com.loveconnect.mongoapp.model.UserProfile;
import com.loveconnect.mongoapp.repository.UserProfileRepository;
import com.loveconnect.mongoapp.security.AppTokenService;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AppAuthService {
    private final UserProfileRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AppTokenService tokens;

    public AppAuthService(UserProfileRepository users, PasswordEncoder passwordEncoder, AppTokenService tokens) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
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
}
