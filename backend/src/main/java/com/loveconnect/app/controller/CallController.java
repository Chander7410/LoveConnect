package com.loveconnect.app.controller;

import com.loveconnect.app.dto.CallRequest;
import com.loveconnect.app.dto.CallResponse;
import com.loveconnect.app.service.CallService;
import com.loveconnect.app.service.CurrentUserService;
import java.util.List;
import javax.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/calls")
public class CallController {
    private final CurrentUserService currentUserService;
    private final CallService callService;

    public CallController(CurrentUserService currentUserService, CallService callService) {
        this.currentUserService = currentUserService;
        this.callService = callService;
    }

    @PostMapping("/start")
    public CallResponse start(Authentication authentication, @Valid @RequestBody CallRequest request) {
        return callService.start(currentUserService.get(authentication), request);
    }

    @PostMapping("/{callId}/end")
    public CallResponse end(Authentication authentication, @PathVariable Long callId) {
        return callService.end(currentUserService.get(authentication), callId);
    }

    @PostMapping("/{callId}/missed")
    public CallResponse missed(@PathVariable Long callId) {
        return callService.markMissed(callId);
    }

    @GetMapping("/history")
    public List<CallResponse> history(Authentication authentication) {
        return callService.history(currentUserService.get(authentication));
    }
}
