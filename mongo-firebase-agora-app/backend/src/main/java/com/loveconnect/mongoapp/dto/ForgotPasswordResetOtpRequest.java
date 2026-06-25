package com.loveconnect.mongoapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForgotPasswordResetOtpRequest(
    String email,
    String identifier,
    @Size(min = 8, max = 80) String newPassword,
    @NotBlank String confirmPassword
) {
}
