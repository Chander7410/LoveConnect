package com.loveconnect.mongoapp.service;

import com.loveconnect.mongoapp.dto.AuthResponse;
import com.loveconnect.mongoapp.dto.CompleteSignupRequest;
import com.loveconnect.mongoapp.dto.ForgotPasswordOtpRequest;
import com.loveconnect.mongoapp.dto.ForgotPasswordOtpVerifyRequest;
import com.loveconnect.mongoapp.dto.ForgotPasswordResetOtpRequest;
import com.loveconnect.mongoapp.dto.GoogleLoginRequest;
import com.loveconnect.mongoapp.dto.LoginOtpVerifyRequest;
import com.loveconnect.mongoapp.dto.LoginRequest;
import com.loveconnect.mongoapp.dto.MobileOtpSendRequest;
import com.loveconnect.mongoapp.dto.MobileOtpVerifyRequest;
import com.loveconnect.mongoapp.dto.OtpSendRequest;
import com.loveconnect.mongoapp.dto.OtpVerifyRequest;
import com.loveconnect.mongoapp.dto.RegisterRequest;
import com.loveconnect.mongoapp.dto.ResendOtpRequest;
import com.loveconnect.mongoapp.dto.SignupEmailOtpRequest;
import com.loveconnect.mongoapp.dto.SignupEmailOtpVerifyRequest;
import com.loveconnect.mongoapp.dto.UserResponse;
import com.loveconnect.mongoapp.model.EmailOtp;
import com.loveconnect.mongoapp.model.EmailOtpPurpose;
import com.loveconnect.mongoapp.model.MobileOtp;
import com.loveconnect.mongoapp.model.OtpPurpose;
import com.loveconnect.mongoapp.model.PendingSignup;
import com.loveconnect.mongoapp.model.UserProfile;
import com.loveconnect.mongoapp.repository.EmailOtpRepository;
import com.loveconnect.mongoapp.repository.MobileOtpRepository;
import com.loveconnect.mongoapp.repository.PendingSignupRepository;
import com.loveconnect.mongoapp.repository.UserProfileRepository;
import com.loveconnect.mongoapp.security.AppTokenService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AppAuthService {
    private final UserProfileRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AppTokenService tokens;
    private final EmailOtpRepository otps;
    private final MobileOtpRepository mobileOtps;
    private final PendingSignupRepository pendingSignups;
    private final EmailService emails;
    private final SmsService sms;
    private final GoogleTokenService googleTokens;
    private final boolean exposeDevOtp;
    private final String smsProvider;
    private final SecureRandom secureRandom = new SecureRandom();

    public AppAuthService(UserProfileRepository users, PasswordEncoder passwordEncoder, AppTokenService tokens,
                          EmailOtpRepository otps, MobileOtpRepository mobileOtps, EmailService emails,
                          PendingSignupRepository pendingSignups, SmsService sms, GoogleTokenService googleTokens,
                          @Value("${app.otp.expose-dev-response:false}") boolean exposeDevOtp,
                          @Value("${app.sms.provider:CONSOLE}") String smsProvider) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.otps = otps;
        this.mobileOtps = mobileOtps;
        this.pendingSignups = pendingSignups;
        this.emails = emails;
        this.sms = sms;
        this.googleTokens = googleTokens;
        this.exposeDevOtp = exposeDevOtp;
        this.smsProvider = smsProvider == null ? "" : smsProvider;
    }

    public AuthResponse register(RegisterRequest request) {
        var email = request.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }
        if (users.existsByPhoneNumber(request.mobileNumber())) {
            throw new IllegalArgumentException("Mobile number is already registered");
        }

        var user = new UserProfile();
        user.setFirebaseUid("app-" + email.replaceAll("[^a-z0-9]", ""));
        user.setDisplayName(request.name().trim());
        user.setEmail(email);
        user.setPhoneNumber(request.mobileNumber().trim());
        user.setGender(request.gender().trim().toUpperCase());
        user.setAge(request.age());
        user.setLocation(request.location().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setOnline(true);
        user.setVerified(true);
        user.setProvider("LOCAL");
        user.setEmailVerified(false);
        user.setMobileVerified(true);
        user.setProfileCompleted(true);
        user.setLastSeenAt(Instant.now());
        user.setRole("USER");
        var saved = users.save(user);
        return new AuthResponse(tokens.create(saved.getFirebaseUid()), UserResponse.from(saved));
    }

    public AuthResponse login(LoginRequest request) {
        var email = normalizeEmail(request.email());
        var user = findUserByEmailForAuth(email);
        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        user.setOnline(true);
        user.setLastSeenAt(Instant.now());
        var saved = users.save(user);
        return new AuthResponse(tokens.create(saved.getFirebaseUid()), UserResponse.from(saved));
    }

    public AuthResponse verifyLoginOtp(LoginOtpVerifyRequest request) {
        var user = findUserByIdentifier(request.identifier());
        verifyMobileOtp(normalizeMobile(user.getPhoneNumber()), request.otp(), OtpPurpose.LOGIN, true);
        user.setOnline(true);
        user.setLastSeenAt(Instant.now());
        var saved = users.save(user);
        return new AuthResponse(tokens.create(saved.getFirebaseUid()), UserResponse.from(saved));
    }

    public String sendSignupEmailOtp(SignupEmailOtpRequest request) {
        var email = normalizeAndValidateEmail(request.email());
        var name = requireText(request.resolvedName(), "Full Name is required");
        var mobile = normalizeOptionalTenDigitMobile(request.mobile());
        var dob = parseOptionalDateOfBirth(request.dob());
        if (dob != null) {
            validateAdult(dob);
        }
        validateSignupPassword(request.password(), request.confirmPassword());
        if (users.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }
        if (!mobile.isBlank() && users.existsByPhoneNumber(mobile)) {
            throw new IllegalArgumentException("Mobile number is already registered");
        }

        var pending = pendingSignups.findByEmail(email).orElseGet(PendingSignup::new);
        pending.setName(name);
        pending.setMobile(mobile);
        pending.setDob(dob);
        pending.setEmail(email);
        pending.setPasswordHash(passwordEncoder.encode(request.password()));
        pending.setExpiryTime(Instant.now().plus(5, ChronoUnit.MINUTES));
        pendingSignups.save(pending);
        return createAndSendEmailOtp(email, EmailOtpPurpose.SIGNUP);
    }

    public String resendSignupEmailOtp(String emailValue) {
        var email = normalizeAndValidateEmail(emailValue);
        var pending = pendingSignups.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Signup session expired. Please start signup again."));
        if (pending.getExpiryTime() == null || pending.getExpiryTime().isBefore(Instant.now())) {
            pendingSignups.deleteByEmail(email);
            throw new IllegalArgumentException("Signup session expired. Please start signup again.");
        }
        return createAndSendEmailOtp(email, EmailOtpPurpose.SIGNUP);
    }

    public void verifySignupEmailOtp(SignupEmailOtpVerifyRequest request) {
        var email = normalizeAndValidateEmail(request.email());
        verifyEmailOtp(email, request.otp(), EmailOtpPurpose.SIGNUP, true);
        var pending = pendingSignups.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Signup session expired. Please send OTP again."));
        if (pending.getExpiryTime() == null || pending.getExpiryTime().isBefore(Instant.now())) {
            pendingSignups.deleteByEmail(email);
            throw new IllegalArgumentException("Signup session expired. Please send OTP again.");
        }
        if (users.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }
        if (pending.getMobile() != null && !pending.getMobile().isBlank() && users.existsByPhoneNumber(pending.getMobile())) {
            throw new IllegalArgumentException("Mobile number is already registered");
        }

        var age = pending.getDob() == null ? 18 : Period.between(pending.getDob(), LocalDate.now()).getYears();
        var user = new UserProfile();
        user.setFirebaseUid("email-" + email.replaceAll("[^a-z0-9]", ""));
        user.setDisplayName(pending.getName());
        user.setEmail(email);
        user.setPhoneNumber(pending.getMobile() == null || pending.getMobile().isBlank() ? uniqueEmailPhone(email) : pending.getMobile());
        user.setDateOfBirth(pending.getDob());
        user.setAge(age);
        user.setGender("OTHER");
        user.setLocation("USA");
        user.setPasswordHash(pending.getPasswordHash());
        user.setProvider("LOCAL");
        user.setEmailVerified(true);
        user.setMobileVerified(false);
        user.setProfileCompleted(true);
        user.setVerified(true);
        user.setRole("USER");
        user.setOnline(false);
        users.save(user);
        pendingSignups.deleteByEmail(email);
    }

    public String sendSignupOtp(MobileOtpSendRequest request) {
        var mobile = normalizeAndValidateMobile(request.mobileNumber());
        if (users.existsByPhoneNumber(mobile)) {
            throw new IllegalArgumentException("Mobile number is already registered");
        }
        return createAndSendMobileOtp(mobile, OtpPurpose.SIGNUP);
    }

    public void verifySignupOtp(MobileOtpVerifyRequest request) {
        verifyMobileOtp(normalizeAndValidateMobile(request.mobileNumber()), request.otp(), OtpPurpose.SIGNUP, true);
    }

    public AuthResponse completeSignup(CompleteSignupRequest request) {
        var mobile = normalizeAndValidateMobile(request.mobileNumber());
        requireVerifiedMobileOtp(mobile, OtpPurpose.SIGNUP);
        if (users.existsByPhoneNumber(mobile)) {
            throw new IllegalArgumentException("Mobile number is already registered");
        }
        var email = normalizeOptionalEmail(request.email());
        if (!email.isBlank() && users.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }
        if (!request.password().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        validateStrongPassword(request.password());
        var dob = parseDateOfBirth(request.dateOfBirth());
        var age = Period.between(dob, LocalDate.now()).getYears();
        if (age < 18) {
            throw new IllegalArgumentException("Age must be at least 18");
        }

        var user = new UserProfile();
        user.setFirebaseUid("mobile-" + mobile.replaceAll("[^0-9A-Za-z]", ""));
        user.setDisplayName(request.fullName().trim());
        user.setEmail(email.isBlank() ? null : email);
        user.setPhoneNumber(mobile);
        user.setGender(request.gender().trim().toUpperCase(Locale.US));
        user.setDateOfBirth(dob);
        user.setAge(age);
        user.setLocation("USA");
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setProvider("LOCAL");
        user.setEmailVerified(email.isBlank());
        user.setMobileVerified(true);
        user.setProfileCompleted(true);
        user.setVerified(true);
        user.setRole("USER");
        user.setOnline(true);
        user.setLastSeenAt(Instant.now());
        var saved = users.save(user);
        return new AuthResponse(tokens.create(saved.getFirebaseUid()), UserResponse.from(saved));
    }

    public String sendForgotPasswordOtp(ForgotPasswordOtpRequest request) {
        var email = emailFromForgotRequest(request.email(), request.identifier());
        findUserByEmailOrThrow(email, "Email is not registered");
        return createAndSendEmailOtp(email, EmailOtpPurpose.FORGOT_PASSWORD);
    }

    public void verifyForgotPasswordOtp(ForgotPasswordOtpVerifyRequest request) {
        var email = emailFromForgotRequest(request.email(), request.identifier());
        var user = findUserByEmailOrThrow(email, "Email is not registered");
        verifyEmailOtp(email, request.otp(), EmailOtpPurpose.FORGOT_PASSWORD, true);
        user.setPasswordResetToken("EMAIL_OTP_VERIFIED");
        user.setPasswordResetExpiresAt(Instant.now().plus(5, ChronoUnit.MINUTES));
        users.save(user);
    }

    public void resetForgotPassword(ForgotPasswordResetOtpRequest request) {
        var email = emailFromForgotRequest(request.email(), request.identifier());
        var user = findUserByEmailOrThrow(email, "Email is not registered");
        if (!"EMAIL_OTP_VERIFIED".equals(user.getPasswordResetToken())
            || user.getPasswordResetExpiresAt() == null
            || user.getPasswordResetExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("OTP verification required");
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        validateStrongPassword(request.newPassword());
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiresAt(null);
        users.save(user);
    }

    public String resendMobileOtp(ResendOtpRequest request) {
        var mobile = normalizeAndValidateMobile(request.mobileNumber());
        if (request.purpose() == OtpPurpose.SIGNUP && users.existsByPhoneNumber(mobile)) {
            throw new IllegalArgumentException("Mobile number is already registered");
        }
        return createAndSendMobileOtp(mobile, request.purpose());
    }

    public AuthResponse googleLogin(GoogleLoginRequest request) {
        var googleProfile = googleTokens.verify(request.idToken());
        var user = users.findAllByEmail(googleProfile.email()).stream()
            .findFirst()
            .map(existing -> {
                if (existing.getProvider() == null || existing.getProvider().isBlank()) {
                    existing.setProvider("GOOGLE");
                }
                if (googleProfile.picture() != null && !googleProfile.picture().isBlank()) {
                    existing.setPhotoUrl(googleProfile.picture());
                }
                return existing;
            })
            .orElseGet(() -> createGoogleUser(googleProfile));
        user.setProvider("GOOGLE");
        user.setEmailVerified(false);
        users.save(user);
        throw new IllegalArgumentException("EMAIL_NOT_VERIFIED");
    }

    public void sendOtp(OtpSendRequest request) {
        createAndSendEmailOtp(normalizeAndValidateEmail(request.email()), EmailOtpPurpose.EMAIL_VERIFY);
    }

    public AuthResponse verifyOtp(OtpVerifyRequest request) {
        var email = normalizeAndValidateEmail(request.email());
        verifyEmailOtp(email, request.otp(), EmailOtpPurpose.EMAIL_VERIFY, true);
        var user = newestFirst(users.findAllByEmail(email)).stream()
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("User not found for verified email"));
        user.setEmailVerified(true);
        user.setOnline(true);
        user.setLastSeenAt(Instant.now());
        var saved = users.save(user);
        return new AuthResponse(tokens.create(saved.getFirebaseUid()), UserResponse.from(saved));
    }

    private List<UserProfile> newestFirst(List<UserProfile> profiles) {
        return profiles.stream()
            .sorted(Comparator.comparing(
                UserProfile::getUpdatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())
            ))
            .toList();
    }

    private String createAndSendEmailOtp(String email, EmailOtpPurpose purpose) {
        var now = Instant.now();
        if (otps.countByEmailAndPurposeAndCreatedAtAfter(email, purpose, now.minus(60, ChronoUnit.SECONDS)) >= 1) {
            throw new IllegalArgumentException("Please wait 60 seconds before requesting another OTP");
        }
        var otp = String.format(Locale.US, "%06d", secureRandom.nextInt(1_000_000));
        otps.deleteByEmailAndPurpose(email, purpose);
        var emailOtp = new EmailOtp();
        emailOtp.setEmail(email);
        emailOtp.setPurpose(purpose);
        emailOtp.setOtpHash(hashEmailOtp(email, purpose, otp));
        emailOtp.setExpiryTime(now.plus(5, ChronoUnit.MINUTES));
        emailOtp.setCreatedAt(now);
        otps.save(emailOtp);
        try {
            emails.sendOtp(email, otp);
        } catch (RuntimeException ex) {
            otps.deleteByEmailAndPurpose(email, purpose);
            throw ex;
        }
        return shouldExposeDevOtp() ? otp : null;
    }

    private void verifyEmailOtp(String email, String otp, EmailOtpPurpose purpose, boolean deleteAfterSuccess) {
        var emailOtp = otps.findTopByEmailAndPurposeOrderByCreatedAtDesc(email, purpose)
            .orElseThrow(() -> new IllegalArgumentException("Invalid or expired OTP"));
        if (emailOtp.isVerified() || emailOtp.getExpiryTime() == null
            || emailOtp.getExpiryTime().isBefore(Instant.now()) || emailOtp.getAttempts() >= 3) {
            throw new IllegalArgumentException("Invalid or expired OTP");
        }
        emailOtp.setAttempts(emailOtp.getAttempts() + 1);
        if (!hashEmailOtp(email, purpose, otp).equals(emailOtp.getOtpHash())) {
            otps.save(emailOtp);
            throw new IllegalArgumentException("Invalid or expired OTP");
        }
        if (deleteAfterSuccess) {
            otps.deleteByEmailAndPurpose(email, purpose);
        } else {
            emailOtp.setVerified(true);
            otps.save(emailOtp);
        }
    }

    private String createAndSendMobileOtp(String mobile, OtpPurpose purpose) {
        if (mobileOtps.countByMobileNumberAndPurposeAndCreatedAtAfter(mobile, purpose, Instant.now().minus(1, ChronoUnit.MINUTES)) >= 1) {
            throw new IllegalArgumentException("Please wait before requesting another OTP");
        }
        if (mobileOtps.countByMobileNumberAndPurposeAndCreatedAtAfter(mobile, purpose, Instant.now().minus(15, ChronoUnit.MINUTES)) >= 5) {
            throw new IllegalArgumentException("Too many OTP requests. Please try again later");
        }
        var otp = String.format(Locale.US, "%06d", secureRandom.nextInt(1_000_000));
        var mobileOtp = new MobileOtp();
        mobileOtp.setMobileNumber(mobile);
        mobileOtp.setPurpose(purpose);
        mobileOtp.setOtpHash(hashMobileOtp(mobile, purpose, otp));
        mobileOtp.setExpiryTime(Instant.now().plus(5, ChronoUnit.MINUTES));
        mobileOtps.save(mobileOtp);
        sms.sendOtp(mobile, otp, purpose.name());
        return shouldExposeDevOtp() ? otp : null;
    }

    private boolean shouldExposeDevOtp() {
        return exposeDevOtp && "CONSOLE".equalsIgnoreCase(smsProvider);
    }

    private void verifyMobileOtp(String mobile, String otp, OtpPurpose purpose, boolean markVerified) {
        var mobileOtp = mobileOtps.findTopByMobileNumberAndPurposeOrderByCreatedAtDesc(mobile, purpose)
            .orElseThrow(() -> new IllegalArgumentException("Invalid or expired OTP"));
        if (mobileOtp.getExpiryTime() == null || mobileOtp.getExpiryTime().isBefore(Instant.now()) || mobileOtp.getAttempts() >= 5) {
            throw new IllegalArgumentException("Invalid or expired OTP");
        }
        mobileOtp.setAttempts(mobileOtp.getAttempts() + 1);
        if (!hashMobileOtp(mobile, purpose, otp).equals(mobileOtp.getOtpHash())) {
            mobileOtps.save(mobileOtp);
            throw new IllegalArgumentException("Invalid or expired OTP");
        }
        if (markVerified) {
            mobileOtp.setVerified(true);
        }
        mobileOtps.save(mobileOtp);
    }

    private void requireVerifiedMobileOtp(String mobile, OtpPurpose purpose) {
        var mobileOtp = mobileOtps.findTopByMobileNumberAndPurposeOrderByCreatedAtDesc(mobile, purpose)
            .orElseThrow(() -> new IllegalArgumentException("OTP verification required"));
        if (!mobileOtp.isVerified() || mobileOtp.getExpiryTime() == null || mobileOtp.getExpiryTime().isBefore(Instant.now())) {
            throw new IllegalArgumentException("OTP verification required");
        }
    }

    private UserProfile findUserByIdentifier(String identifier) {
        var value = identifier == null ? "" : identifier.trim();
        if (value.contains("@")) {
            return newestFirst(users.findAllByEmail(normalizeEmail(value))).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        }
        var mobile = normalizeMobile(value);
        return users.findByPhoneNumber(mobile)
            .or(() -> users.findAll().stream()
                .filter(user -> normalizeMobile(user.getPhoneNumber()).equals(mobile))
                .findFirst())
            .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
    }

    private UserProfile findUserByEmailForAuth(String email) {
        return findUserByEmailOrThrow(email, "Invalid email or password");
    }

    private UserProfile findUserByEmailOrThrow(String email, String message) {
        return newestFirst(users.findAllByEmail(normalizeAndValidateEmail(email))).stream()
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(message));
    }

    private String emailFromForgotRequest(String email, String identifier) {
        var value = email != null && !email.isBlank() ? email : identifier;
        return normalizeAndValidateEmail(value);
    }

    private String normalizeAndValidateEmail(String email) {
        var normalized = normalizeEmail(email);
        if (normalized.isBlank() || !normalized.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("Invalid email");
        }
        return normalized;
    }

    private String normalizeAndValidateTenDigitMobile(String mobile) {
        var normalized = mobile == null ? "" : mobile.replaceAll("[^0-9]", "");
        if (!normalized.matches("^[0-9]{10}$")) {
            throw new IllegalArgumentException("Mobile number must be 10 digits");
        }
        return normalized;
    }

    private String normalizeOptionalTenDigitMobile(String mobile) {
        if (mobile == null || mobile.isBlank()) {
            return "";
        }
        return normalizeAndValidateTenDigitMobile(mobile);
    }

    private String requireText(String value, String message) {
        if (value == null || value.trim().isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private void validateSignupPassword(String password, String confirmPassword) {
        if (password == null || !password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        validateStrongPassword(password);
    }

    private void validateAdult(LocalDate dob) {
        if (Period.between(dob, LocalDate.now()).getYears() < 18) {
            throw new IllegalArgumentException("User must be at least 18 years old");
        }
    }

    private String normalizeAndValidateMobile(String mobile) {
        var normalized = normalizeMobile(mobile);
        if (!normalized.matches("^\\+?[0-9]{8,15}$")) {
            throw new IllegalArgumentException("Invalid mobile number");
        }
        return normalized;
    }

    private String normalizeMobile(String mobile) {
        if (mobile == null) {
            return "";
        }
        var trimmed = mobile.trim();
        var prefix = trimmed.startsWith("+") ? "+" : "";
        return prefix + trimmed.replaceAll("[^0-9]", "");
    }

    private String normalizeOptionalEmail(String email) {
        var normalized = normalizeEmail(email);
        if (!normalized.isBlank() && !normalized.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("Invalid email");
        }
        return normalized;
    }

    private void validateStrongPassword(String password) {
        if (password == null || password.length() < 8
            || !password.matches(".*[A-Za-z].*")
            || !password.matches(".*[0-9].*")) {
            throw new IllegalArgumentException("Password must be at least 8 characters and include letters and numbers");
        }
    }

    private LocalDate parseDateOfBirth(String value) {
        try {
            return LocalDate.parse(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid date of birth");
        }
    }

    private LocalDate parseOptionalDateOfBirth(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return parseDateOfBirth(value);
    }

    private String maskMobile(String mobile) {
        if (mobile.length() <= 4) {
            return "****";
        }
        return "*".repeat(Math.max(0, mobile.length() - 4)) + mobile.substring(mobile.length() - 4);
    }

    private UserProfile createGoogleUser(GoogleTokenService.GoogleProfile googleProfile) {
        var user = new UserProfile();
        user.setFirebaseUid("google-" + googleProfile.email().replaceAll("[^a-z0-9]", ""));
        user.setDisplayName(googleProfile.name());
        user.setEmail(googleProfile.email());
        user.setPhoneNumber(uniqueGooglePhone());
        user.setPasswordHash(passwordEncoder.encode(randomToken()));
        user.setGender("OTHER");
        user.setAge(18);
        user.setLocation("USA");
        user.setRole("USER");
        user.setProvider("GOOGLE");
        user.setEmailVerified(false);
        user.setVerified(true);
        user.setPhotoUrl(googleProfile.picture());
        return user;
    }

    private String uniqueGooglePhone() {
        String phone;
        do {
            phone = "G" + String.format(Locale.US, "%019d", Math.abs(secureRandom.nextLong()));
            if (phone.length() > 20) {
                phone = phone.substring(0, 20);
            }
        } while (users.existsByPhoneNumber(phone));
        return phone;
    }

    private String uniqueEmailPhone(String email) {
        var base = "E" + HexFormat.of().formatHex(normalizeEmail(email).getBytes(StandardCharsets.UTF_8));
        if (base.length() > 20) {
            base = base.substring(0, 20);
        }
        var candidate = base;
        while (users.existsByPhoneNumber(candidate)) {
            candidate = "E" + String.format(Locale.US, "%019d", Math.abs(secureRandom.nextLong()));
            if (candidate.length() > 20) {
                candidate = candidate.substring(0, 20);
            }
        }
        return candidate;
    }

    private String randomToken() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.US);
    }

    private String hashOtp(String email, String otp) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var hash = digest.digest((normalizeEmail(email) + ":" + otp).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Could not process OTP");
        }
    }

    private String hashEmailOtp(String email, EmailOtpPurpose purpose, String otp) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var hash = digest.digest((normalizeEmail(email) + ":" + purpose.name() + ":" + otp).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Could not process OTP");
        }
    }

    private String hashMobileOtp(String mobile, OtpPurpose purpose, String otp) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var hash = digest.digest((normalizeMobile(mobile) + ":" + purpose.name() + ":" + otp).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Could not process OTP");
        }
    }
}
