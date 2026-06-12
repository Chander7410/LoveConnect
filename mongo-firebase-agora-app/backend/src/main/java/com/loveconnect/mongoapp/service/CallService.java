package com.loveconnect.mongoapp.service;

import com.loveconnect.mongoapp.model.CallHistory;
import com.loveconnect.mongoapp.model.CallHistoryStatus;
import com.loveconnect.mongoapp.model.CallType;
import com.loveconnect.mongoapp.model.UserProfile;
import com.loveconnect.mongoapp.repository.CallHistoryRepository;
import com.loveconnect.mongoapp.repository.ChatMessageRepository;
import com.loveconnect.mongoapp.repository.UserProfileRepository;
import com.loveconnect.mongoapp.security.FirebasePrincipal;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class CallService {
    private final CallHistoryRepository calls;
    private final ChatMessageRepository messages;
    private final UserProfileRepository users;

    public CallService(CallHistoryRepository calls, ChatMessageRepository messages, UserProfileRepository users) {
        this.calls = calls;
        this.messages = messages;
        this.users = users;
    }

    public CallHistory request(FirebasePrincipal principal, String receiverId, CallType type) {
        var caller = currentProfile(principal);
        var receiver = users.findById(receiverId == null ? "" : receiverId)
            .orElseThrow(() -> new IllegalArgumentException("Receiver profile not found"));
        validateMatched(caller, receiver);

        var call = new CallHistory();
        call.setCallerId(caller.getId());
        call.setReceiverId(receiver.getId());
        call.setCallType(type == null ? CallType.AUDIO : type);
        call.setStatus(CallHistoryStatus.RINGING);
        call.setStartTime(Instant.now());
        return calls.save(call);
    }

    public CallHistory complete(FirebasePrincipal principal, String callId, CallHistoryStatus status) {
        var call = findParticipantCall(principal, callId);
        call.finish(status == null ? CallHistoryStatus.COMPLETED : status);
        return calls.save(call);
    }

    public CallHistory markAccepted(FirebasePrincipal principal, String callId) {
        return findParticipantCall(principal, callId);
    }

    public List<CallHistory> history(FirebasePrincipal principal) {
        var profileIds = currentProfileIds(principal);
        var page = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "startTime"));
        return java.util.stream.Stream
            .concat(calls.findByCallerIdIn(profileIds, page).stream(), calls.findByReceiverIdIn(profileIds, page).stream())
            .collect(Collectors.toMap(CallHistory::getId, call -> call, (first, second) -> first))
            .values()
            .stream()
            .sorted(Comparator.comparing(CallHistory::getStartTime, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(20)
            .toList();
    }

    public List<CallHistory> incoming(FirebasePrincipal principal) {
        var profileIds = currentProfileIds(principal);
        var recentCutoff = Instant.now().minusSeconds(90);
        return calls.findByReceiverIdInAndStatus(
            profileIds,
            CallHistoryStatus.RINGING,
            PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "startTime"))
        ).stream()
            .filter(call -> call.getStartTime() != null && call.getStartTime().isAfter(recentCutoff))
            .toList();
    }

    public Map<String, Object> toResponse(CallHistory call) {
        var item = new LinkedHashMap<String, Object>();
        item.put("id", call.getId());
        item.put("callerId", call.getCallerId());
        item.put("receiverId", call.getReceiverId());
        item.put("callType", call.getCallType());
        item.put("type", call.getCallType());
        item.put("status", call.getStatus());
        item.put("startTime", call.getStartTime());
        item.put("endTime", call.getEndTime());
        item.put("duration", call.getDuration());
        item.put("durationSeconds", call.getDuration());
        users.findById(call.getCallerId()).ifPresent(user -> item.put("caller", userSummary(user)));
        users.findById(call.getReceiverId()).ifPresent(user -> item.put("receiver", userSummary(user)));
        return item;
    }

    private CallHistory findParticipantCall(FirebasePrincipal principal, String callId) {
        var profileIds = currentProfileIds(principal);
        var call = calls.findById(callId == null ? "" : callId)
            .orElseThrow(() -> new IllegalArgumentException("Call not found"));
        if (!profileIds.contains(call.getCallerId()) && !profileIds.contains(call.getReceiverId())) {
            throw new SecurityException("You are not part of this call");
        }
        return call;
    }

    private void validateMatched(UserProfile caller, UserProfile receiver) {
        if (caller.getId().equals(receiver.getId())) {
            throw new IllegalArgumentException("You cannot call yourself");
        }
        if (caller.isBlocked() || receiver.isBlocked()) {
            throw new IllegalArgumentException("Calls are not available for blocked profiles");
        }
        if (!sameCity(caller, receiver) && sharedInterests(caller, receiver).isEmpty() && !hasConversation(caller, receiver)) {
            throw new IllegalArgumentException("Calls are only allowed with matched users.");
        }
    }

    private Map<String, Object> userSummary(UserProfile user) {
        var item = new LinkedHashMap<String, Object>();
        item.put("id", user.getId());
        item.put("name", user.getDisplayName());
        item.put("email", user.getEmail());
        item.put("gender", user.getGender());
        item.put("location", user.getLocation());
        item.put("profilePictureUrl", user.getPhotoUrl());
        return item;
    }

    private boolean sameCity(UserProfile caller, UserProfile receiver) {
        return caller.getLocation() != null
            && receiver.getLocation() != null
            && caller.getLocation().trim().equalsIgnoreCase(receiver.getLocation().trim());
    }

    private boolean hasConversation(UserProfile caller, UserProfile receiver) {
        if (caller.getFirebaseUid() == null || receiver.getFirebaseUid() == null) return false;
        return messages.existsByConversationId(conversationId(caller.getFirebaseUid(), receiver.getFirebaseUid()));
    }

    private List<String> sharedInterests(UserProfile caller, UserProfile receiver) {
        Set<String> callerInterests = (caller.getInterests() == null ? List.<String>of() : caller.getInterests()).stream()
            .map(value -> value.toLowerCase(Locale.ROOT))
            .collect(Collectors.toSet());
        return (receiver.getInterests() == null ? List.<String>of() : receiver.getInterests()).stream()
            .filter(value -> callerInterests.contains(value.toLowerCase(Locale.ROOT)))
            .toList();
    }

    private UserProfile currentProfile(FirebasePrincipal principal) {
        return currentProfiles(principal).stream()
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Current profile not found"));
    }

    private List<UserProfile> currentProfiles(FirebasePrincipal principal) {
        return users.findAllByFirebaseUid(principal.uid()).stream()
            .sorted(Comparator
                .comparing(UserProfile::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(UserProfile::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();
    }

    private List<String> currentProfileIds(FirebasePrincipal principal) {
        var profileIds = currentProfiles(principal).stream()
            .map(UserProfile::getId)
            .toList();
        if (profileIds.isEmpty()) {
            throw new IllegalArgumentException("Current profile not found");
        }
        return profileIds;
    }

    private String conversationId(String firstUid, String secondUid) {
        return List.of(firstUid, secondUid).stream()
            .sorted(Comparator.naturalOrder())
            .reduce((first, second) -> first + "_" + second)
            .orElseThrow();
    }
}
