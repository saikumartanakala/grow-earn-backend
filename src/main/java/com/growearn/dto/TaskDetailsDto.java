package com.growearn.dto;

public class TaskDetailsDto {
    private Long campaignId;
    private String taskType;
    private String platform; // YOUTUBE, INSTAGRAM, FACEBOOK, TWITTER
    private String channelName;
    private String videoLink;
    private String targetUrl;
    private double earnAmount;
    private String instructions;
    private String taskDescription; // Platform-specific task description

    public TaskDetailsDto() {}

    public Long getCampaignId() { return campaignId; }
    public void setCampaignId(Long campaignId) { this.campaignId = campaignId; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public String getChannelName() { return channelName; }
    public void setChannelName(String channelName) { this.channelName = channelName; }
    public String getVideoLink() { return videoLink; }
    public void setVideoLink(String videoLink) { this.videoLink = videoLink; }
    public double getEarnAmount() { return earnAmount; }
    public void setEarnAmount(double earnAmount) { this.earnAmount = earnAmount; }
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getTargetUrl() { return targetUrl; }
    public void setTargetUrl(String targetUrl) { this.targetUrl = targetUrl; }
    public String getTaskDescription() { return taskDescription; }
    public void setTaskDescription(String taskDescription) { this.taskDescription = taskDescription; }
}
