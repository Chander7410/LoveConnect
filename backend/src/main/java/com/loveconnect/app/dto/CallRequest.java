package com.loveconnect.app.dto;

import com.loveconnect.app.entity.CallType;
import javax.validation.constraints.NotNull;

public class CallRequest {
    @NotNull private Long receiverId;
    @NotNull private CallType type;

    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }
    public CallType getType() { return type; }
    public void setType(CallType type) { this.type = type; }
}
