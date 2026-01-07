package com.growearn.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "campaigns")
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long creatorId;

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

    private double totalAmount;

    private String status; // IN_PROGRESS | COMPLETED

    public Campaign() {}

    // getters & setters (FULL)
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

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
