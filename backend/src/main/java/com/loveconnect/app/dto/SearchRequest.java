package com.loveconnect.app.dto;

import com.loveconnect.app.entity.Gender;

public class SearchRequest {
    private Integer minAge;
    private Integer maxAge;
    private Gender gender;
    private String city;
    private String interest;

    public SearchRequest() {}
    public SearchRequest(Integer minAge, Integer maxAge, Gender gender, String city, String interest) {
        this.minAge = minAge; this.maxAge = maxAge; this.gender = gender; this.city = city; this.interest = interest;
    }
    public Integer getMinAge() { return minAge; }
    public void setMinAge(Integer minAge) { this.minAge = minAge; }
    public Integer getMaxAge() { return maxAge; }
    public void setMaxAge(Integer maxAge) { this.maxAge = maxAge; }
    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getInterest() { return interest; }
    public void setInterest(String interest) { this.interest = interest; }
}

