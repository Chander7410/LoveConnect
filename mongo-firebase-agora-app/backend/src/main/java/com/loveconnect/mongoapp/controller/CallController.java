package com.loveconnect.mongoapp.controller;

import com.loveconnect.mongoapp.dto.AgoraCallResponse;
import com.loveconnect.mongoapp.dto.CallStartRequest;
import com.loveconnect.mongoapp.model.CallSession;
import com.loveconnect.mongoapp.model.CallStatus;
import com.loveconnect.mongoapp.service.CallService;
import com.loveconnect.mongoapp.service.SecurityContextService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/calls")
public class CallController {
    private final SecurityContextService security;
    private final CallService calls;

    public CallController(SecurityContextService security, CallService calls) {
        this.security = security;
        this.calls = calls;
    }

    @PostMapping("/start")
    public AgoraCallResponse start(@Valid @RequestBody CallStartRequest request) {
        return calls.start(security.currentUser(), request);
    }

    @PostMapping("/{callId}/end")
    public CallSession end(@PathVariable String callId, @RequestParam(defaultValue = "ENDED") CallStatus status) {
        return calls.end(security.currentUser(), callId, status);
    }

    @GetMapping("/history")
    public List<CallSession> history() {
        return calls.history(security.currentUser());
    }
}
