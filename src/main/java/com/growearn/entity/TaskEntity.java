package com.growearn.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
public class TaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_id")
    private Long campaignId;

    @Column(name = "task_type")
    private String taskType;

    @Column(name = "target_link")
    private String targetLink;

    @Column(name = "earning")
    private Double earning;

    @Column(name = "status")
    private String status; // OPEN, ASSIGNED, UNDER_VERIFICATION, COMPLETED

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and setters
    public Long getId() { return id; }
    public Long getCampaignId() { return campaignId; }
    public void setCampaignId(Long campaignId) { this.campaignId = campaignId; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public String getTargetLink() { return targetLink; }
    public void setTargetLink(String targetLink) { this.targetLink = targetLink; }
    public Double getEarning() { return earning; }
    public void setEarning(Double earning) { this.earning = earning; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
