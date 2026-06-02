package com.loveconnect.notification.controller;

import com.loveconnect.notification.dto.NotificationDeliveryRequest;
import com.loveconnect.notification.dto.NotificationDeliveryResponse;
import com.loveconnect.notification.service.NotificationDeliveryService;
import java.util.HashMap;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/micro/notifications")
public class NotificationDeliveryController {
    private final NotificationDeliveryService deliveryService;

    public NotificationDeliveryController(NotificationDeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> response = new HashMap<>();
        response.put("service", "notification-service");
        response.put("status", "UP");
        return response;
    }

    @PostMapping("/deliver")
    public NotificationDeliveryResponse deliver(@Valid @RequestBody NotificationDeliveryRequest request) {
        return deliveryService.deliver(request);
    }
}
