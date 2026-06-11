package com.loveconnect.mongoapp.repository;

import com.loveconnect.mongoapp.model.CallHistory;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CallHistoryRepository extends MongoRepository<CallHistory, String> {
    List<CallHistory> findByCallerIdOrReceiverIdOrderByStartTimeDesc(String callerId, String receiverId);
}
