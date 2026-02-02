package com.growearn.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String email;

    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "is_verified")
    private Boolean isVerified = false;

    @Column(name = "suspension_until")
    private LocalDateTime suspensionUntil;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Profile fields
    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "channel_name", length = 100)
    private String channelName;

    @Column(name = "profile_pic_url", length = 500)
    private String profilePicUrl;

    @Column(name = "upi_id", length = 100)
    private String upiId;

    public User() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public User(String email, String password, Role role) {
        this.email = email;
        this.password = password;
        this.role = role;
        this.status = AccountStatus.ACTIVE;
        this.isVerified = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public Role getRole() { return role; }
    public AccountStatus getStatus() { return status != null ? status : AccountStatus.ACTIVE; }
    public Boolean getIsVerified() { return isVerified != null ? isVerified : false; }
    public LocalDateTime getSuspensionUntil() { return suspensionUntil; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public String getFullName() { return fullName; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getChannelName() { return channelName; }
    public String getProfilePicUrl() { return profilePicUrl; }
    public String getUpiId() { return upiId; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setRole(Role role) { this.role = role; }
    public void setStatus(AccountStatus status) { 
        this.status = status; 
        this.updatedAt = LocalDateTime.now();
    }
    public void setIsVerified(Boolean isVerified) { 
        this.isVerified = isVerified; 
        this.updatedAt = LocalDateTime.now();
    }
    public void setSuspensionUntil(LocalDateTime suspensionUntil) { 
        this.suspensionUntil = suspensionUntil; 
        this.updatedAt = LocalDateTime.now();
    }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setFullName(String fullName) { 
        this.fullName = fullName; 
        this.updatedAt = LocalDateTime.now();
    }
    public void setPhoneNumber(String phoneNumber) { 
        this.phoneNumber = phoneNumber; 
        this.updatedAt = LocalDateTime.now();
    }
    public void setChannelName(String channelName) { 
        this.channelName = channelName; 
        this.updatedAt = LocalDateTime.now();
    }
    public void setProfilePicUrl(String profilePicUrl) { 
        this.profilePicUrl = profilePicUrl; 
        this.updatedAt = LocalDateTime.now();
    }
    public void setUpiId(String upiId) { 
        this.upiId = upiId; 
        this.updatedAt = LocalDateTime.now();
    }

    // Helper methods
    public boolean isActive() {
        if (status == AccountStatus.BANNED) return false;
        if (status == AccountStatus.SUSPENDED) {
            if (suspensionUntil == null) return false;
            return LocalDateTime.now().isAfter(suspensionUntil);
        }
        return status == AccountStatus.ACTIVE;
    }

    public boolean isSuspended() {
        if (status != AccountStatus.SUSPENDED) return false;
        if (suspensionUntil == null) return true;
        return LocalDateTime.now().isBefore(suspensionUntil);
    }

    public boolean isBanned() {
        return status == AccountStatus.BANNED;
    }
}
