package com.growearn.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CreatorTopupResponseDTO {
    
    private Long id;
    private String topupId;
    private Long creatorId;
    private Long userId;
    private String creatorEmail;
    private String creatorName;
    private String userName;
    private BigDecimal amount;
    private String upiReference;
    private String upiReferenceId;
    private String proofUrl;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime processedAt;
    private Long approvedBy;
    private Long processedBy;
    private String approvedByEmail;
    private String rejectionReason;
    private String reason;
    private BigDecimal creatorNewBalance;

    // Constructors
    public CreatorTopupResponseDTO() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
        if (id != null) {
            this.topupId = "top_" + id;
        }
    }

    public String getTopupId() {
        return topupId;
    }

    public void setTopupId(String topupId) {
        this.topupId = topupId;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
        this.userId = creatorId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getCreatorEmail() {
        return creatorEmail;
    }

    public void setCreatorEmail(String creatorEmail) {
        this.creatorEmail = creatorEmail;
        this.userName = creatorEmail;
        this.creatorName = creatorEmail;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
        this.userName = creatorName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getUpiReference() {
        return upiReference;
    }

    public void setUpiReference(String upiReference) {
        this.upiReference = upiReference;
        this.upiReferenceId = upiReference;
    }

    public String getUpiReferenceId() {
        return upiReferenceId;
    }

    public void setUpiReferenceId(String upiReferenceId) {
        this.upiReferenceId = upiReferenceId;
        this.upiReference = upiReferenceId;
    }

    public String getProofUrl() {
        return proofUrl;
    }

    public void setProofUrl(String proofUrl) {
        this.proofUrl = proofUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        this.requestedAt = createdAt;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
        this.createdAt = requestedAt;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
        this.processedAt = approvedAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
        this.approvedAt = processedAt;
    }

    public Long getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(Long approvedBy) {
        this.approvedBy = approvedBy;
        this.processedBy = approvedBy;
    }

    public Long getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(Long processedBy) {
        this.processedBy = processedBy;
        this.approvedBy = processedBy;
    }

    public String getApprovedByEmail() {
        return approvedByEmail;
    }

    public void setApprovedByEmail(String approvedByEmail) {
        this.approvedByEmail = approvedByEmail;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
        this.reason = rejectionReason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
        this.rejectionReason = reason;
    }

    public BigDecimal getCreatorNewBalance() {
        return creatorNewBalance;
    }

    public void setCreatorNewBalance(BigDecimal creatorNewBalance) {
        this.creatorNewBalance = creatorNewBalance;
    }
}
