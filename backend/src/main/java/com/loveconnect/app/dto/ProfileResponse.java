package com.loveconnect.app.dto;

import java.util.List;
import java.util.Set;

public class ProfileResponse {
    private Long id;
    private UserResponse user;
    private String bio;
    private String education;
    private String profession;
    private String city;
    private Set<String> interests;
    private List<String> photoUrls;

    public ProfileResponse() {}
    public ProfileResponse(Long id, UserResponse user, String bio, String education, String profession,
                           String city, Set<String> interests, List<String> photoUrls) {
        this.id = id; this.user = user; this.bio = bio; this.education = education; this.profession = profession;
        this.city = city; this.interests = interests; this.photoUrls = photoUrls;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UserResponse getUser() { return user; }
    public void setUser(UserResponse user) { this.user = user; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getEducation() { return education; }
    public void setEducation(String education) { this.education = education; }
    public String getProfession() { return profession; }
    public void setProfession(String profession) { this.profession = profession; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public Set<String> getInterests() { return interests; }
    public void setInterests(Set<String> interests) { this.interests = interests; }
    public List<String> getPhotoUrls() { return photoUrls; }
    public void setPhotoUrls(List<String> photoUrls) { this.photoUrls = photoUrls; }
}

