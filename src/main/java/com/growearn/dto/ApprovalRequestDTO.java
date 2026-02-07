package com.growearn.dto;

public class ApprovalRequestDTO {
    
    private String reason;

    // Constructors
    public ApprovalRequestDTO() {
    }

    public ApprovalRequestDTO(String reason) {
        this.reason = reason;
    }

    // Getters and Setters
    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
