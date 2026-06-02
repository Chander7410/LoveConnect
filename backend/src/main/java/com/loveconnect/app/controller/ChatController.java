package com.loveconnect.app.controller;

import com.loveconnect.app.dto.MessageRequest;
import com.loveconnect.app.dto.MessageResponse;
import com.loveconnect.app.service.ChatService;
import com.loveconnect.app.service.CurrentUserService;
import javax.validation.Valid;
import java.util.List;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final CurrentUserService currentUserService;
    private final ChatService chatService;

    public ChatController(CurrentUserService currentUserService, ChatService chatService) {
        this.currentUserService = currentUserService;
        this.chatService = chatService;
    }

    @PostMapping("/messages")
    public MessageResponse send(Authentication authentication, @Valid @RequestBody MessageRequest request) {
        return chatService.send(currentUserService.get(authentication), request);
    }

    @GetMapping("/conversation/{userId}")
    public List<MessageResponse> conversation(Authentication authentication, @PathVariable Long userId) {
        return chatService.conversation(currentUserService.get(authentication), userId);
    }

    @PostMapping("/messages/{messageId}/read")
    public void read(Authentication authentication, @PathVariable Long messageId) {
        chatService.markRead(currentUserService.get(authentication), messageId);
    }

    @MessageMapping("/chat.send")
    public void websocketSend(MessageRequest request) {
        // REST endpoint persists messages; this mapping is available for STOMP clients to extend.
    }
}


