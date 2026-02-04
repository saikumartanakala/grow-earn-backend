package com.growearn.dto;

public class TaskSubmissionRequest {
    private Long taskId;
    private String proofUrl;
    private String proofPublicId;
    private String proofText;
    private String deviceFingerprint;

    public TaskSubmissionRequest() {}

    public TaskSubmissionRequest(Long taskId, String proofUrl, String proofPublicId, String proofText, String deviceFingerprint) {
        this.taskId = taskId;
        this.proofUrl = proofUrl;
        this.proofPublicId = proofPublicId;
        this.proofText = proofText;
        this.deviceFingerprint = deviceFingerprint;
    }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    
    public String getProofUrl() { return proofUrl; }
    public void setProofUrl(String proofUrl) { this.proofUrl = proofUrl; }
    
    public String getProofPublicId() { return proofPublicId; }
    public void setProofPublicId(String proofPublicId) { this.proofPublicId = proofPublicId; }
    
    public String getProofText() { return proofText; }
    public void setProofText(String proofText) { this.proofText = proofText; }
    
    public String getDeviceFingerprint() { return deviceFingerprint; }
    public void setDeviceFingerprint(String deviceFingerprint) { this.deviceFingerprint = deviceFingerprint; }
}
