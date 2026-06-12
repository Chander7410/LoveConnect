package com.loveconnect.mongoapp.repository;

import com.loveconnect.mongoapp.model.CallHistory;
import com.loveconnect.mongoapp.model.CallHistoryStatus;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CallHistoryRepository extends MongoRepository<CallHistory, String> {
    List<CallHistory> findByCallerIdOrReceiverIdOrderByStartTimeDesc(String callerId, String receiverId);
    List<CallHistory> findByReceiverIdAndStatusOrderByStartTimeDesc(String receiverId, CallHistoryStatus status);
}
