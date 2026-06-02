package com.loveconnect.app.repository;

import com.loveconnect.app.entity.UserBlock;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {
    boolean existsByBlockerIdAndBlockedUserId(Long blockerId, Long blockedUserId);
    Optional<UserBlock> findByBlockerIdAndBlockedUserId(Long blockerId, Long blockedUserId);
}
