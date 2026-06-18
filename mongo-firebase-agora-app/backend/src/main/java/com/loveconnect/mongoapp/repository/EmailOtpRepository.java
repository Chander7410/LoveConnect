package com.loveconnect.mongoapp.repository;

import com.loveconnect.mongoapp.model.EmailOtp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EmailOtpRepository extends MongoRepository<EmailOtp, String> {
    Optional<EmailOtp> findTopByEmailOrderByCreatedAtDesc(String email);
    long countByEmailAndCreatedAtAfter(String email, Instant createdAt);
}
