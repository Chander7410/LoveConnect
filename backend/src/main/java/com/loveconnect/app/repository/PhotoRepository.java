package com.loveconnect.app.repository;

import com.loveconnect.app.entity.Photo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhotoRepository extends JpaRepository<Photo, Long> {
    List<Photo> findByProfileUserId(Long userId);
}


