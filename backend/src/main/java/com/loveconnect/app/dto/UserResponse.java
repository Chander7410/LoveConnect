package com.loveconnect.app.dto;

import com.loveconnect.app.entity.Gender;
import com.loveconnect.app.entity.Role;
import com.loveconnect.app.entity.AuthProvider;

public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private String mobileNumber;
    private Gender gender;
    private Integer age;
    private String location;
    private String profilePictureUrl;
    private Role role;
    private boolean online;
    private boolean blocked;
    private boolean verified;
    private AuthProvider provider;
    private boolean emailVerified;
    private int fakeProfileScore;

    public UserResponse() {}
    public UserResponse(Long id, String name, String email, String mobileNumber, Gender gender, Integer age,
                        String location, String profilePictureUrl, Role role, boolean online, boolean blocked) {
        this(id, name, email, mobileNumber, gender, age, location, profilePictureUrl, role, online, blocked, false, 0);
    }
    public UserResponse(Long id, String name, String email, String mobileNumber, Gender gender, Integer age,
                        String location, String profilePictureUrl, Role role, boolean online, boolean blocked,
                        boolean verified, int fakeProfileScore) {
        this(id, name, email, mobileNumber, gender, age, location, profilePictureUrl, role, online, blocked,
                verified, AuthProvider.LOCAL, true, fakeProfileScore);
    }
    public UserResponse(Long id, String name, String email, String mobileNumber, Gender gender, Integer age,
                        String location, String profilePictureUrl, Role role, boolean online, boolean blocked,
                        boolean verified, AuthProvider provider, boolean emailVerified, int fakeProfileScore) {
        this.id = id; this.name = name; this.email = email; this.mobileNumber = mobileNumber; this.gender = gender;
        this.age = age; this.location = location; this.profilePictureUrl = profilePictureUrl; this.role = role;
        this.online = online; this.blocked = blocked; this.verified = verified; this.provider = provider;
        this.emailVerified = emailVerified; this.fakeProfileScore = fakeProfileScore;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }
    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getProfilePictureUrl() { return profilePictureUrl; }
    public void setProfilePictureUrl(String profilePictureUrl) { this.profilePictureUrl = profilePictureUrl; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }
    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
    public AuthProvider getProvider() { return provider; }
    public void setProvider(AuthProvider provider) { this.provider = provider; }
    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
    public int getFakeProfileScore() { return fakeProfileScore; }
    public void setFakeProfileScore(int fakeProfileScore) { this.fakeProfileScore = fakeProfileScore; }
}

