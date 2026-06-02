package com.loveconnect.app.repository;

import com.loveconnect.app.entity.Gender;
import com.loveconnect.app.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPasswordResetToken(String passwordResetToken);
    boolean existsByEmail(String email);
    boolean existsByMobileNumber(String mobileNumber);

    @Query("select distinct u from User u " +
            "left join u.profile p " +
            "left join p.interests i " +
            "where u.blocked = false " +
            "and (:minAge is null or u.age >= :minAge) " +
            "and (:maxAge is null or u.age <= :maxAge) " +
            "and (:gender is null or u.gender = :gender) " +
            "and (:city is null or lower(coalesce(p.city, u.location)) like lower(concat('%', :city, '%'))) " +
            "and (:interest is null or lower(i) like lower(concat('%', :interest, '%')))")
    List<User> search(@Param("minAge") Integer minAge,
                      @Param("maxAge") Integer maxAge,
                      @Param("gender") Gender gender,
                      @Param("city") String city,
                      @Param("interest") String interest);
}


