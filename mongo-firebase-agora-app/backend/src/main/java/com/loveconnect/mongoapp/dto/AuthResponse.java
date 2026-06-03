package com.loveconnect.mongoapp.dto;

public record AuthResponse(String token, UserResponse user) {
}
