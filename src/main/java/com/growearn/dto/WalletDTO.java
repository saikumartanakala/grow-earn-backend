package com.growearn.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WalletDTO {
    
    private Long userId;
    private BigDecimal balance;
    private BigDecimal lockedBalance;
    private BigDecimal totalBalance;
    private LocalDateTime updatedAt;

    // Constructors
    public WalletDTO() {
    }

    public WalletDTO(Long userId, BigDecimal balance, BigDecimal lockedBalance, LocalDateTime updatedAt) {
        this.userId = userId;
        this.balance = balance;
        this.lockedBalance = lockedBalance;
        this.totalBalance = balance.add(lockedBalance);
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
        this.totalBalance = this.balance.add(this.lockedBalance != null ? this.lockedBalance : BigDecimal.ZERO);
    }

    public BigDecimal getLockedBalance() {
        return lockedBalance;
    }

    public void setLockedBalance(BigDecimal lockedBalance) {
        this.lockedBalance = lockedBalance;
        this.totalBalance = (this.balance != null ? this.balance : BigDecimal.ZERO).add(this.lockedBalance);
    }

    public BigDecimal getTotalBalance() {
        return totalBalance;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
