package com.loveconnect.mongoapp.service;

import com.loveconnect.mongoapp.dto.ProfileUpdateRequest;
import com.loveconnect.mongoapp.model.UserProfile;
import com.loveconnect.mongoapp.repository.UserProfileRepository;
import com.loveconnect.mongoapp.security.FirebasePrincipal;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ProfileService {
    private final UserProfileRepository userProfiles;

    public ProfileService(UserProfileRepository userProfiles) {
        this.userProfiles = userProfiles;
    }

    public UserProfile getOrCreate(FirebasePrincipal principal) {
        return userProfiles.findByFirebaseUid(principal.uid()).orElseGet(() -> {
            var profile = new UserProfile();
            profile.setFirebaseUid(principal.uid());
            profile.setPhoneNumber(StringUtils.hasText(principal.phoneNumber()) ? principal.phoneNumber() : principal.uid());
            profile.setDisplayName(StringUtils.hasText(principal.name()) ? principal.name() : "New Member");
            profile.setOnline(true);
            profile.setLastSeenAt(Instant.now());
            return userProfiles.save(profile);
        });
    }

    public UserProfile update(FirebasePrincipal principal, ProfileUpdateRequest request) {
        var profile = getOrCreate(principal);
        if (request.displayName() != null) {
            profile.setDisplayName(request.displayName().trim());
        }
        if (request.bio() != null) {
            profile.setBio(request.bio().trim());
        }
        if (request.photoUrl() != null) {
            profile.setPhotoUrl(request.photoUrl().trim());
        }
        profile.setLastSeenAt(Instant.now());
        return userProfiles.save(profile);
    }

    public List<UserProfile> discover(FirebasePrincipal principal) {
        getOrCreate(principal);
        return userProfiles.findByFirebaseUidNotOrderByDisplayNameAsc(principal.uid());
    }
}
