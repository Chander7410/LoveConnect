package com.loveconnect.mongoapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MessageRequest(
    @NotBlank String receiverUid,
    @NotBlank @Size(max = 2000) String text
) {
}
