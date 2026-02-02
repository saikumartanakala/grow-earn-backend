package com.growearn.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Entity
@Table(name = "revoked_tokens", indexes = {
    @Index(name = "idx_revoked_tokens_token_hash", columnList = "token_hash"),
    @Index(name = "idx_revoked_tokens_user_id", columnList = "user_id")
})
public class RevokedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Store hash of token for efficient indexing (SHA-256 hash = 64 chars hex)
    @Column(name = "token_hash", length = 64, nullable = false, unique = true)
    private String tokenHash;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String token;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoked_by")
    private Long revokedBy;

    @Column(length = 100)
    private String reason;

    public RevokedToken() {
        this.revokedAt = LocalDateTime.now();
    }

    public RevokedToken(Long userId, String token, Long revokedBy, String reason) {
        this.userId = userId;
        this.token = token;
        this.tokenHash = hashToken(token);
        this.revokedBy = revokedBy;
        this.reason = reason;
        this.revokedAt = LocalDateTime.now();
    }

    // Static method to hash tokens for lookup
    public static String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    // Getters
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getToken() { return token; }
    public String getTokenHash() { return tokenHash; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public Long getRevokedBy() { return revokedBy; }
    public String getReason() { return reason; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setToken(String token) { 
        this.token = token;
        this.tokenHash = hashToken(token);
    }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }
    public void setRevokedBy(Long revokedBy) { this.revokedBy = revokedBy; }
    public void setReason(String reason) { this.reason = reason; }
}
