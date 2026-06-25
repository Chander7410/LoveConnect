package com.loveconnect.mongoapp.dto;

public record SignupEmailOtpRequest(
    String name,
    String fullName,
    String mobile,
    String dob,
    String email,
    String password,
    String confirmPassword
) {
    public String resolvedName() {
        return fullName != null && !fullName.isBlank() ? fullName : name;
    }
}
