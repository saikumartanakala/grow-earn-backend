package com.growearn.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "viewer_tasks",
       indexes = {
           @Index(name = "idx_assigned_status", columnList = "assigned_to,status"),
           @Index(name = "idx_campaign_status", columnList = "campaign_id,status")
       }
)
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

    // New field to track which viewer has taken the task
    @Column(name = "assigned_to")
    private Long assignedTo;
    public Long getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(Long assignedTo) {
        this.assignedTo = assignedTo;
    }

    @Column(name = "task_type")
    private String taskType;

    @Column(name = "completed")
    private boolean completed;

    @Column(name = "status")
    private String status; // PENDING, COMPLETED

    @Column(name = "target_link")
    private String targetLink;

    @Column(name = "proof_url")
    private String proofUrl;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

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

    public String getProofUrl() {
        return proofUrl;
    }

    public void setProofUrl(String proofUrl) {
        this.proofUrl = proofUrl;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
}
