package com.loveconnect.app.dto;

import com.loveconnect.app.entity.Gender;
import javax.validation.constraints.Email;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class RegisterRequest {
    @NotBlank private String name;
    @Email @NotBlank private String email;
    @Pattern(regexp = "^[0-9+\\- ]{8,20}$") private String mobileNumber;
    @NotNull private Gender gender;
    @Min(18) @Max(100) private Integer age;
    @NotBlank private String location;
    @Size(min = 8, max = 80) private String password;

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
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

