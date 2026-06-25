package com.loveconnect.mongoapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginOtpVerifyRequest(
    @NotBlank String identifier,
    @Pattern(regexp = "^[0-9]{6}$") String otp
) {
}
