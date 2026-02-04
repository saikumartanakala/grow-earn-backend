package com.growearn.dto;

public class TaskSubmitResponseDTO {
    private boolean success;
    private String message;
    private String taskId;

    public TaskSubmitResponseDTO() {
    }

    public TaskSubmitResponseDTO(boolean success, String message, String taskId) {
        this.success = success;
        this.message = message;
        this.taskId = taskId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
}
