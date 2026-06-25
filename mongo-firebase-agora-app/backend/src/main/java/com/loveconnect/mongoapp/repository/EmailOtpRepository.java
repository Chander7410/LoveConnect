package com.loveconnect.mongoapp.repository;

import com.loveconnect.mongoapp.model.EmailOtp;
import com.loveconnect.mongoapp.model.EmailOtpPurpose;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EmailOtpRepository extends MongoRepository<EmailOtp, String> {
    Optional<EmailOtp> findTopByEmailOrderByCreatedAtDesc(String email);
    Optional<EmailOtp> findTopByEmailAndPurposeOrderByCreatedAtDesc(String email, EmailOtpPurpose purpose);
    long countByEmailAndCreatedAtAfter(String email, Instant createdAt);
    long countByEmailAndPurposeAndCreatedAtAfter(String email, EmailOtpPurpose purpose, Instant createdAt);
    void deleteByEmailAndPurpose(String email, EmailOtpPurpose purpose);
}
