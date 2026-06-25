package com.loveconnect.mongoapp.dto;

import com.loveconnect.mongoapp.model.OtpPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ResendOtpRequest(
    @NotBlank String mobileNumber,
    @NotNull OtpPurpose purpose
) {
}
