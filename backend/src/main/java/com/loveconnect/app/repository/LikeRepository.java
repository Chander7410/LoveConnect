package com.loveconnect.app.repository;

import com.loveconnect.app.entity.LikeAction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LikeRepository extends JpaRepository<LikeAction, Long> {
    Optional<LikeAction> findByFromUserIdAndToUserId(Long fromUserId, Long toUserId);
    boolean existsByFromUserIdAndToUserIdAndLikedTrue(Long fromUserId, Long toUserId);
    List<LikeAction> findByToUserIdAndLikedTrue(Long toUserId);
}


