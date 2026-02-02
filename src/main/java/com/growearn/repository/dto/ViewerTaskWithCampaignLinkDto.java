package com.growearn.repository.dto;

public class ViewerTaskWithCampaignLinkDto {
    private Long id;
    private Long campaignId;
    private Long viewerId;
    private Long creatorId;
    private String taskType;
    private boolean completed;
    private String status;
    private String targetLink;

    public ViewerTaskWithCampaignLinkDto(Long id, Long campaignId, Long viewerId, Long creatorId, String taskType, boolean completed, String status, String targetLink) {
        this.id = id;
        this.campaignId = campaignId;
        this.viewerId = viewerId;
        this.creatorId = creatorId;
        this.taskType = taskType;
        this.completed = completed;
        this.status = status;
        this.targetLink = targetLink;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCampaignId() { return campaignId; }
    public void setCampaignId(Long campaignId) { this.campaignId = campaignId; }
    public Long getViewerId() { return viewerId; }
    public void setViewerId(Long viewerId) { this.viewerId = viewerId; }
    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTargetLink() { return targetLink; }
    public void setTargetLink(String targetLink) { this.targetLink = targetLink; }
}