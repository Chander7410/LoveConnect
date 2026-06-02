package com.loveconnect.notification.service;

import com.loveconnect.notification.dto.NotificationDeliveryRequest;
import com.loveconnect.notification.dto.NotificationDeliveryResponse;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationDeliveryService {
    private static final Logger log = LoggerFactory.getLogger(NotificationDeliveryService.class);

    public NotificationDeliveryResponse deliver(NotificationDeliveryRequest request) {
        boolean pushDelivered = request.isPushEnabled();
        boolean emailQueued = request.isEmailEnabled();
        log.info("Deliver notification userId={} email={} type={} push={} email={} message={}",
                request.getUserId(), request.getEmail(), request.getType(), pushDelivered, emailQueued, request.getMessage());
        return new NotificationDeliveryResponse("DELIVERED", pushDelivered, emailQueued, Instant.now());
    }
}
