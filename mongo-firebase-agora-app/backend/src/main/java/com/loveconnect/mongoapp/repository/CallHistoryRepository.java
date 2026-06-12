package com.loveconnect.mongoapp.repository;

import com.loveconnect.mongoapp.model.CallHistory;
import com.loveconnect.mongoapp.model.CallHistoryStatus;
import java.util.List;
import java.util.Collection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CallHistoryRepository extends MongoRepository<CallHistory, String> {
    List<CallHistory> findByCallerIdOrReceiverIdOrderByStartTimeDesc(String callerId, String receiverId);
    List<CallHistory> findByReceiverIdAndStatusOrderByStartTimeDesc(String receiverId, CallHistoryStatus status);
    List<CallHistory> findByCallerIdInOrReceiverIdInOrderByStartTimeDesc(Collection<String> callerIds, Collection<String> receiverIds);
    List<CallHistory> findByCallerIdInOrReceiverIdIn(Collection<String> callerIds, Collection<String> receiverIds, Pageable pageable);
    List<CallHistory> findByCallerIdIn(Collection<String> callerIds, Pageable pageable);
    List<CallHistory> findByReceiverIdIn(Collection<String> receiverIds, Pageable pageable);
    List<CallHistory> findByReceiverIdInAndStatusOrderByStartTimeDesc(Collection<String> receiverIds, CallHistoryStatus status);
    List<CallHistory> findByReceiverIdInAndStatus(Collection<String> receiverIds, CallHistoryStatus status, Pageable pageable);
}
