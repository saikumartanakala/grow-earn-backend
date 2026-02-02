package com.growearn.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "viewer_tasks")
public class ViewerTaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "viewer_id")
    private Long viewerId;

    @Column(name = "status")
    private String status; // ASSIGNED, UNDER_VERIFICATION, COMPLETED, REJECTED

    @Column(name = "proof", columnDefinition = "TEXT")
    private String proof;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // Getters/setters
    public Long getId() { return id; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getViewerId() { return viewerId; }
    public void setViewerId(Long viewerId) { this.viewerId = viewerId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getProof() { return proof; }
    public void setProof(String proof) { this.proof = proof; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public Long getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Long approvedBy) { this.approvedBy = approvedBy; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}
