package com.growearn.dto;

import java.time.LocalDateTime;

public class TaskDto {
    private Long taskId; // campaignId
    private Long campaignId;
    private String taskType;
    private String platform; // YOUTUBE, INSTAGRAM, FACEBOOK, TWITTER
    private String channelName;
    private String targetUrl;
    private double earnAmount;
    private LocalDateTime postedAt;

    public TaskDto() {}

    public TaskDto(Long campaignId, String taskType, String channelName, double earnAmount, LocalDateTime postedAt) {
        this.taskId = campaignId;
        this.campaignId = campaignId;
        this.taskType = taskType;
        this.channelName = channelName;
        this.earnAmount = earnAmount;
        this.postedAt = postedAt;
    }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getTargetUrl() { return targetUrl; }
    public void setTargetUrl(String targetUrl) { this.targetUrl = targetUrl; }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getCampaignId() { return campaignId; }
    public void setCampaignId(Long campaignId) { this.campaignId = campaignId; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public String getChannelName() { return channelName; }
    public void setChannelName(String channelName) { this.channelName = channelName; }
    public double getEarnAmount() { return earnAmount; }
    public void setEarnAmount(double earnAmount) { this.earnAmount = earnAmount; }
    public LocalDateTime getPostedAt() { return postedAt; }
    public void setPostedAt(LocalDateTime postedAt) { this.postedAt = postedAt; }
}
