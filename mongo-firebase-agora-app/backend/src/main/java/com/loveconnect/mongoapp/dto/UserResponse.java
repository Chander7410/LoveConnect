package com.loveconnect.mongoapp.dto;

import com.loveconnect.mongoapp.model.UserProfile;

public record UserResponse(
    String id,
    String name,
    String email,
    String mobileNumber,
    String gender,
    Integer age,
    String location,
    String profilePictureUrl,
    String role,
    boolean online,
    boolean blocked,
    boolean verified,
    String provider,
    boolean emailVerified,
    int fakeProfileScore
) {
    public static UserResponse from(UserProfile profile) {
        return new UserResponse(
            profile.getId(),
            profile.getDisplayName(),
            profile.getEmail(),
            profile.getPhoneNumber(),
            profile.getGender(),
            profile.getAge(),
            profile.getLocation(),
            profile.getPhotoUrl(),
            profile.getRole(),
            profile.isOnline(),
            profile.isBlocked(),
            profile.isVerified(),
            profile.getProvider(),
            profile.isEmailVerified(),
            profile.getFakeProfileScore()
        );
    }
}
