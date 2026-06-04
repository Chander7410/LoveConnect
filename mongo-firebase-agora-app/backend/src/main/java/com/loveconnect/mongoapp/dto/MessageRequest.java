package com.loveconnect.mongoapp.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MessageRequest(
    @JsonAlias("receiverId")
    @NotBlank String receiverUid,
    @JsonAlias("content")
    @NotBlank @Size(max = 2000) String text
) {
}
