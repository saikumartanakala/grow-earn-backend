package com.growearn.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "withdrawal_requests")
public class WithdrawalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "upi_id", nullable = false, length = 100)
    private String upiId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WithdrawalStatus status = WithdrawalStatus.PENDING;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "processed_by")
    private Long processedBy;

    @ManyToOne
    @JoinColumn(name = "processed_by", insertable = false, updatable = false)
    private User processor;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @PrePersist
    protected void onCreate() {
        this.requestedAt = LocalDateTime.now();
    }

    // Constructors
    public WithdrawalRequest() {
    }

    public WithdrawalRequest(Long userId, BigDecimal amount, String upiId) {
        this.userId = userId;
        this.amount = amount;
        this.upiId = upiId;
        this.status = WithdrawalStatus.PENDING;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getUpiId() {
        return upiId;
    }

    public void setUpiId(String upiId) {
        this.upiId = upiId;
    }

    public WithdrawalStatus getStatus() {
        return status;
    }

    public void setStatus(WithdrawalStatus status) {
        this.status = status;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public Long getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(Long processedBy) {
        this.processedBy = processedBy;
    }

    public User getProcessor() {
        return processor;
    }

    public void setProcessor(User processor) {
        this.processor = processor;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    // Helper methods
    public void approve(Long adminId) {
        this.status = WithdrawalStatus.PAID;
        this.processedBy = adminId;
        this.processedAt = LocalDateTime.now();
    }

    public void markProcessing(Long adminId) {
        this.status = WithdrawalStatus.PROCESSING;
        this.processedBy = adminId;
        this.processedAt = LocalDateTime.now();
    }

    public void markPaid() {
        this.status = WithdrawalStatus.PAID;
        this.processedAt = LocalDateTime.now();
    }

    public void markFailed(String reason) {
        this.status = WithdrawalStatus.FAILED;
        this.processedAt = LocalDateTime.now();
        this.rejectionReason = reason;
    }

    public void reject(Long adminId, String reason) {
        this.status = WithdrawalStatus.REJECTED;
        this.processedBy = adminId;
        this.processedAt = LocalDateTime.now();
        this.rejectionReason = reason;
    }

    public boolean isPending() {
        return this.status == WithdrawalStatus.PENDING;
    }
}
