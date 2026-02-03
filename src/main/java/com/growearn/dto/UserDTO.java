package com.growearn.dto;

public class UserDTO {
    private Long id;
    private String email;
    private String role;
    private String status;
    private Boolean isVerified;
    private String suspensionUntil;
    private String createdAt;
    private Boolean canLogin;

    public UserDTO(Long id, String email, String role, String status, Boolean isVerified, String suspensionUntil, String createdAt, Boolean canLogin) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.status = status;
        this.isVerified = isVerified;
        this.suspensionUntil = suspensionUntil;
        this.createdAt = createdAt;
        this.canLogin = canLogin;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Boolean getIsVerified() { return isVerified; }
    public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; }
    public String getSuspensionUntil() { return suspensionUntil; }
    public void setSuspensionUntil(String suspensionUntil) { this.suspensionUntil = suspensionUntil; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public Boolean getCanLogin() { return canLogin; }
    public void setCanLogin(Boolean canLogin) { this.canLogin = canLogin; }
}
