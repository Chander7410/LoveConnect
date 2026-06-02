package com.loveconnect.app.service;

import com.loveconnect.app.dto.AuthResponse;
import com.loveconnect.app.dto.ForgotPasswordResponse;
import com.loveconnect.app.dto.ForgotPasswordRequest;
import com.loveconnect.app.dto.LoginRequest;
import com.loveconnect.app.dto.RegisterRequest;
import com.loveconnect.app.dto.ResetPasswordRequest;
import com.loveconnect.app.entity.Profile;
import com.loveconnect.app.entity.User;
import com.loveconnect.app.exception.BadRequestException;
import com.loveconnect.app.repository.UserRepository;
import com.loveconnect.app.security.JwtService;
import com.loveconnect.app.util.Mapper;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }
        if (userRepository.existsByMobileNumber(request.getMobileNumber())) {
            throw new BadRequestException("Mobile number is already registered");
        }
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail().toLowerCase());
        user.setMobileNumber(request.getMobileNumber());
        user.setGender(request.getGender());
        user.setAge(request.getAge());
        user.setLocation(request.getLocation());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        Profile profile = new Profile();
        profile.setUser(user);
        profile.setCity(request.getLocation());
        user.setProfile(profile);
        userRepository.save(user);
        UserDetails details = org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                .password(user.getPassword()).roles(user.getRole().name()).build();
        return new AuthResponse(jwtService.generateToken(details, false), Mapper.user(user));
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid credentials"));
        UserDetails details = org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                .password(user.getPassword()).roles(user.getRole().name()).build();
        return new AuthResponse(jwtService.generateToken(details, request.isRememberMe()), Mapper.user(user));
    }

    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().toLowerCase();
        return userRepository.findByEmail(email)
                .map(user -> {
                    byte[] tokenBytes = new byte[32];
                    secureRandom.nextBytes(tokenBytes);
                    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
                    user.setPasswordResetToken(token);
                    user.setPasswordResetExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
                    userRepository.save(user);
                    return new ForgotPasswordResponse("Reset token created. Use it below to set a new password.", token);
                })
                .orElseGet(() -> new ForgotPasswordResponse("If the email exists, a reset link will be sent.", null));
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByPasswordResetToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));
        if (user.getPasswordResetExpiresAt() == null || user.getPasswordResetExpiresAt().isBefore(Instant.now())) {
            user.setPasswordResetToken(null);
            user.setPasswordResetExpiresAt(null);
            userRepository.save(user);
            throw new BadRequestException("Invalid or expired reset token");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiresAt(null);
        userRepository.save(user);
    }
}


