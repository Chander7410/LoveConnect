package com.loveconnect.mongoapp.repository;

import com.loveconnect.mongoapp.model.CallSession;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CallSessionRepository extends MongoRepository<CallSession, String> {
    List<CallSession> findByCallerUidOrReceiverUidOrderByCreatedAtDesc(String callerUid, String receiverUid);
}
