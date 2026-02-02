package com.growearn.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "wallet")
public class WalletEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "viewer_id", unique = true)
    private Long viewerId;

    @Column(name = "balance")
    private Double balance = 0.0;

    public Long getId() { return id; }
    public Long getViewerId() { return viewerId; }
    public void setViewerId(Long viewerId) { this.viewerId = viewerId; }
    public Double getBalance() { return balance; }
    public void setBalance(Double balance) { this.balance = balance; }
}
