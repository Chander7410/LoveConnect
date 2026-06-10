package com.loveconnect.mongoapp.service;

import com.loveconnect.mongoapp.dto.ProfileUpdateRequest;
import com.loveconnect.mongoapp.model.UserProfile;
import com.loveconnect.mongoapp.repository.UserProfileRepository;
import com.loveconnect.mongoapp.security.FirebasePrincipal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ProfileService {
    private final UserProfileRepository userProfiles;

    public ProfileService(UserProfileRepository userProfiles) {
        this.userProfiles = userProfiles;
    }

    public UserProfile getOrCreate(FirebasePrincipal principal) {
        return newestFirst(userProfiles.findAllByFirebaseUid(principal.uid())).stream().findFirst().orElseGet(() -> {
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
        if (request.education() != null) {
            profile.setEducation(request.education().trim());
        }
        if (request.profession() != null) {
            profile.setProfession(request.profession().trim());
        }
        if (request.city() != null) {
            profile.setLocation(request.city().trim());
        }
        if (request.interests() != null) {
            profile.setInterests(request.interests().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .limit(20)
                .toList());
        }
        profile.setLastSeenAt(Instant.now());
        return userProfiles.save(profile);
    }

    public List<UserProfile> discover(FirebasePrincipal principal) {
        getOrCreate(principal);
        return userProfiles.findByFirebaseUidNotOrderByDisplayNameAsc(principal.uid());
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
