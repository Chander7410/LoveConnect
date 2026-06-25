package com.loveconnect.mongoapp.repository;

import com.loveconnect.mongoapp.model.MobileOtp;
import com.loveconnect.mongoapp.model.OtpPurpose;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MobileOtpRepository extends MongoRepository<MobileOtp, String> {
    Optional<MobileOtp> findTopByMobileNumberAndPurposeOrderByCreatedAtDesc(String mobileNumber, OtpPurpose purpose);
    long countByMobileNumberAndPurposeAndCreatedAtAfter(String mobileNumber, OtpPurpose purpose, Instant createdAt);
}
