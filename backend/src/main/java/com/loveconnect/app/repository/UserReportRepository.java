package com.loveconnect.app.repository;

import com.loveconnect.app.entity.ReportStatus;
import com.loveconnect.app.entity.UserReport;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserReportRepository extends JpaRepository<UserReport, Long> {
    long countByStatus(ReportStatus status);
    List<UserReport> findAllByOrderByCreatedAtDesc();
}
