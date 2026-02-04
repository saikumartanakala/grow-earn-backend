package com.growearn.dto;

public class TaskSubmitRequestDTO {
    private String taskId;
    private String proofUrl;
    private String publicId;
    private String proofText;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getProofUrl() {
        return proofUrl;
    }

    public void setProofUrl(String proofUrl) {
        this.proofUrl = proofUrl;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public String getProofText() {
        return proofText;
    }

    public void setProofText(String proofText) {
        this.proofText = proofText;
    }
}
