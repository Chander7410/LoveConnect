package com.loveconnect.app.service;

import com.loveconnect.app.dto.MessageRequest;
import com.loveconnect.app.dto.MessageResponse;
import com.loveconnect.app.entity.Message;
import com.loveconnect.app.entity.NotificationType;
import com.loveconnect.app.entity.User;
import com.loveconnect.app.exception.ResourceNotFoundException;
import com.loveconnect.app.repository.MessageRepository;
import com.loveconnect.app.repository.UserRepository;
import com.loveconnect.app.util.Mapper;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ModerationService moderationService;
    private final SafetyService safetyService;

    public ChatService(MessageRepository messageRepository, UserRepository userRepository,
                       NotificationService notificationService, SimpMessagingTemplate messagingTemplate,
                       ModerationService moderationService, SafetyService safetyService) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.messagingTemplate = messagingTemplate;
        this.moderationService = moderationService;
        this.safetyService = safetyService;
    }

    @Transactional
    public MessageResponse send(User sender, MessageRequest request) {
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found"));
        if (safetyService.isBlockedBetween(sender.getId(), receiver.getId())) {
            throw new ResourceNotFoundException("Conversation is unavailable");
        }
        moderationService.validateMessage(request.getContent());
        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(request.getContent());
        MessageResponse response = Mapper.message(messageRepository.save(message));
        notificationService.create(receiver, NotificationType.MESSAGE, "New message from " + sender.getName());
        messagingTemplate.convertAndSendToUser(receiver.getEmail(), "/queue/messages", response);
        return response;
    }

    public List<MessageResponse> conversation(User current, Long otherUserId) {
        return messageRepository
                .findBySenderIdAndReceiverIdOrReceiverIdAndSenderIdOrderByCreatedAtAsc(
                        current.getId(), otherUserId, current.getId(), otherUserId)
                .stream().map(Mapper::message).collect(Collectors.toList());
    }

    @Transactional
    public void markRead(User current, Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
        if (message.getReceiver().getId().equals(current.getId())) {
            message.setReadAt(Instant.now());
        }
    }
}


