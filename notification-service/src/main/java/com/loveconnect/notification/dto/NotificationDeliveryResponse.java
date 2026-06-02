package com.loveconnect.notification.dto;

import java.time.Instant;

public class NotificationDeliveryResponse {
    private String status;
    private boolean pushDelivered;
    private boolean emailQueued;
    private Instant deliveredAt;

    public NotificationDeliveryResponse() {}

    public NotificationDeliveryResponse(String status, boolean pushDelivered, boolean emailQueued, Instant deliveredAt) {
        this.status = status;
        this.pushDelivered = pushDelivered;
        this.emailQueued = emailQueued;
        this.deliveredAt = deliveredAt;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isPushDelivered() { return pushDelivered; }
    public void setPushDelivered(boolean pushDelivered) { this.pushDelivered = pushDelivered; }
    public boolean isEmailQueued() { return emailQueued; }
    public void setEmailQueued(boolean emailQueued) { this.emailQueued = emailQueued; }
    public Instant getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(Instant deliveredAt) { this.deliveredAt = deliveredAt; }
}
