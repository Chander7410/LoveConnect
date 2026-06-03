package com.loveconnect.mongoapp.model;

import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("call_sessions")
public class CallSession {
    @Id
    private String id;
    @Indexed
    private String callerUid;
    @Indexed
    private String receiverUid;
    private String channelName;
    private CallType type;
    private CallStatus status;
    @CreatedDate
    private Instant createdAt;
    private Instant endedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCallerUid() { return callerUid; }
    public void setCallerUid(String callerUid) { this.callerUid = callerUid; }
    public String getReceiverUid() { return receiverUid; }
    public void setReceiverUid(String receiverUid) { this.receiverUid = receiverUid; }
    public String getChannelName() { return channelName; }
    public void setChannelName(String channelName) { this.channelName = channelName; }
    public CallType getType() { return type; }
    public void setType(CallType type) { this.type = type; }
    public CallStatus getStatus() { return status; }
    public void setStatus(CallStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }
}
