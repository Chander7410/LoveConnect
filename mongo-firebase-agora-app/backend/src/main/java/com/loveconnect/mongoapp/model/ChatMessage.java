package com.loveconnect.mongoapp.model;

import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("chat_messages")
public class ChatMessage {
    @Id
    private String id;
    @Indexed
    private String conversationId;
    @Indexed
    private String senderUid;
    @Indexed
    private String receiverUid;
    private String text;
    private Instant readAt;
    @CreatedDate
    private Instant createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public String getSenderUid() { return senderUid; }
    public void setSenderUid(String senderUid) { this.senderUid = senderUid; }
    public String getReceiverUid() { return receiverUid; }
    public void setReceiverUid(String receiverUid) { this.receiverUid = receiverUid; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public Instant getReadAt() { return readAt; }
    public void setReadAt(Instant readAt) { this.readAt = readAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
