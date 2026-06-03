package com.loveconnect.mongoapp.repository;

import com.loveconnect.mongoapp.model.ChatMessage;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(String conversationId);
}
