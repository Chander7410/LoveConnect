package com.loveconnect.app.dto;

import com.loveconnect.app.entity.NotificationType;
import java.time.Instant;

public class NotificationResponse {
    private Long id;
    private NotificationType type;
    private String message;
    private boolean read;
    private boolean pushDelivered;
    private boolean emailQueued;
    private Instant createdAt;

    public NotificationResponse() {}
    public NotificationResponse(Long id, NotificationType type, String message, boolean read,
                                boolean pushDelivered, boolean emailQueued, Instant createdAt) {
        this.id = id; this.type = type; this.message = message; this.read = read;
        this.pushDelivered = pushDelivered; this.emailQueued = emailQueued; this.createdAt = createdAt;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public boolean isPushDelivered() { return pushDelivered; }
    public void setPushDelivered(boolean pushDelivered) { this.pushDelivered = pushDelivered; }
    public boolean isEmailQueued() { return emailQueued; }
    public void setEmailQueued(boolean emailQueued) { this.emailQueued = emailQueued; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

