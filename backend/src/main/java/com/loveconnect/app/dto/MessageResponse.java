package com.loveconnect.app.dto;

import java.time.Instant;

public class MessageResponse {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private String content;
    private Instant sentAt;
    private Instant readAt;

    public MessageResponse() {}
    public MessageResponse(Long id, Long senderId, Long receiverId, String content, Instant sentAt, Instant readAt) {
        this.id = id; this.senderId = senderId; this.receiverId = receiverId; this.content = content; this.sentAt = sentAt; this.readAt = readAt;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }
    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
    public Instant getReadAt() { return readAt; }
    public void setReadAt(Instant readAt) { this.readAt = readAt; }
}

