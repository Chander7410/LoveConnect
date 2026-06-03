package com.loveconnect.mongoapp.service;

import com.loveconnect.mongoapp.dto.AgoraCallResponse;
import com.loveconnect.mongoapp.dto.CallStartRequest;
import com.loveconnect.mongoapp.model.CallSession;
import com.loveconnect.mongoapp.model.CallStatus;
import com.loveconnect.mongoapp.repository.CallSessionRepository;
import com.loveconnect.mongoapp.security.FirebasePrincipal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CallService {
    private final CallSessionRepository calls;
    private final SimpMessagingTemplate messagingTemplate;
    private final String agoraAppId;
    private final String agoraCertificate;

    public CallService(
        CallSessionRepository calls,
        SimpMessagingTemplate messagingTemplate,
        @Value("${app.agora.app-id:}") String agoraAppId,
        @Value("${app.agora.app-certificate:}") String agoraCertificate
    ) {
        this.calls = calls;
        this.messagingTemplate = messagingTemplate;
        this.agoraAppId = agoraAppId;
        this.agoraCertificate = agoraCertificate;
    }

    public AgoraCallResponse start(FirebasePrincipal principal, CallStartRequest request) {
        var call = new CallSession();
        call.setCallerUid(principal.uid());
        call.setReceiverUid(request.receiverUid());
        call.setType(request.type());
        call.setStatus(CallStatus.RINGING);
        call.setChannelName("lc_" + UUID.randomUUID().toString().replace("-", ""));
        var saved = calls.save(call);
        messagingTemplate.convertAndSend("/topic/calls/" + request.receiverUid(), saved);

        var note = StringUtils.hasText(agoraCertificate)
            ? "Add Agora token generation before production if token security is enabled."
            : "For development, use an Agora project with token authentication disabled. Enable token generation for production.";
        return new AgoraCallResponse(saved, agoraAppId, saved.getChannelName(), null, note);
    }

    public CallSession end(FirebasePrincipal principal, String callId, CallStatus status) {
        var call = calls.findById(callId).orElseThrow(() -> new IllegalArgumentException("Call not found"));
        if (!principal.uid().equals(call.getCallerUid()) && !principal.uid().equals(call.getReceiverUid())) {
            throw new SecurityException("You are not part of this call");
        }
        call.setStatus(status == null ? CallStatus.ENDED : status);
        call.setEndedAt(Instant.now());
        var saved = calls.save(call);
        messagingTemplate.convertAndSend("/topic/calls/" + call.getCallerUid(), saved);
        messagingTemplate.convertAndSend("/topic/calls/" + call.getReceiverUid(), saved);
        return saved;
    }

    public List<CallSession> history(FirebasePrincipal principal) {
        return calls.findByCallerUidOrReceiverUidOrderByCreatedAtDesc(principal.uid(), principal.uid());
    }
}
