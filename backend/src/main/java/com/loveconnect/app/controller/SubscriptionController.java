package com.loveconnect.app.controller;

import com.loveconnect.app.dto.SubscriptionRequest;
import com.loveconnect.app.entity.Subscription;
import com.loveconnect.app.service.CurrentUserService;
import com.loveconnect.app.service.SubscriptionService;
import javax.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {
    private final CurrentUserService currentUserService;
    private final SubscriptionService subscriptionService;

    public SubscriptionController(CurrentUserService currentUserService, SubscriptionService subscriptionService) {
        this.currentUserService = currentUserService;
        this.subscriptionService = subscriptionService;
    }

    @PostMapping
    public Subscription subscribe(Authentication authentication, @Valid @RequestBody SubscriptionRequest request) {
        return subscriptionService.subscribe(currentUserService.get(authentication), request);
    }

    @GetMapping
    public List<Subscription> history(Authentication authentication) {
        return subscriptionService.history(currentUserService.get(authentication).getId());
    }
}


