package com.loveconnect.app.service;

import com.loveconnect.app.dto.NotificationResponse;
import com.loveconnect.app.entity.Notification;
import com.loveconnect.app.entity.NotificationType;
import com.loveconnect.app.entity.User;
import com.loveconnect.app.repository.NotificationRepository;
import com.loveconnect.app.util.Mapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.notification-service.url:}")
    private String notificationServiceUrl;

    public NotificationService(NotificationRepository notificationRepository, SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public void create(User user, NotificationType type, String message) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setMessage(message);
        notification.setPushDelivered(user.isPushNotificationsEnabled());
        notification.setEmailQueued(user.isEmailNotificationsEnabled());
        Notification saved = notificationRepository.save(notification);
        if (user.isPushNotificationsEnabled()) {
            messagingTemplate.convertAndSendToUser(user.getEmail(), "/queue/notifications", Mapper.notification(saved));
        }
        deliverThroughMicroservice(user, type, message);
    }

    private void deliverThroughMicroservice(User user, NotificationType type, String message) {
        if (notificationServiceUrl == null || notificationServiceUrl.trim().isEmpty()) {
            return;
        }
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", user.getId());
            payload.put("email", user.getEmail());
            payload.put("type", type.name());
            payload.put("message", message);
            payload.put("pushEnabled", user.isPushNotificationsEnabled());
            payload.put("emailEnabled", user.isEmailNotificationsEnabled());
            restTemplate.postForObject(notificationServiceUrl, payload, Map.class);
        } catch (Exception ex) {
            log.warn("Notification microservice unavailable: {}", ex.getMessage());
        }
    }

    public List<NotificationResponse> list(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(Mapper::notification)
                .collect(Collectors.toList());
    }
}


