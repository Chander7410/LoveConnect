package com.loveconnect.app.repository;

import com.loveconnect.app.entity.EmailOtp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailOtpRepository extends JpaRepository<EmailOtp, Long> {
    Optional<EmailOtp> findTopByEmailOrderByCreatedAtDesc(String email);
    long countByEmailAndCreatedAtAfter(String email, Instant createdAt);
}
