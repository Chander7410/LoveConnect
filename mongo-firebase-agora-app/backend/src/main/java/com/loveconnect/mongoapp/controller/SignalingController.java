package com.loveconnect.mongoapp.controller;

import com.loveconnect.mongoapp.dto.CallSignalMessage;
import com.loveconnect.mongoapp.model.CallHistoryStatus;
import com.loveconnect.mongoapp.model.CallType;
import com.loveconnect.mongoapp.repository.UserProfileRepository;
import com.loveconnect.mongoapp.security.FirebasePrincipal;
import com.loveconnect.mongoapp.service.CallService;
import java.security.Principal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class SignalingController {
    private static final Logger log = LoggerFactory.getLogger(SignalingController.class);
    private final SimpMessagingTemplate messaging;
    private final UserProfileRepository users;
    private final CallService calls;

    public SignalingController(SimpMessagingTemplate messaging, UserProfileRepository users, CallService calls) {
        this.messaging = messaging;
        this.users = users;
        this.calls = calls;
    }

    @MessageMapping("/signal/{type}")
    public void signal(@DestinationVariable String type, CallSignalMessage message, Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Login is required before calling");
        }
        var sender = newestFirst(users.findAllByFirebaseUid(principal.getName())).stream().findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Caller profile not found"));
        var receiver = resolveReceiver(message);
        log.info("WebRTC {} received call={} fromUid={} fromId={} toUid={} toId={}",
            type, message.callId(), sender.getFirebaseUid(), sender.getId(), receiver.getFirebaseUid(), receiver.getId());
        if (receiver.getFirebaseUid() == null || receiver.getFirebaseUid().isBlank()) {
            throw new IllegalArgumentException("Receiver WebSocket uid not found");
        }

        var outbound = new LinkedHashMap<String, Object>();
        outbound.put("type", type);
        outbound.put("callId", message.callId());
        outbound.put("callType", message.callType());
        outbound.put("senderId", sender.getId());
        outbound.put("senderUid", sender.getFirebaseUid());
        outbound.put("senderName", sender.getDisplayName());
        outbound.put("receiverId", receiver.getId());
        outbound.put("payload", message.payload() == null ? Map.of() : message.payload());

        var callPrincipal = new FirebasePrincipal(principal.getName(), null, null);
        if ("call-request".equals(type) && (message.callId() == null || message.callId().isBlank())) {
            var call = calls.request(callPrincipal, receiver.getId(), message.callType() == null ? CallType.AUDIO : message.callType());
            outbound.put("callId", call.getId());
        } else if ("call-accept".equals(type)) {
            calls.markAccepted(callPrincipal, message.callId());
        } else if ("call-reject".equals(type)) {
            calls.complete(callPrincipal, message.callId(), CallHistoryStatus.REJECTED);
        } else if ("call-end".equals(type)) {
            calls.complete(callPrincipal, message.callId(), CallHistoryStatus.COMPLETED);
            log.info("WebRTC call ended call={} fromUid={} toUid={}", message.callId(), sender.getFirebaseUid(), receiver.getFirebaseUid());
        }

        messaging.convertAndSend("/topic/signaling/" + receiver.getFirebaseUid(), outbound);
        log.info("WebRTC {} sent call={} topic=/topic/signaling/{}", type, outbound.get("callId"), receiver.getFirebaseUid());
    }

    private com.loveconnect.mongoapp.model.UserProfile resolveReceiver(CallSignalMessage message) {
        if (message.receiverUid() != null && !message.receiverUid().isBlank()) {
            return newestFirst(users.findAllByFirebaseUid(message.receiverUid())).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Receiver profile not found"));
        }
        if (message.receiverId() != null && !message.receiverId().isBlank()) {
            return users.findById(message.receiverId())
                .orElseThrow(() -> new IllegalArgumentException("Receiver profile not found"));
        }
        throw new IllegalArgumentException("Receiver is required");
    }

    private List<com.loveconnect.mongoapp.model.UserProfile> newestFirst(List<com.loveconnect.mongoapp.model.UserProfile> profiles) {
        return profiles.stream()
            .sorted(Comparator
                .comparing(com.loveconnect.mongoapp.model.UserProfile::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(com.loveconnect.mongoapp.model.UserProfile::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();
    }
}
