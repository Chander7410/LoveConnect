package com.loveconnect.app.config;

import com.loveconnect.app.entity.Gender;
import com.loveconnect.app.entity.Profile;
import com.loveconnect.app.entity.Role;
import com.loveconnect.app.entity.User;
import com.loveconnect.app.repository.UserRepository;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@org.springframework.context.annotation.Profile("dev")
public class DemoDataConfig {
    private static final String DEMO_PASSWORD = "Password123!";
    private static final String LIVE_TEST_PASSWORD = "qwerty@123";

    @Bean
    CommandLineRunner seedDemoProfiles(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            createDemoUser(userRepository, passwordEncoder,
                    "Aisha Demo", "demo.aisha@loveconnect.test", "9800000001",
                    Gender.FEMALE, 27, "Pune", "Product designer",
                    "Design school", "Pune", "Coffee walks, music, and weekend travel.",
                    new HashSet<>(Arrays.asList("music", "travel", "coffee")));

            createDemoUser(userRepository, passwordEncoder,
                    "Rahul Demo", "demo.rahul@loveconnect.test", "9800000002",
                    Gender.MALE, 30, "Pune", "Software engineer",
                    "Engineering college", "Pune", "Runner, reader, and live music fan.",
                    new HashSet<>(Arrays.asList("music", "running", "books")));

            createDemoUser(userRepository, passwordEncoder,
                    "Mira Demo", "demo.mira@loveconnect.test", "9800000003",
                    Gender.FEMALE, 29, "Mumbai", "Marketing lead",
                    "Business school", "Mumbai", "Food trails, travel, and photography.",
                    new HashSet<>(Arrays.asList("travel", "food", "photography")));

            createDemoUser(userRepository, passwordEncoder,
                    "roshan", "roshan@gmail.com", "7000000001",
                    Gender.MALE, 18, "pune", "Student",
                    "Live profile", "pune", "Roshan live test profile.",
                    new HashSet<>(Arrays.asList("Car", "music", "travel")), LIVE_TEST_PASSWORD);

            createDemoUser(userRepository, passwordEncoder,
                    "pinky", "pinky@gmail.com", "7000000002",
                    Gender.FEMALE, 18, "Bangalore", "Student",
                    "Live profile", "Bangalore", "Pinky live test profile.",
                    new HashSet<>(Arrays.asList("Car", "music", "travel")), LIVE_TEST_PASSWORD);
        };
    }

    private void createDemoUser(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                String name, String email, String mobileNumber,
                                Gender gender, int age, String location, String profession,
                                String education, String city, String bio, Set<String> interests) {
        createDemoUser(userRepository, passwordEncoder, name, email, mobileNumber, gender, age,
                location, profession, education, city, bio, interests, DEMO_PASSWORD);
    }

    private void createDemoUser(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                String name, String email, String mobileNumber,
                                Gender gender, int age, String location, String profession,
                                String education, String city, String bio, Set<String> interests,
                                String password) {
        if (userRepository.existsByEmail(email)) {
            userRepository.findByEmail(email).ifPresent(existing -> {
                existing.setPassword(passwordEncoder.encode(password));
                userRepository.save(existing);
            });
            return;
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setMobileNumber(mobileNumber);
        user.setPassword(passwordEncoder.encode(password));
        user.setGender(gender);
        user.setAge(age);
        user.setLocation(location);
        user.setRole(Role.USER);
        user.setEnabled(true);
        user.setVerified(true);
        user.setFakeProfileScore(10);

        Profile profile = new Profile();
        profile.setUser(user);
        profile.setBio(bio);
        profile.setEducation(education);
        profile.setProfession(profession);
        profile.setCity(city);
        profile.setInterests(interests);
        user.setProfile(profile);

        userRepository.save(user);
    }
}
