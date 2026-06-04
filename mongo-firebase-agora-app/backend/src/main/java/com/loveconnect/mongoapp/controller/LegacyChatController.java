package com.loveconnect.mongoapp.controller;

import com.loveconnect.mongoapp.dto.MessageRequest;
import com.loveconnect.mongoapp.model.ChatMessage;
import com.loveconnect.mongoapp.model.UserProfile;
import com.loveconnect.mongoapp.repository.ChatMessageRepository;
import com.loveconnect.mongoapp.repository.UserProfileRepository;
import com.loveconnect.mongoapp.service.ChatService;
import com.loveconnect.mongoapp.service.ProfileService;
import com.loveconnect.mongoapp.service.SecurityContextService;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class LegacyChatController {
    private final SecurityContextService security;
    private final ProfileService profiles;
    private final UserProfileRepository users;
    private final ChatService chats;
    private final ChatMessageRepository messages;

    public LegacyChatController(SecurityContextService security, ProfileService profiles,
                                UserProfileRepository users, ChatService chats, ChatMessageRepository messages) {
        this.security = security;
        this.profiles = profiles;
        this.users = users;
        this.chats = chats;
        this.messages = messages;
    }

    @GetMapping("/conversation/{otherId}")
    public List<Map<String, Object>> conversation(@PathVariable String otherId) {
        var current = profiles.getOrCreate(security.currentUser());
        var other = users.findById(otherId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        return messages.findByConversationIdOrderByCreatedAtAsc(chats.conversationId(current.getFirebaseUid(), other.getFirebaseUid()))
            .stream()
            .map(message -> legacyMessage(message, current, other))
            .toList();
    }

    @PostMapping("/messages")
    public Map<String, Object> send(@Valid @RequestBody MessageRequest request) {
        var current = profiles.getOrCreate(security.currentUser());
        var receiver = users.findById(request.receiverUid()).orElseThrow(() -> new IllegalArgumentException("User not found"));
        var saved = chats.send(security.currentUser(), new MessageRequest(receiver.getFirebaseUid(), request.text()));
        return legacyMessage(saved, current, receiver);
    }

    private Map<String, Object> legacyMessage(ChatMessage message, UserProfile current, UserProfile other) {
        var item = new LinkedHashMap<String, Object>();
        item.put("id", message.getId());
        item.put("senderId", message.getSenderUid().equals(current.getFirebaseUid()) ? current.getId() : other.getId());
        item.put("receiverId", message.getReceiverUid().equals(current.getFirebaseUid()) ? current.getId() : other.getId());
        item.put("content", message.getText());
        item.put("readAt", message.getReadAt());
        item.put("createdAt", message.getCreatedAt());
        return item;
    }
}
