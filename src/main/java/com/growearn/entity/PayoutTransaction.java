package com.growearn.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payout_transactions")
public class PayoutTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "withdrawal_id", nullable = false)
    private Long withdrawalId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 10)
    private String currency = "INR";

    @Column(name = "mode", length = 20)
    private String mode = "UPI";

    @Column(name = "purpose", length = 50)
    private String purpose = "payout";

    @Column(name = "fund_account", length = 150)
    private String fundAccount;

    @Column(name = "razorpay_payout_id", length = 100, unique = true)
    private String razorpayPayoutId;

    @Column(name = "status", length = 30)
    private String status;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getWithdrawalId() { return withdrawalId; }
    public void setWithdrawalId(Long withdrawalId) { this.withdrawalId = withdrawalId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getFundAccount() { return fundAccount; }
    public void setFundAccount(String fundAccount) { this.fundAccount = fundAccount; }
    public String getRazorpayPayoutId() { return razorpayPayoutId; }
    public void setRazorpayPayoutId(String razorpayPayoutId) { this.razorpayPayoutId = razorpayPayoutId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
}
