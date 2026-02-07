package com.growearn.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "creator_wallet")
public class CreatorWallet {

    @Id
    @Column(name = "creator_id")
    private Long creatorId;

    @Column(name = "balance", precision = 10, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "locked_balance", precision = 10, scale = 2)
    private BigDecimal lockedBalance = BigDecimal.ZERO;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToOne
    @JoinColumn(name = "creator_id", insertable = false, updatable = false)
    private User creator;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Constructors
    public CreatorWallet() {
    }

    public CreatorWallet(Long creatorId) {
        this.creatorId = creatorId;
        this.balance = BigDecimal.ZERO;
        this.lockedBalance = BigDecimal.ZERO;
    }

    // Getters and Setters
    public Long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
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

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
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

    public boolean hasAvailableBalance(BigDecimal amount) {
        return this.balance.compareTo(amount) >= 0;
    }
}
