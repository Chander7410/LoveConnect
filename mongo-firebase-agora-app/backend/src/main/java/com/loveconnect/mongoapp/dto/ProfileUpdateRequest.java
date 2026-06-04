package com.loveconnect.mongoapp.dto;

import jakarta.validation.constraints.Size;
import java.util.List;

public record ProfileUpdateRequest(
    @Size(max = 80) String displayName,
    @Size(max = 500) String bio,
    @Size(max = 500) String photoUrl,
    @Size(max = 120) String education,
    @Size(max = 120) String profession,
    @Size(max = 120) String city,
    List<String> interests
) {
}
