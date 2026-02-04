package com.growearn.dto;

public class TaskVerificationRequest {
    private Long viewerTaskId;
    private String action; // "APPROVE" or "REJECT"
    private String rejectionReason;

    public TaskVerificationRequest() {}

    public TaskVerificationRequest(Long viewerTaskId, String action, String rejectionReason) {
        this.viewerTaskId = viewerTaskId;
        this.action = action;
        this.rejectionReason = rejectionReason;
    }

    public Long getViewerTaskId() { return viewerTaskId; }
    public void setViewerTaskId(Long viewerTaskId) { this.viewerTaskId = viewerTaskId; }
    
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}
