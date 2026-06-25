package com.loveconnect.mongoapp.repository;

import com.loveconnect.mongoapp.model.PendingSignup;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PendingSignupRepository extends MongoRepository<PendingSignup, String> {
    Optional<PendingSignup> findByEmail(String email);
    void deleteByEmail(String email);
}
