package com.loveconnect.mongoapp.model;

import java.time.Duration;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("call_history")
public class CallHistory {
    @Id
    private String id;
    @Indexed
    private String callerId;
    @Indexed
    private String receiverId;
    private CallType callType;
    private CallHistoryStatus status;
    private Instant startTime;
    private Instant endTime;
    private Long duration;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCallerId() { return callerId; }
    public void setCallerId(String callerId) { this.callerId = callerId; }
    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }
    public CallType getCallType() { return callType; }
    public void setCallType(CallType callType) { this.callType = callType; }
    public CallHistoryStatus getStatus() { return status; }
    public void setStatus(CallHistoryStatus status) { this.status = status; }
    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }
    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }
    public Long getDuration() { return duration; }
    public void setDuration(Long duration) { this.duration = duration; }

    public void finish(CallHistoryStatus nextStatus) {
        this.status = nextStatus;
        this.endTime = Instant.now();
        if (startTime != null) {
            this.duration = Math.max(0, Duration.between(startTime, endTime).getSeconds());
        }
    }
}
