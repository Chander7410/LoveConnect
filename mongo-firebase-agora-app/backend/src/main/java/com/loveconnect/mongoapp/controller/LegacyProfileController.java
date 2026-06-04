package com.loveconnect.mongoapp.controller;

import com.loveconnect.mongoapp.dto.ProfileUpdateRequest;
import com.loveconnect.mongoapp.dto.UserResponse;
import com.loveconnect.mongoapp.model.UserProfile;
import com.loveconnect.mongoapp.repository.UserProfileRepository;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/profile")
public class LegacyProfileController {
    private final SecurityContextService security;
    private final ProfileService profiles;
    private final UserProfileRepository users;

    public LegacyProfileController(SecurityContextService security, ProfileService profiles, UserProfileRepository users) {
        this.security = security;
        this.profiles = profiles;
        this.users = users;
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
        var profile = profiles.getOrCreate(security.currentUser());
        profile.setVerified(true);
        return legacyResponse(users.save(profile));
    }

    @PostMapping("/picture")
    public Map<String, Object> picture(@RequestParam("file") MultipartFile file) {
        return legacyResponse(profiles.getOrCreate(security.currentUser()));
    }

    @PostMapping("/photos")
    public Map<String, Object> photos(@RequestParam("file") MultipartFile file) {
        return legacyResponse(profiles.getOrCreate(security.currentUser()));
    }

    private Map<String, Object> legacyResponse(UserProfile profile) {
        return Map.of(
            "user", UserResponse.from(profile),
            "bio", profile.getBio() == null ? "" : profile.getBio(),
            "education", profile.getEducation() == null ? "" : profile.getEducation(),
            "profession", profile.getProfession() == null ? "" : profile.getProfession(),
            "city", profile.getLocation() == null ? "" : profile.getLocation(),
            "interests", profile.getInterests() == null ? List.of() : profile.getInterests(),
            "photoUrls", List.of()
        );
    }
}
