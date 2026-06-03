package com.loveconnect.mongoapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank String name,
    @Email @NotBlank String email,
    @Pattern(regexp = "^[0-9+\\- ]{8,20}$") String mobileNumber,
    @NotBlank String gender,
    @NotNull @Min(18) @Max(100) Integer age,
    @NotBlank String location,
    @Size(min = 8, max = 80) String password
) {
}
