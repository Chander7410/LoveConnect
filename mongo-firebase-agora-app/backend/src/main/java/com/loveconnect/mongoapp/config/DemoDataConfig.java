package com.loveconnect.mongoapp.config;

import com.loveconnect.mongoapp.model.UserProfile;
import com.loveconnect.mongoapp.repository.UserProfileRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DemoDataConfig {
    private static final Logger log = LoggerFactory.getLogger(DemoDataConfig.class);
    private static final String LIVE_TEST_PASSWORD = "qwerty@123";

    @Bean
    @ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true")
    CommandLineRunner seedMongoProfiles(UserProfileRepository users, PasswordEncoder passwordEncoder) {
        return args -> {
            seedProfile(users, passwordEncoder, "roshan", "roshan@gmail.com", "7000000001",
                "MALE", 18, "pune", "Student", "Live profile", List.of("Car", "music", "travel"), LIVE_TEST_PASSWORD, true);
            seedProfile(users, passwordEncoder, "pinky", "pinky@gmail.com", "7000000002",
                "FEMALE", 18, "Bangalore", "Student", "Live profile", List.of("Car", "music", "travel"), LIVE_TEST_PASSWORD, true);
            seedProfile(users, passwordEncoder, "Aisha Demo", "demo.aisha@loveconnect.test", "9800000001",
                "FEMALE", 27, "Pune", "Product designer", "Coffee walks, music, and weekend travel.", List.of("music", "travel", "coffee"), "Password123!", false);
            seedProfile(users, passwordEncoder, "Rahul Demo", "demo.rahul@loveconnect.test", "9800000002",
                "MALE", 30, "Pune", "Software engineer", "Runner, reader, and live music fan.", List.of("music", "running", "books"), "Password123!", false);
            seedProfile(users, passwordEncoder, "Mira Demo", "demo.mira@loveconnect.test", "9800000003",
                "FEMALE", 29, "Mumbai", "Marketing lead", "Food trails, travel, and photography.", List.of("travel", "food", "photography"), "Password123!", false);
        };
    }

    private void seedProfile(UserProfileRepository users, PasswordEncoder passwordEncoder, String name, String email,
                             String phone, String gender, Integer age, String location, String profession,
                             String bio, List<String> interests, String password, boolean online) {
        try {
            upsert(users, passwordEncoder, name, email, phone, gender, age, location, profession, bio, interests, password, online);
        } catch (RuntimeException ex) {
            log.warn("Skipping demo profile seed for {}", email, ex);
        }
    }

    private void upsert(UserProfileRepository users, PasswordEncoder passwordEncoder, String name, String email,
                        String phone, String gender, Integer age, String location, String profession,
                        String bio, List<String> interests, String password, boolean online) {
        String firebaseUid = "app-" + email.replaceAll("[^a-z0-9]", "");
        var profile = firstByEmail(users, email)
            .or(() -> firstByFirebaseUid(users, firebaseUid))
            .or(() -> firstByPhone(users, phone))
            .orElseGet(UserProfile::new);

        if (isUniqueAvailable(firstByFirebaseUid(users, firebaseUid).orElse(null), profile)) {
            profile.setFirebaseUid(firebaseUid);
        } else if (profile.getFirebaseUid() == null) {
            profile.setFirebaseUid("app-" + email.replaceAll("[^a-z0-9]", "") + "-" + System.currentTimeMillis());
        }
        profile.setDisplayName(name);
        if (isUniqueAvailable(firstByEmail(users, email).orElse(null), profile)) {
            profile.setEmail(email);
        }
        if (isUniqueAvailable(firstByPhone(users, phone).orElse(null), profile)) {
            profile.setPhoneNumber(phone);
        }
        profile.setGender(gender);
        profile.setAge(age);
        profile.setLocation(location);
        profile.setProfession(profession);
        profile.setBio(bio);
        profile.setInterests(interests);
        profile.setPasswordHash(passwordEncoder.encode(password));
        profile.setRole("USER");
        profile.setVerified(true);
        profile.setOnline(online);
        profile.setLastSeenAt(Instant.now());
        users.save(profile);
    }

    private boolean isUniqueAvailable(UserProfile existing, UserProfile profile) {
        return existing == null || Objects.equals(existing.getId(), profile.getId());
    }

    private java.util.Optional<UserProfile> firstByEmail(UserProfileRepository users, String email) {
        return users.findAllByEmail(email).stream().findFirst();
    }

    private java.util.Optional<UserProfile> firstByPhone(UserProfileRepository users, String phone) {
        return users.findAllByPhoneNumber(phone).stream().findFirst();
    }

    private java.util.Optional<UserProfile> firstByFirebaseUid(UserProfileRepository users, String firebaseUid) {
        return users.findAllByFirebaseUid(firebaseUid).stream().findFirst();
    }
}
