package com.growearn.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_violations")
public class UserViolation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "violation_type", length = 50, nullable = false)
    private String violationType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "strike_count", nullable = false)
    private Integer strikeCount = 0;

    @Column(name = "action_taken", length = 50)
    private String actionTaken;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private Long createdBy;

    public UserViolation() {
        this.createdAt = LocalDateTime.now();
    }

    public UserViolation(Long userId, String violationType, String description, Integer strikeCount, String actionTaken, Long createdBy) {
        this.userId = userId;
        this.violationType = violationType;
        this.description = description;
        this.strikeCount = strikeCount;
        this.actionTaken = actionTaken;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
    }

    // Getters
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getViolationType() { return violationType; }
    public String getDescription() { return description; }
    public Integer getStrikeCount() { return strikeCount; }
    public String getActionTaken() { return actionTaken; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getCreatedBy() { return createdBy; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setViolationType(String violationType) { this.violationType = violationType; }
    public void setDescription(String description) { this.description = description; }
    public void setStrikeCount(Integer strikeCount) { this.strikeCount = strikeCount; }
    public void setActionTaken(String actionTaken) { this.actionTaken = actionTaken; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
}
