package com.loveconnect.app.repository;

import com.loveconnect.app.entity.Match;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRepository extends JpaRepository<Match, Long> {
    boolean existsByUserOneIdAndUserTwoId(Long userOneId, Long userTwoId);
    List<Match> findByUserOneIdOrUserTwoId(Long userOneId, Long userTwoId);
}


