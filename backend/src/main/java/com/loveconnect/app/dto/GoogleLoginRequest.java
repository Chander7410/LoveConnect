package com.loveconnect.app.dto;

import javax.validation.constraints.NotBlank;

public class GoogleLoginRequest {
    @NotBlank
    private String idToken;
    private boolean rememberMe;

    public String getIdToken() { return idToken; }
    public void setIdToken(String idToken) { this.idToken = idToken; }
    public boolean isRememberMe() { return rememberMe; }
    public void setRememberMe(boolean rememberMe) { this.rememberMe = rememberMe; }
}
