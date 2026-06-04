package com.loveconnect.mongoapp.controller;

import com.loveconnect.mongoapp.dto.AgoraCallResponse;
import com.loveconnect.mongoapp.dto.CallStartRequest;
import com.loveconnect.mongoapp.model.CallSession;
import com.loveconnect.mongoapp.model.CallStatus;
import com.loveconnect.mongoapp.model.CallType;
import com.loveconnect.mongoapp.repository.UserProfileRepository;
import com.loveconnect.mongoapp.service.CallService;
import com.loveconnect.mongoapp.service.SecurityContextService;
import java.util.LinkedHashMap;
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
    private final UserProfileRepository users;

    public CallController(SecurityContextService security, CallService calls, UserProfileRepository users) {
        this.security = security;
        this.calls = calls;
        this.users = users;
    }

    @PostMapping("/start")
    public Map<String, Object> start(@RequestBody Map<String, String> request) {
        var receiverUid = request.get("receiverUid");
        if (receiverUid == null || receiverUid.isBlank()) {
            var receiverId = request.get("receiverId");
            receiverUid = users.findById(receiverId == null ? "" : receiverId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"))
                .getFirebaseUid();
        }
        var type = CallType.valueOf(request.getOrDefault("type", "AUDIO"));
        return legacyCall(calls.start(security.currentUser(), new CallStartRequest(receiverUid, type)));
    }

    @PostMapping("/{callId}/end")
    public CallSession end(@PathVariable String callId, @RequestParam(defaultValue = "ENDED") CallStatus status) {
        return calls.end(security.currentUser(), callId, status);
    }

    @GetMapping("/history")
    public List<CallSession> history() {
        return calls.history(security.currentUser());
    }

    private Map<String, Object> legacyCall(AgoraCallResponse response) {
        var call = response.call();
        var item = new LinkedHashMap<String, Object>();
        item.put("id", call.getId());
        item.put("type", call.getType());
        item.put("status", call.getStatus());
        item.put("channelName", response.channelName());
        item.put("appId", response.appId());
        item.put("token", response.token());
        item.put("note", response.note());
        return item;
    }
}
