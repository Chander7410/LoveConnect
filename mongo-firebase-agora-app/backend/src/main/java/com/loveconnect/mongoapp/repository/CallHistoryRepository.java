package com.loveconnect.mongoapp.repository;

import com.loveconnect.mongoapp.model.CallHistory;
import com.loveconnect.mongoapp.model.CallHistoryStatus;
import java.util.List;
import java.util.Collection;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CallHistoryRepository extends MongoRepository<CallHistory, String> {
    List<CallHistory> findByCallerIdOrReceiverIdOrderByStartTimeDesc(String callerId, String receiverId);
    List<CallHistory> findByReceiverIdAndStatusOrderByStartTimeDesc(String receiverId, CallHistoryStatus status);
    List<CallHistory> findByCallerIdInOrReceiverIdInOrderByStartTimeDesc(Collection<String> callerIds, Collection<String> receiverIds);
    List<CallHistory> findTop20ByCallerIdInOrReceiverIdInOrderByStartTimeDesc(Collection<String> callerIds, Collection<String> receiverIds);
    List<CallHistory> findByReceiverIdInAndStatusOrderByStartTimeDesc(Collection<String> receiverIds, CallHistoryStatus status);
}
