package com.loveconnect.app.dto;

import com.loveconnect.app.entity.ReportStatus;
import java.time.Instant;

public class ReportResponse {
    private Long id;
    private UserResponse reporter;
    private UserResponse reportedUser;
    private String reason;
    private String details;
    private ReportStatus status;
    private int riskScore;
    private Instant createdAt;

    public ReportResponse() {}
    public ReportResponse(Long id, UserResponse reporter, UserResponse reportedUser, String reason,
                          String details, ReportStatus status, int riskScore, Instant createdAt) {
        this.id = id; this.reporter = reporter; this.reportedUser = reportedUser; this.reason = reason;
        this.details = details; this.status = status; this.riskScore = riskScore; this.createdAt = createdAt;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UserResponse getReporter() { return reporter; }
    public void setReporter(UserResponse reporter) { this.reporter = reporter; }
    public UserResponse getReportedUser() { return reportedUser; }
    public void setReportedUser(UserResponse reportedUser) { this.reportedUser = reportedUser; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public ReportStatus getStatus() { return status; }
    public void setStatus(ReportStatus status) { this.status = status; }
    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
