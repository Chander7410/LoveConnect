package com.loveconnect.mongoapp.dto;

import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
    @Size(max = 80) String displayName,
    @Size(max = 500) String bio,
    @Size(max = 500) String photoUrl
) {
}
