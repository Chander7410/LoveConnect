package com.loveconnect.app.dto;

import java.util.Set;
import javax.validation.constraints.Size;

public class ProfileRequest {
    @Size(max = 1000) private String bio;
    private String education;
    private String profession;
    private String city;
    private Set<String> interests;

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
}

