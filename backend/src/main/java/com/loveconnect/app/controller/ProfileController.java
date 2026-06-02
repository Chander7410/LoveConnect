package com.loveconnect.app.controller;

import com.loveconnect.app.dto.ProfileRequest;
import com.loveconnect.app.dto.ProfileResponse;
import com.loveconnect.app.service.CurrentUserService;
import com.loveconnect.app.service.ProfileService;
import javax.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final CurrentUserService currentUserService;
    private final ProfileService profileService;

    public ProfileController(CurrentUserService currentUserService, ProfileService profileService) {
        this.currentUserService = currentUserService;
        this.profileService = profileService;
    }

    @GetMapping("/me")
    public ProfileResponse me(Authentication authentication) {
        return profileService.get(currentUserService.get(authentication).getId());
    }

    @GetMapping("/{userId}")
    public ProfileResponse byUser(@PathVariable Long userId) {
        return profileService.get(userId);
    }

    @PutMapping("/me")
    public ProfileResponse update(Authentication authentication, @Valid @RequestBody ProfileRequest request) {
        return profileService.update(currentUserService.get(authentication), request);
    }

    @PostMapping("/picture")
    public ProfileResponse picture(Authentication authentication, @RequestParam("file") MultipartFile file) {
        return profileService.uploadProfilePicture(currentUserService.get(authentication), file);
    }

    @PostMapping("/photos")
    public ProfileResponse photo(Authentication authentication, @RequestParam("file") MultipartFile file) {
        return profileService.uploadPhoto(currentUserService.get(authentication), file);
    }

    @PostMapping("/verify")
    public ProfileResponse verify(Authentication authentication) {
        return profileService.requestVerification(currentUserService.get(authentication));
    }
}


