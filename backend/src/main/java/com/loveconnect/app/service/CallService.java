package com.loveconnect.app.service;

import com.loveconnect.app.dto.CallRequest;
import com.loveconnect.app.dto.CallResponse;
import com.loveconnect.app.entity.CallSession;
import com.loveconnect.app.entity.CallStatus;
import com.loveconnect.app.entity.NotificationType;
import com.loveconnect.app.entity.User;
import com.loveconnect.app.exception.BadRequestException;
import com.loveconnect.app.exception.ResourceNotFoundException;
import com.loveconnect.app.repository.CallSessionRepository;
import com.loveconnect.app.repository.UserRepository;
import com.loveconnect.app.util.Mapper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CallService {
    private final CallSessionRepository callRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final SafetyService safetyService;

    public CallService(CallSessionRepository callRepository, UserRepository userRepository,
                       NotificationService notificationService, SafetyService safetyService) {
        this.callRepository = callRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.safetyService = safetyService;
    }

    @Transactional
    public CallResponse start(User caller, CallRequest request) {
        if (caller.getId().equals(request.getReceiverId())) {
            throw new BadRequestException("You cannot call yourself");
        }
        User managedCaller = userRepository.findById(caller.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found"));
        if (receiver.isBlocked() || safetyService.isBlockedBetween(managedCaller.getId(), receiver.getId())) {
            throw new BadRequestException("This contact is unavailable for calls");
        }
        CallSession call = new CallSession();
        call.setCaller(managedCaller);
        call.setReceiver(receiver);
        call.setType(request.getType());
        call.setStartedAt(Instant.now());
        call.setStatus(CallStatus.RINGING);
        CallSession saved = callRepository.save(call);
        notificationService.create(receiver, NotificationType.CALL,
                managedCaller.getName() + " started a " + request.getType().name().toLowerCase() + " call");
        return Mapper.call(saved);
    }

    @Transactional
    public CallResponse end(User current, Long callId) {
        CallSession call = callRepository.findById(callId)
                .orElseThrow(() -> new ResourceNotFoundException("Call not found"));
        if (!call.getCaller().getId().equals(current.getId()) && !call.getReceiver().getId().equals(current.getId())) {
            throw new BadRequestException("You are not part of this call");
        }
        Instant endedAt = Instant.now();
        call.setEndedAt(endedAt);
        call.setStatus(CallStatus.COMPLETED);
        if (call.getStartedAt() != null) {
            call.setDurationSeconds(Math.max(1, Duration.between(call.getStartedAt(), endedAt).getSeconds()));
        }
        return Mapper.call(call);
    }

    @Transactional
    public CallResponse markMissed(Long callId) {
        CallSession call = callRepository.findById(callId)
                .orElseThrow(() -> new ResourceNotFoundException("Call not found"));
        call.setEndedAt(Instant.now());
        call.setStatus(CallStatus.MISSED);
        call.setDurationSeconds(0L);
        return Mapper.call(call);
    }

    @Transactional(readOnly = true)
    public List<CallResponse> history(User current) {
        return callRepository.findByCallerIdOrReceiverIdOrderByCreatedAtDesc(current.getId(), current.getId())
                .stream().map(Mapper::call).collect(Collectors.toList());
    }
}
