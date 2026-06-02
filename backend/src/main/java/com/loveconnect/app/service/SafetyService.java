package com.loveconnect.app.service;

import com.loveconnect.app.dto.ApiMessage;
import com.loveconnect.app.dto.ReportRequest;
import com.loveconnect.app.dto.ReportResponse;
import com.loveconnect.app.entity.NotificationType;
import com.loveconnect.app.entity.User;
import com.loveconnect.app.entity.UserBlock;
import com.loveconnect.app.entity.UserReport;
import com.loveconnect.app.exception.BadRequestException;
import com.loveconnect.app.exception.ResourceNotFoundException;
import com.loveconnect.app.repository.UserBlockRepository;
import com.loveconnect.app.repository.UserReportRepository;
import com.loveconnect.app.repository.UserRepository;
import com.loveconnect.app.util.Mapper;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SafetyService {
    private final UserRepository userRepository;
    private final UserReportRepository reportRepository;
    private final UserBlockRepository blockRepository;
    private final NotificationService notificationService;
    private final ModerationService moderationService;

    public SafetyService(UserRepository userRepository, UserReportRepository reportRepository,
                         UserBlockRepository blockRepository, NotificationService notificationService,
                         ModerationService moderationService) {
        this.userRepository = userRepository;
        this.reportRepository = reportRepository;
        this.blockRepository = blockRepository;
        this.notificationService = notificationService;
        this.moderationService = moderationService;
    }

    @Transactional
    public ReportResponse report(User reporter, ReportRequest request) {
        if (reporter.getId().equals(request.getReportedUserId())) {
            throw new BadRequestException("You cannot report your own profile");
        }
        User managedReporter = userRepository.findById(reporter.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
        User reported = userRepository.findById(request.getReportedUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Reported user not found"));
        moderationService.validateUserContent(request.getReason());
        moderationService.validateUserContent(request.getDetails());
        UserReport report = new UserReport();
        report.setReporter(managedReporter);
        report.setReportedUser(reported);
        report.setReason(request.getReason());
        report.setDetails(request.getDetails());
        report.setRiskScore(moderationService.reportRiskScore(request.getReason(), request.getDetails()));
        reported.setFakeProfileScore(Math.max(reported.getFakeProfileScore(), report.getRiskScore()));
        notificationService.create(managedReporter, NotificationType.REPORT, "Your report was submitted for review");
        return Mapper.report(reportRepository.save(report));
    }

    @Transactional
    public ApiMessage block(User blocker, Long blockedUserId) {
        if (blocker.getId().equals(blockedUserId)) {
            throw new BadRequestException("You cannot block yourself");
        }
        User managedBlocker = userRepository.findById(blocker.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
        User blocked = userRepository.findById(blockedUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Blocked user not found"));
        if (!blockRepository.existsByBlockerIdAndBlockedUserId(managedBlocker.getId(), blocked.getId())) {
            UserBlock block = new UserBlock();
            block.setBlocker(managedBlocker);
            block.setBlockedUser(blocked);
            blockRepository.save(block);
        }
        notificationService.create(managedBlocker, NotificationType.BLOCK, "User blocked");
        return new ApiMessage("User blocked.");
    }

    @Transactional
    public ApiMessage unblock(User blocker, Long blockedUserId) {
        blockRepository.findByBlockerIdAndBlockedUserId(blocker.getId(), blockedUserId).ifPresent(blockRepository::delete);
        return new ApiMessage("User unblocked.");
    }

    public boolean isBlockedBetween(Long firstUserId, Long secondUserId) {
        return blockRepository.existsByBlockerIdAndBlockedUserId(firstUserId, secondUserId)
                || blockRepository.existsByBlockerIdAndBlockedUserId(secondUserId, firstUserId);
    }

    public List<ReportResponse> reports() {
        return reportRepository.findAllByOrderByCreatedAtDesc().stream().map(Mapper::report).collect(Collectors.toList());
    }
}
