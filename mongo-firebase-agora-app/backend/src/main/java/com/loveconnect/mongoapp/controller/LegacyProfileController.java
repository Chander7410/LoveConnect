package com.loveconnect.mongoapp.controller;

import com.loveconnect.mongoapp.dto.ProfileUpdateRequest;
import com.loveconnect.mongoapp.dto.UserResponse;
import com.loveconnect.mongoapp.model.UserProfile;
import com.loveconnect.mongoapp.repository.UserProfileRepository;
import com.loveconnect.mongoapp.service.ProfileService;
import com.loveconnect.mongoapp.service.SecurityContextService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
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
    public Map<String, Object> picture(@RequestParam("file") MultipartFile file) throws IOException {
        var profile = profiles.getOrCreate(security.currentUser());
        profile.setPhotoUrl(toDataUrl(file));
        profile.setLastSeenAt(Instant.now());
        return legacyResponse(users.save(profile));
    }

    @PostMapping("/photos")
    public Map<String, Object> photos(@RequestParam("file") MultipartFile file) throws IOException {
        var profile = profiles.getOrCreate(security.currentUser());
        var gallery = new ArrayList<>(profile.getPhotoUrls() == null ? List.<String>of() : profile.getPhotoUrls());
        gallery.add(toDataUrl(file));
        profile.setPhotoUrls(gallery.stream().skip(Math.max(0, gallery.size() - 8)).toList());
        profile.setLastSeenAt(Instant.now());
        return legacyResponse(users.save(profile));
    }

    private Map<String, Object> legacyResponse(UserProfile profile) {
        return Map.of(
            "user", UserResponse.from(profile),
            "bio", profile.getBio() == null ? "" : profile.getBio(),
            "education", profile.getEducation() == null ? "" : profile.getEducation(),
            "profession", profile.getProfession() == null ? "" : profile.getProfession(),
            "city", profile.getLocation() == null ? "" : profile.getLocation(),
            "interests", profile.getInterests() == null ? List.of() : profile.getInterests(),
            "photoUrls", profile.getPhotoUrls() == null ? List.of() : profile.getPhotoUrls()
        );
    }

    private String toDataUrl(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please choose a photo to upload.");
        }
        var contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new IllegalArgumentException("Only image uploads are supported.");
        }
        if (file.getSize() > 2_000_000) {
            throw new IllegalArgumentException("Photo must be 2 MB or smaller.");
        }
        return "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(file.getBytes());
    }
}
