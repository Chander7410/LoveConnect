package com.loveconnect.app.service;

import com.loveconnect.app.dto.SubscriptionRequest;
import com.loveconnect.app.entity.PlanType;
import com.loveconnect.app.entity.Subscription;
import com.loveconnect.app.entity.SubscriptionStatus;
import com.loveconnect.app.entity.User;
import com.loveconnect.app.repository.SubscriptionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    public Subscription subscribe(User user, SubscriptionRequest request) {
        Subscription subscription = new Subscription();
        subscription.setUser(user);
        subscription.setPlanType(request.getPlanType());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setAmount(request.getAmount() == null ? defaultAmount(request.getPlanType()) : request.getAmount());
        subscription.setPaymentReference(request.getPaymentReference());
        subscription.setStartDate(LocalDate.now());
        subscription.setEndDate(request.getPlanType() == PlanType.PREMIUM ? LocalDate.now().plusMonths(1) : LocalDate.now().plusYears(100));
        return subscriptionRepository.save(subscription);
    }

    public List<Subscription> history(Long userId) {
        return subscriptionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    private BigDecimal defaultAmount(PlanType planType) {
        return planType == PlanType.PREMIUM ? new BigDecimal("19.99") : BigDecimal.ZERO;
    }
}


