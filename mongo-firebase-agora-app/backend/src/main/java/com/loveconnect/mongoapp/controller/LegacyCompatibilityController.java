package com.loveconnect.mongoapp.controller;

import com.loveconnect.mongoapp.dto.ApiMessage;
import com.loveconnect.mongoapp.dto.UserResponse;
import com.loveconnect.mongoapp.model.UserProfile;
import com.loveconnect.mongoapp.repository.UserProfileRepository;
import com.loveconnect.mongoapp.service.ProfileService;
import com.loveconnect.mongoapp.service.SecurityContextService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class LegacyCompatibilityController {
    private final SecurityContextService security;
    private final ProfileService profiles;
    private final UserProfileRepository users;

    public LegacyCompatibilityController(SecurityContextService security, ProfileService profiles, UserProfileRepository users) {
        this.security = security;
        this.profiles = profiles;
        this.users = users;
    }

    @GetMapping("/search")
    public List<Map<String, Object>> search(
        @RequestParam(required = false) Integer minAge,
        @RequestParam(required = false) Integer maxAge,
        @RequestParam(required = false) String gender,
        @RequestParam(required = false) String city,
        @RequestParam(required = false) String interest
    ) {
        var current = touchCurrent();
        return users.findByFirebaseUidNotOrderByDisplayNameAsc(current.getFirebaseUid()).stream()
            .filter(user -> minAge == null || user.getAge() == null || user.getAge() >= minAge)
            .filter(user -> maxAge == null || user.getAge() == null || user.getAge() <= maxAge)
            .filter(user -> blank(gender) || equalsIgnoreCase(user.getGender(), gender))
            .filter(user -> blank(city) || containsIgnoreCase(user.getLocation(), city))
            .filter(user -> blank(interest) || interests(user).stream().anyMatch(value -> containsIgnoreCase(value, interest)))
            .map(user -> match(current, user))
            .toList();
    }

    @GetMapping("/search/recommendations")
    public List<Map<String, Object>> recommendations() {
        return search(null, null, null, null, null);
    }

    @PostMapping("/likes")
    public ApiMessage like(@RequestBody Map<String, Object> request) {
        return new ApiMessage(Boolean.FALSE.equals(request.get("liked")) ? "Profile passed." : "Like saved.");
    }

    @GetMapping("/likes/matches")
    public List<Map<String, Object>> matches() {
        return List.of();
    }

    @GetMapping("/likes/received")
    public List<Map<String, Object>> receivedLikes() {
        return List.of();
    }

    @GetMapping("/notifications")
    public List<Map<String, Object>> notifications() {
        return List.of();
    }

    @GetMapping("/subscriptions")
    public List<Map<String, Object>> subscriptions() {
        return List.of();
    }

    @PostMapping("/subscriptions")
    public Map<String, Object> subscribe(@RequestBody Map<String, Object> request) {
        var item = new LinkedHashMap<String, Object>();
        item.put("id", "sub-" + Instant.now().toEpochMilli());
        item.put("planType", Objects.toString(request.getOrDefault("planType", "FREE")));
        item.put("status", "ACTIVE");
        item.put("startDate", Instant.now().toString());
        item.put("endDate", Instant.now().plus(30, ChronoUnit.DAYS).toString());
        return item;
    }

    @PostMapping("/safety/reports")
    public ApiMessage report() {
        return new ApiMessage("Report sent.");
    }

    @PostMapping("/safety/blocks/{userId}")
    public ApiMessage block(@PathVariable String userId) {
        return new ApiMessage("User blocked.");
    }

    private UserProfile touchCurrent() {
        var profile = profiles.getOrCreate(security.currentUser());
        profile.setOnline(true);
        profile.setLastSeenAt(Instant.now());
        return users.save(profile);
    }

    private Map<String, Object> match(UserProfile current, UserProfile target) {
        var common = commonInterests(current, target);
        int score = Math.min(95, 30 + (common.size() * 20) + (equalsIgnoreCase(current.getLocation(), target.getLocation()) ? 10 : 0));
        var item = new LinkedHashMap<String, Object>();
        item.put("user", UserResponse.from(target));
        item.put("matchScore", score);
        item.put("commonInterests", common);
        return item;
    }

    private List<String> commonInterests(UserProfile current, UserProfile target) {
        Set<String> currentValues = interests(current).stream()
            .map(value -> value.toLowerCase(Locale.ROOT))
            .collect(Collectors.toSet());
        List<String> common = new ArrayList<>();
        for (String interest : interests(target)) {
            if (currentValues.contains(interest.toLowerCase(Locale.ROOT))) {
                common.add(interest);
            }
        }
        return common;
    }

    private List<String> interests(UserProfile user) {
        return user.getInterests() == null ? List.of() : user.getInterests();
    }

    private boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private boolean containsIgnoreCase(String value, String needle) {
        return value != null && needle != null && value.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }
}
