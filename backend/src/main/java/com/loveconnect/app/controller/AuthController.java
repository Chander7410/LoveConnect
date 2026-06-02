package com.loveconnect.app.controller;

import com.loveconnect.app.dto.ApiMessage;
import com.loveconnect.app.dto.AuthResponse;
import com.loveconnect.app.dto.ForgotPasswordResponse;
import com.loveconnect.app.dto.ForgotPasswordRequest;
import com.loveconnect.app.dto.LoginRequest;
import com.loveconnect.app.dto.RegisterRequest;
import com.loveconnect.app.dto.ResetPasswordRequest;
import com.loveconnect.app.service.AuthService;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/forgot-password")
    public ForgotPasswordResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    public ApiMessage resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return new ApiMessage("Password updated. You can login with the new password.");
    }
}


