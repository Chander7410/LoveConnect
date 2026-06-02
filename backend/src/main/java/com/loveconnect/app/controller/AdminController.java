package com.loveconnect.app.controller;

import com.loveconnect.app.dto.UserResponse;
import com.loveconnect.app.dto.ReportResponse;
import com.loveconnect.app.service.AdminService;
import com.loveconnect.app.service.SafetyService;
import com.loveconnect.app.util.Mapper;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;
    private final SafetyService safetyService;

    public AdminController(AdminService adminService, SafetyService safetyService) {
        this.adminService = adminService;
        this.safetyService = safetyService;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        return adminService.dashboard();
    }

    @GetMapping("/users")
    public Iterable<UserResponse> users() {
        return StreamSupport.stream(adminService.users().spliterator(), false).map(Mapper::user).collect(Collectors.toList());
    }

    @PatchMapping("/users/{userId}/block")
    public UserResponse block(@PathVariable Long userId, @RequestParam boolean blocked) {
        return Mapper.user(adminService.block(userId, blocked));
    }

    @GetMapping("/reports")
    public Iterable<ReportResponse> reports() {
        return safetyService.reports();
    }

    @GetMapping("/subscriptions")
    public Map<String, Object> subscriptions() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "Subscription management endpoints are available.");
        return response;
    }
}


