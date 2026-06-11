package com.loveconnect.mongoapp.controller;

import com.loveconnect.mongoapp.model.CallHistoryStatus;
import com.loveconnect.mongoapp.model.CallType;
import com.loveconnect.mongoapp.service.CallService;
import com.loveconnect.mongoapp.service.SecurityContextService;
import java.util.List;
import java.util.Map;
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
    public Map<String, Object> start(@RequestBody Map<String, String> request) {
        var type = CallType.valueOf(request.getOrDefault("type", "AUDIO"));
        return calls.toResponse(calls.request(security.currentUser(), request.get("receiverId"), type));
    }

    @PostMapping("/{callId}/accept")
    public Map<String, Object> accept(@PathVariable String callId) {
        return calls.toResponse(calls.markAccepted(security.currentUser(), callId));
    }

    @PostMapping("/{callId}/reject")
    public Map<String, Object> reject(@PathVariable String callId) {
        return calls.toResponse(calls.complete(security.currentUser(), callId, CallHistoryStatus.REJECTED));
    }

    @PostMapping("/{callId}/end")
    public Map<String, Object> end(@PathVariable String callId, @RequestParam(defaultValue = "COMPLETED") CallHistoryStatus status) {
        return calls.toResponse(calls.complete(security.currentUser(), callId, status));
    }

    @GetMapping("/history")
    public List<Map<String, Object>> history() {
        return calls.history(security.currentUser()).stream().map(calls::toResponse).toList();
    }
}
