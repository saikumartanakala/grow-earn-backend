package com.growearn.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "viewer_wallet")
public class ViewerWallet {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "balance", precision = 10, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "locked_balance", precision = 10, scale = 2)
    private BigDecimal lockedBalance = BigDecimal.ZERO;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToOne
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Constructors
    public ViewerWallet() {
    }

    public ViewerWallet(Long userId) {
        this.userId = userId;
        this.balance = BigDecimal.ZERO;
        this.lockedBalance = BigDecimal.ZERO;
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
    }

    public BigDecimal getLockedBalance() {
        return lockedBalance;
    }

    public void setLockedBalance(BigDecimal lockedBalance) {
        this.lockedBalance = lockedBalance;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    // Helper methods
    public void addToBalance(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    public void deductFromBalance(BigDecimal amount) {
        this.balance = this.balance.subtract(amount);
    }

    public void addToLockedBalance(BigDecimal amount) {
        this.lockedBalance = this.lockedBalance.add(amount);
    }

    public void deductFromLockedBalance(BigDecimal amount) {
        this.lockedBalance = this.lockedBalance.subtract(amount);
    }

    public BigDecimal getTotalBalance() {
        return this.balance.add(this.lockedBalance);
    }
}
