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


    // Dashboard stat fields
    private int subscribers;
    private int videoViews;
    private int shortViews;
    private int videoLikes;
    private int shortLikes;
    private int videoComments;
    private int shortComments;

    public CreatorStats() {}

    private int totalEarnings;

    public CreatorStats(Long creatorId) {
        this.creatorId = creatorId;
        this.subscribers = 0;
        this.videoViews = 0;
        this.shortViews = 0;
        this.videoLikes = 0;
        this.shortLikes = 0;
        this.videoComments = 0;
        this.shortComments = 0;
        this.totalEarnings = 0;
    }
    // Dashboard stat getters/setters
    public Integer getSubscribers() { return subscribers; }
    public void setSubscribers(int subscribers) { this.subscribers = subscribers; }

    public Integer getVideoViews() { return videoViews; }
    public void setVideoViews(int videoViews) { this.videoViews = videoViews; }

    public Integer getShortViews() { return shortViews; }
    public void setShortViews(int shortViews) { this.shortViews = shortViews; }

    public Integer getVideoLikes() { return videoLikes; }
    public void setVideoLikes(int videoLikes) { this.videoLikes = videoLikes; }

    public Integer getShortLikes() { return shortLikes; }
    public void setShortLikes(int shortLikes) { this.shortLikes = shortLikes; }

    public Integer getVideoComments() { return videoComments; }
    public void setVideoComments(int videoComments) { this.videoComments = videoComments; }

    public Integer getShortComments() { return shortComments; }
    public void setShortComments(int shortComments) { this.shortComments = shortComments; }

    // ===== GETTERS & SETTERS =====
    public int getTotalEarnings() {
        return totalEarnings;
    }
    public void setTotalEarnings(int totalEarnings) {
        this.totalEarnings = totalEarnings;
    }

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
