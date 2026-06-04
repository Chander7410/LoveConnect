package com.loveconnect.mongoapp.controller;

import com.loveconnect.mongoapp.dto.ProfileUpdateRequest;
import com.loveconnect.mongoapp.dto.UserResponse;
import com.loveconnect.mongoapp.model.UserProfile;
import com.loveconnect.mongoapp.service.ProfileService;
import com.loveconnect.mongoapp.service.SecurityContextService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class LegacyProfileController {
    private final SecurityContextService security;
    private final ProfileService profiles;

    public LegacyProfileController(SecurityContextService security, ProfileService profiles) {
        this.security = security;
        this.profiles = profiles;
    }

    @GetMapping("/me")
    public Map<String, Object> me() {
        return legacyResponse(profiles.getOrCreate(security.currentUser()));
    }

    @PutMapping("/me")
    public Map<String, Object> update(@Valid @RequestBody ProfileUpdateRequest request) {
        return legacyResponse(profiles.update(security.currentUser(), request));
    }

    @PostMapping("/verify")
    public Map<String, Object> verify() {
        return legacyResponse(profiles.getOrCreate(security.currentUser()));
    }

    private Map<String, Object> legacyResponse(UserProfile profile) {
        return Map.of(
            "user", UserResponse.from(profile),
            "bio", profile.getBio() == null ? "" : profile.getBio(),
            "education", "",
            "profession", "",
            "city", profile.getLocation() == null ? "" : profile.getLocation(),
            "interests", List.of(),
            "photoUrls", List.of()
        );
    }
}
