package com.loveconnect.app.repository;

import com.loveconnect.app.entity.CallSession;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CallSessionRepository extends JpaRepository<CallSession, Long> {
    List<CallSession> findByCallerIdOrReceiverIdOrderByCreatedAtDesc(Long callerId, Long receiverId);
}
