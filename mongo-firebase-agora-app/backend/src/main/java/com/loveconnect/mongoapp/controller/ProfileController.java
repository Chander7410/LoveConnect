package com.loveconnect.mongoapp.controller;

import com.loveconnect.mongoapp.dto.ProfileUpdateRequest;
import com.loveconnect.mongoapp.model.UserProfile;
import com.loveconnect.mongoapp.service.ProfileService;
import com.loveconnect.mongoapp.service.SecurityContextService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {
    private final SecurityContextService security;
    private final ProfileService profiles;

    public ProfileController(SecurityContextService security, ProfileService profiles) {
        this.security = security;
        this.profiles = profiles;
    }

    @GetMapping("/me")
    public UserProfile me() {
        return profiles.getOrCreate(security.currentUser());
    }

    @PutMapping("/me")
    public UserProfile update(@Valid @RequestBody ProfileUpdateRequest request) {
        return profiles.update(security.currentUser(), request);
    }

    @GetMapping
    public List<UserProfile> discover() {
        return profiles.discover(security.currentUser());
    }
}
