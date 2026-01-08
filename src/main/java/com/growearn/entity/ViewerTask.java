package com.growearn.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "viewer_tasks")
public class ViewerTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_id")
    private Long campaignId;

    @Column(name = "viewer_id")
    private Long viewerId;

    @Column(name = "creator_id")
    private Long creatorId;

    @Column(name = "task_type")
    private String taskType;

    @Column(name = "completed")
    private boolean completed;

    @Column(name = "status")
    private String status; // PENDING, COMPLETED

    @Column(name = "target_link")
    private String targetLink;

    // ---------- Getters & Setters ----------

    public Long getId() {
        return id;
    }

    public Long getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(Long campaignId) {
        this.campaignId = campaignId;
    }

    public Long getViewerId() {
        return viewerId;
    }

    public void setViewerId(Long viewerId) {
        this.viewerId = viewerId;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTargetLink() {
        return targetLink;
    }

    public void setTargetLink(String targetLink) {
        this.targetLink = targetLink;
    }
}
