package com.growearn.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "viewer_tasks")
public class ViewerTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long campaignId;

    private String taskType; // SUBSCRIBE, VIEW, LIKE

    private String targetLink;

    private boolean completed = false;

    private Long viewerId;

    // 🔹 GETTERS
    public Long getId() {
        return id;
    }

    public Long getCampaignId() {
        return campaignId;
    }

    public String getTaskType() {
        return taskType;
    }

    public String getTargetLink() {
        return targetLink;
    }

    public boolean isCompleted() {
        return completed;
    }

    public Long getViewerId() {
        return viewerId;
    }

    // 🔹 SETTERS (🔥 MISSING EARLIER)
    public void setCampaignId(Long campaignId) {
        this.campaignId = campaignId;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public void setTargetLink(String targetLink) {
        this.targetLink = targetLink;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public void setViewerId(Long viewerId) {
        this.viewerId = viewerId;
    }
}
