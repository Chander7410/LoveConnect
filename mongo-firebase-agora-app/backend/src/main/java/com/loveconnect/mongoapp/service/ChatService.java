package com.loveconnect.mongoapp.service;

import com.loveconnect.mongoapp.dto.MessageRequest;
import com.loveconnect.mongoapp.model.ChatMessage;
import com.loveconnect.mongoapp.repository.ChatMessageRepository;
import com.loveconnect.mongoapp.security.FirebasePrincipal;
import java.util.Comparator;
import java.util.List;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class ChatService {
    private final ChatMessageRepository messages;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatService(ChatMessageRepository messages, SimpMessagingTemplate messagingTemplate) {
        this.messages = messages;
        this.messagingTemplate = messagingTemplate;
    }

    public String conversationId(String firstUid, String secondUid) {
        return List.of(firstUid, secondUid).stream()
            .sorted(Comparator.naturalOrder())
            .reduce((first, second) -> first + "_" + second)
            .orElseThrow();
    }

    public List<ChatMessage> history(FirebasePrincipal principal, String otherUid) {
        return messages.findByConversationIdOrderByCreatedAtAsc(conversationId(principal.uid(), otherUid));
    }

    public ChatMessage send(FirebasePrincipal principal, MessageRequest request) {
        var message = new ChatMessage();
        message.setSenderUid(principal.uid());
        message.setReceiverUid(request.receiverUid());
        message.setConversationId(conversationId(principal.uid(), request.receiverUid()));
        message.setText(request.text().trim());
        var saved = messages.save(message);
        messagingTemplate.convertAndSend("/topic/conversations/" + saved.getConversationId(), saved);
        return saved;
    }
}
