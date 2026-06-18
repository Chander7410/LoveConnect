package com.loveconnect.mongoapp.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
    @NotBlank String idToken,
    boolean rememberMe
) {
}
