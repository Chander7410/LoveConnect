package com.loveconnect.mongoapp.config;

import com.loveconnect.mongoapp.model.UserProfile;
import com.loveconnect.mongoapp.repository.UserProfileRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DemoDataConfig {
    private static final String LIVE_TEST_PASSWORD = "qwerty@123";

    @Bean
    CommandLineRunner seedMongoProfiles(UserProfileRepository users, PasswordEncoder passwordEncoder) {
        return args -> {
            upsert(users, passwordEncoder, "roshan", "roshan@gmail.com", "7000000001",
                "MALE", 18, "pune", "Student", "Live profile", List.of("Car", "music", "travel"), LIVE_TEST_PASSWORD, true);
            upsert(users, passwordEncoder, "pinky", "pinky@gmail.com", "7000000002",
                "FEMALE", 18, "Bangalore", "Student", "Live profile", List.of("Car", "music", "travel"), LIVE_TEST_PASSWORD, true);
            upsert(users, passwordEncoder, "Aisha Demo", "demo.aisha@loveconnect.test", "9800000001",
                "FEMALE", 27, "Pune", "Product designer", "Coffee walks, music, and weekend travel.", List.of("music", "travel", "coffee"), "Password123!", false);
            upsert(users, passwordEncoder, "Rahul Demo", "demo.rahul@loveconnect.test", "9800000002",
                "MALE", 30, "Pune", "Software engineer", "Runner, reader, and live music fan.", List.of("music", "running", "books"), "Password123!", false);
            upsert(users, passwordEncoder, "Mira Demo", "demo.mira@loveconnect.test", "9800000003",
                "FEMALE", 29, "Mumbai", "Marketing lead", "Food trails, travel, and photography.", List.of("travel", "food", "photography"), "Password123!", false);
        };
    }

    private void upsert(UserProfileRepository users, PasswordEncoder passwordEncoder, String name, String email,
                        String phone, String gender, Integer age, String location, String profession,
                        String bio, List<String> interests, String password, boolean online) {
        var profile = users.findByEmail(email).orElseGet(UserProfile::new);
        profile.setFirebaseUid(profile.getFirebaseUid() == null ? "app-" + email.replaceAll("[^a-z0-9]", "") : profile.getFirebaseUid());
        profile.setDisplayName(name);
        profile.setEmail(email);
        profile.setPhoneNumber(phone);
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
}
