package com.loveconnect.app.dto;

public class ForgotPasswordResponse extends ApiMessage {
    private String resetToken;

    public ForgotPasswordResponse() {}

    public ForgotPasswordResponse(String message, String resetToken) {
        super(message);
        this.resetToken = resetToken;
    }

    public String getResetToken() { return resetToken; }
    public void setResetToken(String resetToken) { this.resetToken = resetToken; }
}
