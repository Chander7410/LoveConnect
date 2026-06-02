package com.loveconnect.app.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Enumerated(EnumType.STRING)
    private NotificationType type;
    @Column(nullable = false)
    private String message;
    private boolean readNotification;
    private boolean pushDelivered;
    private boolean emailQueued;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public boolean isReadNotification() { return readNotification; }
    public void setReadNotification(boolean readNotification) { this.readNotification = readNotification; }
    public boolean isPushDelivered() { return pushDelivered; }
    public void setPushDelivered(boolean pushDelivered) { this.pushDelivered = pushDelivered; }
    public boolean isEmailQueued() { return emailQueued; }
    public void setEmailQueued(boolean emailQueued) { this.emailQueued = emailQueued; }
}


