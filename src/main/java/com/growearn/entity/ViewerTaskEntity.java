package com.growearn.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "viewer_tasks")
public class ViewerTaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "viewer_id")
    private Long viewerId;

    @Column(name = "status")
    private String status; // ASSIGNED, UNDER_VERIFICATION, COMPLETED, REJECTED

    @Column(name = "proof", columnDefinition = "TEXT")
    private String proof;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // New fields for verification pipeline
    @Column(name = "proof_url", length = 500)
    private String proofUrl;

    @Column(name = "proof_public_id", length = 255)
    private String proofPublicId;

    @Column(name = "proof_text", columnDefinition = "TEXT")
    private String proofText;

    @Column(name = "proof_hash", length = 64)
    private String proofHash;

    @Column(name = "risk_score")
    private Double riskScore;

    @Column(name = "auto_flag")
    private Boolean autoFlag;

    @Column(name = "device_fingerprint", length = 255)
    private String deviceFingerprint;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "payment_txn_id", length = 100)
    private String paymentTxnId;

    @Column(name = "hold_expiry")
    private LocalDateTime holdExpiry;

    @Column(name = "hold_start_time")
    private LocalDateTime holdStartTime;

    @Column(name = "hold_end_time")
    private LocalDateTime holdEndTime;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    // Getters/setters
    public Long getId() { return id; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getViewerId() { return viewerId; }
    public void setViewerId(Long viewerId) { this.viewerId = viewerId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getProof() { return proof; }
    public void setProof(String proof) { this.proof = proof; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public Long getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Long approvedBy) { this.approvedBy = approvedBy; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getProofUrl() { return proofUrl; }
    public void setProofUrl(String proofUrl) { this.proofUrl = proofUrl; }
    public String getProofPublicId() { return proofPublicId; }
    public void setProofPublicId(String proofPublicId) { this.proofPublicId = proofPublicId; }
    public String getProofText() { return proofText; }
    public void setProofText(String proofText) { this.proofText = proofText; }
    public String getProofHash() { return proofHash; }
    public void setProofHash(String proofHash) { this.proofHash = proofHash; }
    public Double getRiskScore() { return riskScore; }
    public void setRiskScore(Double riskScore) { this.riskScore = riskScore; }
    public Boolean getAutoFlag() { return autoFlag; }
    public void setAutoFlag(Boolean autoFlag) { this.autoFlag = autoFlag; }
    public String getDeviceFingerprint() { return deviceFingerprint; }
    public void setDeviceFingerprint(String deviceFingerprint) { this.deviceFingerprint = deviceFingerprint; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public String getPaymentTxnId() { return paymentTxnId; }
    public void setPaymentTxnId(String paymentTxnId) { this.paymentTxnId = paymentTxnId; }
    public LocalDateTime getHoldExpiry() { return holdExpiry; }
    public void setHoldExpiry(LocalDateTime holdExpiry) { this.holdExpiry = holdExpiry; }
    public LocalDateTime getHoldStartTime() { return holdStartTime; }
    public void setHoldStartTime(LocalDateTime holdStartTime) { this.holdStartTime = holdStartTime; }
    public LocalDateTime getHoldEndTime() { return holdEndTime; }
    public void setHoldEndTime(LocalDateTime holdEndTime) { this.holdEndTime = holdEndTime; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
}
