package com.loveconnect.app.service;

import com.loveconnect.app.dto.AuthResponse;
import com.loveconnect.app.dto.ForgotPasswordResponse;
import com.loveconnect.app.dto.ForgotPasswordRequest;
import com.loveconnect.app.dto.GoogleLoginRequest;
import com.loveconnect.app.dto.LoginRequest;
import com.loveconnect.app.dto.OtpSendRequest;
import com.loveconnect.app.dto.OtpVerifyRequest;
import com.loveconnect.app.dto.RegisterRequest;
import com.loveconnect.app.dto.ResetPasswordRequest;
import com.loveconnect.app.entity.AuthProvider;
import com.loveconnect.app.entity.EmailOtp;
import com.loveconnect.app.entity.Gender;
import com.loveconnect.app.entity.Profile;
import com.loveconnect.app.entity.Role;
import com.loveconnect.app.entity.User;
import com.loveconnect.app.exception.BadRequestException;
import com.loveconnect.app.repository.EmailOtpRepository;
import com.loveconnect.app.repository.UserRepository;
import com.loveconnect.app.security.JwtService;
import com.loveconnect.app.util.Mapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Locale;
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
    private final EmailOtpRepository emailOtpRepository;
    private final EmailService emailService;
    private final GoogleTokenService googleTokenService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, AuthenticationManager authenticationManager,
                       EmailOtpRepository emailOtpRepository, EmailService emailService,
                       GoogleTokenService googleTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.emailOtpRepository = emailOtpRepository;
        this.emailService = emailService;
        this.googleTokenService = googleTokenService;
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
        user.setProvider(AuthProvider.LOCAL);
        user.setEmailVerified(false);
        Profile profile = new Profile();
        profile.setUser(user);
        profile.setCity(request.getLocation());
        user.setProfile(profile);
        user.setOnline(true);
        user.setLastSeenAt(Instant.now());
        userRepository.save(user);
        UserDetails details = org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                .password(user.getPassword()).roles(user.getRole().name()).build();
        return new AuthResponse(jwtService.generateToken(details, false), Mapper.user(user));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid credentials"));
        if (user.getProvider() == AuthProvider.LOCAL && !user.isEmailVerified()) {
            throw new BadRequestException("EMAIL_NOT_VERIFIED");
        }
        markOnline(user);
        return issueToken(user, request.isRememberMe());
    }

    @Transactional
    public AuthResponse googleLogin(GoogleLoginRequest request) {
        GoogleTokenService.GoogleProfile googleProfile = googleTokenService.verify(request.getIdToken());
        User user = userRepository.findByEmail(googleProfile.getEmail())
                .map(existing -> {
                    existing.setEmailVerified(true);
                    if (existing.getProvider() == null) {
                        existing.setProvider(AuthProvider.GOOGLE);
                    }
                    if (googleProfile.getPicture() != null && !"null".equals(googleProfile.getPicture())) {
                        existing.setProfilePictureUrl(googleProfile.getPicture());
                    }
                    return existing;
                })
                .orElseGet(() -> createGoogleUser(googleProfile));
        markOnline(user);
        return issueToken(user, request.isRememberMe());
    }

    @Transactional
    public void sendOtp(OtpSendRequest request) {
        String email = normalizeEmail(request.getEmail());
        Instant oneMinuteAgo = Instant.now().minus(1, ChronoUnit.MINUTES);
        if (emailOtpRepository.countByEmailAndCreatedAtAfter(email, oneMinuteAgo) >= 1) {
            throw new BadRequestException("Please wait before requesting another OTP");
        }
        String otp = String.format(Locale.US, "%06d", secureRandom.nextInt(1_000_000));
        EmailOtp emailOtp = new EmailOtp();
        emailOtp.setEmail(email);
        emailOtp.setOtpHash(hashOtp(email, otp));
        emailOtp.setExpiryTime(Instant.now().plus(5, ChronoUnit.MINUTES));
        emailOtpRepository.save(emailOtp);
        emailService.sendOtp(email, otp);
    }

    @Transactional
    public void verifyOtp(OtpVerifyRequest request) {
        String email = normalizeEmail(request.getEmail());
        EmailOtp emailOtp = emailOtpRepository.findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new BadRequestException("Invalid or expired OTP"));
        if (emailOtp.isVerified() || emailOtp.getExpiryTime().isBefore(Instant.now()) || emailOtp.getAttempts() >= 5) {
            throw new BadRequestException("Invalid or expired OTP");
        }
        emailOtp.setAttempts(emailOtp.getAttempts() + 1);
        if (!hashOtp(email, request.getOtp()).equals(emailOtp.getOtpHash())) {
            emailOtpRepository.save(emailOtp);
            throw new BadRequestException("Invalid or expired OTP");
        }
        emailOtp.setVerified(true);
        emailOtpRepository.save(emailOtp);
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setEmailVerified(true);
            userRepository.save(user);
        });
    }

    @Transactional
    public void markOnline(User user) {
        user.setOnline(true);
        user.setLastSeenAt(Instant.now());
        userRepository.save(user);
    }

    @Transactional
    public void markOffline(User user) {
        user.setOnline(false);
        user.setLastSeenAt(Instant.now());
        userRepository.save(user);
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

    private AuthResponse issueToken(User user, boolean rememberMe) {
        UserDetails details = org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                .password(user.getPassword()).roles(user.getRole().name()).build();
        return new AuthResponse(jwtService.generateToken(details, rememberMe), Mapper.user(user));
    }

    private User createGoogleUser(GoogleTokenService.GoogleProfile googleProfile) {
        User user = new User();
        user.setName(googleProfile.getName());
        user.setEmail(googleProfile.getEmail());
        user.setMobileNumber(uniqueGoogleMobileNumber());
        user.setPassword(passwordEncoder.encode(Base64.getUrlEncoder().withoutPadding()
                .encodeToString(randomBytes(24))));
        user.setGender(Gender.OTHER);
        user.setAge(18);
        user.setLocation("USA");
        user.setRole(Role.USER);
        user.setProvider(AuthProvider.GOOGLE);
        user.setEmailVerified(true);
        if (googleProfile.getPicture() != null && !"null".equals(googleProfile.getPicture())) {
            user.setProfilePictureUrl(googleProfile.getPicture());
        }
        Profile profile = new Profile();
        profile.setUser(user);
        profile.setCity(user.getLocation());
        user.setProfile(profile);
        return userRepository.save(user);
    }

    private byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        secureRandom.nextBytes(bytes);
        return bytes;
    }

    private String uniqueGoogleMobileNumber() {
        String mobile;
        do {
            mobile = "G" + String.format(Locale.US, "%019d", Math.abs(secureRandom.nextLong()));
            if (mobile.length() > 20) {
                mobile = mobile.substring(0, 20);
            }
        } while (userRepository.existsByMobileNumber(mobile));
        return mobile;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.US);
    }

    private String hashOtp(String email, String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((normalizeEmail(email) + ":" + otp).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception ex) {
            throw new BadRequestException("Could not process OTP");
        }
    }
}


