package com.growearn.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "creator_stats")
public class CreatorStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "creator_id", unique = true)
    private Long creatorId;

    private int totalFollowers;
    private int totalViews;
    private int totalLikes;
    private int totalComments;

    public CreatorStats() {}

    public CreatorStats(Long creatorId) {
        this.creatorId = creatorId;
    }

    // ===== GETTERS & SETTERS =====

    public Long getCreatorId() {
        return creatorId;
    }

    public int getTotalFollowers() {
        return totalFollowers;
    }

    public void setTotalFollowers(int totalFollowers) {
        this.totalFollowers = totalFollowers;
    }

    public int getTotalViews() {
        return totalViews;
    }

    public void setTotalViews(int totalViews) {
        this.totalViews = totalViews;
    }

    public int getTotalLikes() {
        return totalLikes;
    }

    public void setTotalLikes(int totalLikes) {
        this.totalLikes = totalLikes;
    }

    public int getTotalComments() {
        return totalComments;
    }

    public void setTotalComments(int totalComments) {
        this.totalComments = totalComments;
    }
}
