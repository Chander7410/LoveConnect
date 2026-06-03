package com.loveconnect.mongoapp.dto;

import com.loveconnect.mongoapp.model.CallType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CallStartRequest(
    @NotBlank String receiverUid,
    @NotNull CallType type
) {
}
