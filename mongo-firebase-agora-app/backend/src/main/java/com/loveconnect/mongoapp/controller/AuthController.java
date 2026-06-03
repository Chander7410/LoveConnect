package com.loveconnect.mongoapp.controller;

import com.loveconnect.mongoapp.dto.AuthResponse;
import com.loveconnect.mongoapp.dto.LoginRequest;
import com.loveconnect.mongoapp.dto.RegisterRequest;
import com.loveconnect.mongoapp.model.UserProfile;
import com.loveconnect.mongoapp.service.AppAuthService;
import com.loveconnect.mongoapp.service.ProfileService;
import com.loveconnect.mongoapp.service.SecurityContextService;
import jakarta.validation.Valid;
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

    public AuthController(SecurityContextService security, ProfileService profiles, AppAuthService appAuth) {
        this.security = security;
        this.profiles = profiles;
        this.appAuth = appAuth;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return appAuth.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return appAuth.login(request);
    }

    @GetMapping("/me")
    public UserProfile me() {
        return profiles.getOrCreate(security.currentUser());
    }
}
