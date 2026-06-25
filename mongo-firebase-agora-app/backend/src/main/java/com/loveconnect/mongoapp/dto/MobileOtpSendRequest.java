package com.loveconnect.mongoapp.dto;

import jakarta.validation.constraints.NotBlank;

public record MobileOtpSendRequest(@NotBlank String mobileNumber) {
}
