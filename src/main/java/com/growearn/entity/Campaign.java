package com.growearn.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "campaigns")
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long creatorId;


    private String title;
    private String description;
    private double goalAmount;
    private double currentAmount;
    private LocalDateTime updatedAt;

    private String platform; // YOUTUBE
    private String goalType; // S | V | L | C | SVLC

    private String channelName;
    private String channelLink;

    private String contentType; // VIDEO | SHORT
    private String videoLink;
    private String videoDuration;


    private int subscriberGoal;
    private int viewsGoal;
    private int likesGoal;
    private int commentsGoal;

    // Current progress counters
    private int currentSubscribers;
    private int currentViews;
    private int currentLikes;
    private int currentComments;

    // New: allow creator to specify number of tasks for each type
    private int subscriberTaskCount;
    private int viewsTaskCount;
    private int likesTaskCount;
    private int commentsTaskCount;

    private double totalAmount;

    private String status; // IN_PROGRESS | COMPLETED


    public Campaign() {
        this.goalAmount = 0.0;
        this.currentAmount = 0.0;
        this.updatedAt = LocalDateTime.now();
        this.currentSubscribers = 0;
        this.currentViews = 0;
        this.currentLikes = 0;
        this.currentComments = 0;
        this.status = "ACTIVE";
    }

    // getters & setters (FULL)
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getGoalAmount() { return goalAmount; }
    public void setGoalAmount(double goalAmount) { this.goalAmount = goalAmount; }

    public double getCurrentAmount() { return currentAmount; }
    public void setCurrentAmount(double currentAmount) { this.currentAmount = currentAmount; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Long getId() { return id; }
    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getGoalType() { return goalType; }
    public void setGoalType(String goalType) { this.goalType = goalType; }

    public String getChannelName() { return channelName; }
    public void setChannelName(String channelName) { this.channelName = channelName; }

    public String getChannelLink() { return channelLink; }
    public void setChannelLink(String channelLink) { this.channelLink = channelLink; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getVideoLink() { return videoLink; }
    public void setVideoLink(String videoLink) { this.videoLink = videoLink; }

    public String getVideoDuration() { return videoDuration; }
    public void setVideoDuration(String videoDuration) { this.videoDuration = videoDuration; }


    public int getSubscriberGoal() { return subscriberGoal; }
    public void setSubscriberGoal(int subscriberGoal) { this.subscriberGoal = subscriberGoal; }

    public int getViewsGoal() { return viewsGoal; }
    public void setViewsGoal(int viewsGoal) { this.viewsGoal = viewsGoal; }

    public int getLikesGoal() { return likesGoal; }
    public void setLikesGoal(int likesGoal) { this.likesGoal = likesGoal; }

    public int getCommentsGoal() { return commentsGoal; }
    public void setCommentsGoal(int commentsGoal) { this.commentsGoal = commentsGoal; }

    public int getCurrentSubscribers() { return currentSubscribers; }
    public void setCurrentSubscribers(int currentSubscribers) { this.currentSubscribers = currentSubscribers; }

    public int getCurrentViews() { return currentViews; }
    public void setCurrentViews(int currentViews) { this.currentViews = currentViews; }

    public int getCurrentLikes() { return currentLikes; }
    public void setCurrentLikes(int currentLikes) { this.currentLikes = currentLikes; }

    public int getCurrentComments() { return currentComments; }
    public void setCurrentComments(int currentComments) { this.currentComments = currentComments; }

    public int getSubscriberTaskCount() { return subscriberTaskCount; }
    public void setSubscriberTaskCount(int subscriberTaskCount) { this.subscriberTaskCount = subscriberTaskCount; }

    public int getViewsTaskCount() { return viewsTaskCount; }
    public void setViewsTaskCount(int viewsTaskCount) { this.viewsTaskCount = viewsTaskCount; }

    public int getLikesTaskCount() { return likesTaskCount; }
    public void setLikesTaskCount(int likesTaskCount) { this.likesTaskCount = likesTaskCount; }

    public int getCommentsTaskCount() { return commentsTaskCount; }
    public void setCommentsTaskCount(int commentsTaskCount) { this.commentsTaskCount = commentsTaskCount; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
