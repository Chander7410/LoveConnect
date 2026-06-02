package com.loveconnect.app.controller;

import com.loveconnect.app.dto.ApiMessage;
import com.loveconnect.app.dto.ReportRequest;
import com.loveconnect.app.dto.ReportResponse;
import com.loveconnect.app.service.CurrentUserService;
import com.loveconnect.app.service.SafetyService;
import javax.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/safety")
public class SafetyController {
    private final CurrentUserService currentUserService;
    private final SafetyService safetyService;

    public SafetyController(CurrentUserService currentUserService, SafetyService safetyService) {
        this.currentUserService = currentUserService;
        this.safetyService = safetyService;
    }

    @PostMapping("/reports")
    public ReportResponse report(Authentication authentication, @Valid @RequestBody ReportRequest request) {
        return safetyService.report(currentUserService.get(authentication), request);
    }

    @PostMapping("/blocks/{userId}")
    public ApiMessage block(Authentication authentication, @PathVariable Long userId) {
        return safetyService.block(currentUserService.get(authentication), userId);
    }

    @DeleteMapping("/blocks/{userId}")
    public ApiMessage unblock(Authentication authentication, @PathVariable Long userId) {
        return safetyService.unblock(currentUserService.get(authentication), userId);
    }
}
