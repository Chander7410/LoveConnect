package com.loveconnect.mongoapp.repository;

import com.loveconnect.mongoapp.model.UserProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserProfileRepository extends MongoRepository<UserProfile, String> {
    Optional<UserProfile> findByFirebaseUid(String firebaseUid);
    List<UserProfile> findAllByFirebaseUid(String firebaseUid);
    Optional<UserProfile> findByPhoneNumber(String phoneNumber);
    Optional<UserProfile> findByEmail(String email);
    List<UserProfile> findAllByEmail(String email);
    List<UserProfile> findAllByPhoneNumber(String phoneNumber);
    Optional<UserProfile> findByPasswordResetToken(String passwordResetToken);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
    List<UserProfile> findByFirebaseUidNotOrderByDisplayNameAsc(String firebaseUid);
}
