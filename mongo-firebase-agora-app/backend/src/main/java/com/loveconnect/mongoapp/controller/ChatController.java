package com.loveconnect.mongoapp.controller;

import com.loveconnect.mongoapp.dto.MessageRequest;
import com.loveconnect.mongoapp.model.ChatMessage;
import com.loveconnect.mongoapp.service.ChatService;
import com.loveconnect.mongoapp.service.SecurityContextService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chats")
public class ChatController {
    private final SecurityContextService security;
    private final ChatService chats;

    public ChatController(SecurityContextService security, ChatService chats) {
        this.security = security;
        this.chats = chats;
    }

    @GetMapping("/{otherUid}")
    public List<ChatMessage> history(@PathVariable String otherUid) {
        return chats.history(security.currentUser(), otherUid);
    }

    @PostMapping("/messages")
    public ChatMessage send(@Valid @RequestBody MessageRequest request) {
        return chats.send(security.currentUser(), request);
    }
}
