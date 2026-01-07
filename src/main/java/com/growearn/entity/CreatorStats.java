package com.growearn.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "creator_stats")
public class CreatorStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long creatorId; // references users.id

    @Column(nullable = false)
    private int totalViews;

    @Column(nullable = false)
    private int totalFollowers;

    @Column(nullable = false)
    private double totalEarnings;

    public CreatorStats() {}

    public CreatorStats(Long creatorId) {
        this.creatorId = creatorId;
        this.totalViews = 0;
        this.totalFollowers = 0;
        this.totalEarnings = 0.0;
    }

    public Long getId() {
        return id;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public int getTotalViews() {
        return totalViews;
    }

    public int getTotalFollowers() {
        return totalFollowers;
    }

    public double getTotalEarnings() {
        return totalEarnings;
    }

    public void setTotalViews(int totalViews) {
        this.totalViews = totalViews;
    }

    public void setTotalFollowers(int totalFollowers) {
        this.totalFollowers = totalFollowers;
    }

    public void setTotalEarnings(double totalEarnings) {
        this.totalEarnings = totalEarnings;
    }
}
