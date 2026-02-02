package com.growearn.service;

import com.growearn.entity.*;
import com.growearn.repository.UserRepository;
import com.growearn.repository.UserViolationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class UserViolationService {

    private static final Logger logger = LoggerFactory.getLogger(UserViolationService.class);

    // Strike thresholds
    private static final int WARNING_THRESHOLD = 1;
    private static final int SUSPENSION_THRESHOLD = 2;
    private static final int BAN_THRESHOLD = 3;
    private static final int SUSPENSION_DAYS = 7;

    private final UserViolationRepository violationRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public UserViolationService(UserViolationRepository violationRepository,
                                UserRepository userRepository,
                                UserService userService) {
        this.violationRepository = violationRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    /**
     * Issue a warning to a user
     */
    @Transactional
    public Map<String, Object> warnUser(Long userId, String violationType, String description, Long adminId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        int currentStrikes = violationRepository.getMaxStrikeCount(userId);

        UserViolation violation = new UserViolation(
            userId,
            violationType,
            description,
            currentStrikes, // Warning doesn't increase strike count
            "WARNING",
            adminId
        );
        violationRepository.save(violation);

        logger.info("Warning issued to user {} for {}. Current strikes: {}", userId, violationType, currentStrikes);

        return Map.of(
            "success", true,
            "action", "WARNING",
            "userId", userId,
            "violationType", violationType,
            "currentStrikes", currentStrikes,
            "message", "Warning issued successfully"
        );
    }

    /**
     * Issue a strike to a user with automatic enforcement
     * 1st strike = warning
     * 2nd strike = 7-day suspension
     * 3rd strike = permanent ban
     */
    @Transactional
    public Map<String, Object> strikeUser(Long userId, String violationType, String description, Long adminId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        int currentStrikes = violationRepository.getMaxStrikeCount(userId);
        int newStrikeCount = currentStrikes + 1;
        String actionTaken;
        String message;

        // Determine action based on strike count
        if (newStrikeCount >= BAN_THRESHOLD) {
            // 3rd strike = permanent ban
            actionTaken = "BANNED";
            userService.banUser(userId, "Strike " + newStrikeCount + ": " + violationType);
            userService.forceLogout(userId, adminId, "Banned due to policy violation");
            message = "User permanently banned after " + newStrikeCount + " strikes";
            
        } else if (newStrikeCount >= SUSPENSION_THRESHOLD) {
            // 2nd strike = 7-day suspension
            actionTaken = "SUSPENDED_7_DAYS";
            userService.suspendUser(userId, SUSPENSION_DAYS, "Strike " + newStrikeCount + ": " + violationType);
            userService.forceLogout(userId, adminId, "Suspended due to policy violation");
            message = "User suspended for " + SUSPENSION_DAYS + " days after " + newStrikeCount + " strikes";
            
        } else {
            // 1st strike = warning only
            actionTaken = "WARNING";
            message = "Warning issued. Strike " + newStrikeCount + " of " + BAN_THRESHOLD;
        }

        // Record the violation
        UserViolation violation = new UserViolation(
            userId,
            "STRIKE",
            violationType + ": " + description,
            newStrikeCount,
            actionTaken,
            adminId
        );
        violationRepository.save(violation);

        logger.info("Strike {} issued to user {} for {}. Action: {}", 
            newStrikeCount, userId, violationType, actionTaken);

        return Map.of(
            "success", true,
            "action", actionTaken,
            "userId", userId,
            "violationType", violationType,
            "strikeCount", newStrikeCount,
            "maxStrikes", BAN_THRESHOLD,
            "message", message
        );
    }

    /**
     * Get all violations for a user
     */
    public List<UserViolation> getUserViolations(Long userId) {
        return violationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Get strike count for a user
     */
    public int getStrikeCount(Long userId) {
        return violationRepository.getMaxStrikeCount(userId);
    }

    /**
     * Get violation summary for a user
     */
    public Map<String, Object> getViolationSummary(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        List<UserViolation> violations = violationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        int strikeCount = violationRepository.getMaxStrikeCount(userId);

        return Map.of(
            "userId", userId,
            "email", user.getEmail(),
            "role", user.getRole().name(),
            "status", user.getStatus().name(),
            "strikeCount", strikeCount,
            "maxStrikes", BAN_THRESHOLD,
            "strikesUntilBan", Math.max(0, BAN_THRESHOLD - strikeCount),
            "violationCount", violations.size(),
            "violations", violations
        );
    }

    /**
     * Delete a specific violation by ID
     */
    @Transactional
    public Map<String, Object> deleteViolation(Long violationId, Long adminId) {
        UserViolation violation = violationRepository.findById(violationId)
            .orElseThrow(() -> new RuntimeException("Violation not found: " + violationId));
        
        Long userId = violation.getUserId();
        String violationType = violation.getViolationType();
        
        violationRepository.delete(violation);
        
        // Recalculate strike count after deletion
        int newStrikeCount = violationRepository.getMaxStrikeCount(userId);
        
        logger.info("Admin {} deleted violation {} for user {}. New strike count: {}", 
            adminId, violationId, userId, newStrikeCount);
        
        return Map.of(
            "success", true,
            "message", "Violation removed successfully",
            "violationId", violationId,
            "userId", userId,
            "violationType", violationType,
            "newStrikeCount", newStrikeCount
        );
    }

    /**
     * Delete all violations for a user (reset their record)
     */
    @Transactional
    public Map<String, Object> clearViolations(Long userId, Long adminId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        List<UserViolation> violations = violationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        int count = violations.size();
        
        violationRepository.deleteAll(violations);
        
        logger.info("Admin {} cleared {} violations for user {}", adminId, count, userId);
        
        return Map.of(
            "success", true,
            "message", "All violations cleared for user",
            "userId", userId,
            "violationsRemoved", count,
            "newStrikeCount", 0
        );
    }
}
