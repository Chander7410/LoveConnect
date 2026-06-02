package com.loveconnect.app.dto;

import com.loveconnect.app.entity.CallStatus;
import com.loveconnect.app.entity.CallType;
import java.time.Instant;

public class CallResponse {
    private Long id;
    private UserResponse caller;
    private UserResponse receiver;
    private CallType type;
    private CallStatus status;
    private Instant startedAt;
    private Instant endedAt;
    private Long durationSeconds;

    public CallResponse() {}
    public CallResponse(Long id, UserResponse caller, UserResponse receiver, CallType type, CallStatus status,
                        Instant startedAt, Instant endedAt, Long durationSeconds) {
        this.id = id; this.caller = caller; this.receiver = receiver; this.type = type; this.status = status;
        this.startedAt = startedAt; this.endedAt = endedAt; this.durationSeconds = durationSeconds;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UserResponse getCaller() { return caller; }
    public void setCaller(UserResponse caller) { this.caller = caller; }
    public UserResponse getReceiver() { return receiver; }
    public void setReceiver(UserResponse receiver) { this.receiver = receiver; }
    public CallType getType() { return type; }
    public void setType(CallType type) { this.type = type; }
    public CallStatus getStatus() { return status; }
    public void setStatus(CallStatus status) { this.status = status; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }
    public Long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Long durationSeconds) { this.durationSeconds = durationSeconds; }
}
