package com.loveconnect.mongoapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompleteSignupRequest(
    @NotBlank String mobileNumber,
    @NotBlank String fullName,
    @Email String email,
    @Size(min = 8, max = 80) String password,
    @NotBlank String confirmPassword,
    @NotBlank String gender,
    @NotBlank String dateOfBirth
) {
}
