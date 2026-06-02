package com.loveconnect.app.service;

import com.loveconnect.app.entity.User;
import com.loveconnect.app.entity.ReportStatus;
import com.loveconnect.app.exception.ResourceNotFoundException;
import com.loveconnect.app.repository.MatchRepository;
import com.loveconnect.app.repository.SubscriptionRepository;
import com.loveconnect.app.repository.UserReportRepository;
import com.loveconnect.app.repository.UserRepository;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private final UserRepository userRepository;
    private final MatchRepository matchRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserReportRepository reportRepository;

    public AdminService(UserRepository userRepository, MatchRepository matchRepository,
                        SubscriptionRepository subscriptionRepository, UserReportRepository reportRepository) {
        this.userRepository = userRepository;
        this.matchRepository = matchRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.reportRepository = reportRepository;
    }

    public Map<String, Object> dashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("users", userRepository.count());
        dashboard.put("matches", matchRepository.count());
        dashboard.put("subscriptions", subscriptionRepository.count());
        dashboard.put("openReports", reportRepository.countByStatus(ReportStatus.OPEN));
        return dashboard;
    }

    public Iterable<User> users() {
        return userRepository.findAll();
    }

    @Transactional
    public User block(Long userId, boolean blocked) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setBlocked(blocked);
        return user;
    }
}


