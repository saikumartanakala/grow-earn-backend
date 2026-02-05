package com.growearn.service;

import com.growearn.entity.ViewerTaskEntity;
import com.growearn.entity.User;
import com.growearn.entity.Platform;
import com.growearn.entity.TaskType;
import com.growearn.repository.ViewerTaskEntityRepository;
import com.growearn.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
public class RiskAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(RiskAnalysisService.class);

    // Risk scoring thresholds
    private static final double HIGH_RISK_THRESHOLD = 70.0;
    private static final double MEDIUM_RISK_THRESHOLD = 40.0;
    
    // Risk weights
    private static final double WEIGHT_DUPLICATE_PROOF = 30.0;
    private static final double WEIGHT_REPEATED_TEXT = 25.0;
    private static final double WEIGHT_TASK_FREQUENCY = 20.0;
    private static final double WEIGHT_ACCOUNT_AGE = 15.0;
    private static final double WEIGHT_DEVICE_REUSE = 10.0;

    private final ViewerTaskEntityRepository viewerTaskRepository;
    private final UserRepository userRepository;
    private final PlatformTaskMapper platformTaskMapper;

    public RiskAnalysisService(ViewerTaskEntityRepository viewerTaskRepository,
                               UserRepository userRepository,
                               PlatformTaskMapper platformTaskMapper) {
        this.viewerTaskRepository = viewerTaskRepository;
        this.userRepository = userRepository;
        this.platformTaskMapper = platformTaskMapper;
    }

    /**
     * Analyze risk for a task submission with platform and task type awareness
     */
    public Map<String, Object> analyzeRisk(ViewerTaskEntity task, String proofUrl, String proofText, 
                                           String deviceFingerprint, String ipAddress) {
        double riskScore = 0.0;
        StringBuilder reasons = new StringBuilder();

        // Get platform and task type for risk adjustments
        Platform platform = task.getPlatform() != null ? task.getPlatform() : Platform.YOUTUBE;
        TaskType taskType = task.getTaskType() != null ? task.getTaskType() : TaskType.VIEW_LONG;

        // Apply platform-specific risk multiplier
        double taskTypeMultiplier = platformTaskMapper.getRiskMultiplier(taskType);

        // 1. Check duplicate proof hash
        String proofHash = generateHash(proofUrl);
        task.setProofHash(proofHash);
        double duplicateProofScore = checkDuplicateProof(proofHash, task.getViewerId(), platform);
        riskScore += duplicateProofScore * taskTypeMultiplier;
        if (duplicateProofScore > 0) {
            reasons.append("Duplicate proof detected. ");
        }

        // 2. Check repeated comment text (higher risk for COMMENT tasks)
        if (proofText != null && !proofText.trim().isEmpty()) {
            double repeatedTextScore = checkRepeatedText(proofText, task.getViewerId(), platform);
            // Extra penalty for repetitive comments
            double textMultiplier = taskType == TaskType.COMMENT ? 1.5 : 1.0;
            riskScore += repeatedTextScore * textMultiplier;
            if (repeatedTextScore > 0) {
                reasons.append("Repeated proof text detected. ");
            }
        }

        // 3. Check task frequency per device and platform
        double frequencyScore = checkTaskFrequency(task.getViewerId(), deviceFingerprint, platform, taskType);
        riskScore += frequencyScore;
        if (frequencyScore > 0) {
            reasons.append("High task submission frequency for " + platform.name() + ". ");
        }

        // 4. Check account age
        double accountAgeScore = checkAccountAge(task.getViewerId());
        riskScore += accountAgeScore;
        if (accountAgeScore > 0) {
            reasons.append("New account activity. ");
        }

        // 5. Check IP/device reuse
        double deviceReuseScore = checkDeviceReuse(deviceFingerprint, ipAddress, task.getViewerId());
        riskScore += deviceReuseScore;
        if (deviceReuseScore > 0) {
            reasons.append("Device/IP shared with other accounts. ");
        }

        // Additional check: FOLLOW tasks always flagged for manual review
        if (taskType == TaskType.FOLLOW) {
            reasons.append("Follow task requires hold period verification. ");
        }

        // Determine auto-flag
        boolean autoFlag = riskScore >= MEDIUM_RISK_THRESHOLD || taskType == TaskType.FOLLOW;
        boolean autoReject = riskScore >= HIGH_RISK_THRESHOLD;

        String riskLevel;
        if (riskScore >= HIGH_RISK_THRESHOLD) {
            riskLevel = "HIGH";
        } else if (riskScore >= MEDIUM_RISK_THRESHOLD) {
            riskLevel = "MEDIUM";
        } else {
            riskLevel = "LOW";
        }

        logger.info("Risk analysis for viewer {} on {} {}: score={}, level={}, autoReject={}", 
                    task.getViewerId(), platform.name(), taskType.name(), riskScore, riskLevel, autoReject);

        return Map.of(
            "riskScore", riskScore,
            "riskLevel", riskLevel,
            "autoFlag", autoFlag,
            "autoReject", autoReject,
            "reasons", reasons.toString().trim(),
            "proofHash", proofHash
        );
    }

    /**
     * Check for duplicate proof submissions (platform-aware)
     */
    private double checkDuplicateProof(String proofHash, Long viewerId, Platform platform) {
        List<ViewerTaskEntity> existingTasks = viewerTaskRepository.findAll().stream()
            .filter(t -> proofHash.equals(t.getProofHash()))
            .filter(t -> platform.equals(t.getPlatform())) // Same platform
            .filter(t -> !viewerId.equals(t.getViewerId())) // Different user with same proof
            .toList();

        if (!existingTasks.isEmpty()) {
            return WEIGHT_DUPLICATE_PROOF;
        }

        // Check if same user submitted same proof multiple times on same platform
        long sameUserCount = viewerTaskRepository.findAll().stream()
            .filter(t -> proofHash.equals(t.getProofHash()))
            .filter(t -> platform.equals(t.getPlatform()))
            .filter(t -> viewerId.equals(t.getViewerId()))
            .count();

        if (sameUserCount > 1) {
            return WEIGHT_DUPLICATE_PROOF * 0.5; // Lower penalty for same user
        }

        return 0.0;
    }

    /**
     * Check for repeated proof text (platform-aware)
     */
    private double checkRepeatedText(String proofText, Long viewerId, Platform platform) {
        String textHash = generateHash(proofText);
        
        long matchCount = viewerTaskRepository.findAll().stream()
            .filter(t -> viewerId.equals(t.getViewerId()))
            .filter(t -> platform.equals(t.getPlatform()))
            .filter(t -> t.getProofText() != null)
            .filter(t -> textHash.equals(generateHash(t.getProofText())))
            .count();

        if (matchCount >= 3) {
            return WEIGHT_REPEATED_TEXT;
        } else if (matchCount >= 2) {
            return WEIGHT_REPEATED_TEXT * 0.5;
        }

        return 0.0;
    }

    /**
     * Check task submission frequency (platform and task type aware)
     */
    private double checkTaskFrequency(Long viewerId, String deviceFingerprint, Platform platform, TaskType taskType) {
        LocalDateTime oneDayAgo = LocalDateTime.now().minus(1, ChronoUnit.DAYS);
        
        // Check platform-specific rate limits
        int rateLimit = platformTaskMapper.getPlatformRateLimit(platform, taskType);
        
        long tasksLast24Hours = viewerTaskRepository.findAll().stream()
            .filter(t -> viewerId.equals(t.getViewerId()))
            .filter(t -> platform.equals(t.getPlatform()))
            .filter(t -> taskType.equals(t.getTaskType()))
            .filter(t -> t.getSubmittedAt() != null && t.getSubmittedAt().isAfter(oneDayAgo))
            .count();

        // Check device-based frequency too
        if (deviceFingerprint != null) {
            long deviceTasksLast24Hours = viewerTaskRepository.findAll().stream()
                .filter(t -> deviceFingerprint.equals(t.getDeviceFingerprint()))
                .filter(t -> platform.equals(t.getPlatform()))
                .filter(t -> t.getSubmittedAt() != null && t.getSubmittedAt().isAfter(oneDayAgo))
                .count();

            tasksLast24Hours = Math.max(tasksLast24Hours, deviceTasksLast24Hours);
        }

        // Use platform-specific rate limits
        if (tasksLast24Hours > rateLimit * 2) {
            return WEIGHT_TASK_FREQUENCY; // Exceeded by 200%
        } else if (tasksLast24Hours > rateLimit * 1.5) {
            return WEIGHT_TASK_FREQUENCY * 0.7; // Exceeded by 150%
        } else if (tasksLast24Hours > rateLimit) {
            return WEIGHT_TASK_FREQUENCY * 0.4; // Exceeded limit
        }

        return 0.0;
    }

    /**
     * Check account age - newer accounts are riskier
     */
    private double checkAccountAge(Long viewerId) {
        return userRepository.findById(viewerId)
            .map(user -> {
                if (user.getCreatedAt() == null) {
                    return 0.0;
                }
                
                long daysSinceCreation = ChronoUnit.DAYS.between(user.getCreatedAt(), LocalDateTime.now());
                
                if (daysSinceCreation < 1) {
                    return WEIGHT_ACCOUNT_AGE; // Brand new account
                } else if (daysSinceCreation < 7) {
                    return WEIGHT_ACCOUNT_AGE * 0.6; // Less than a week old
                } else if (daysSinceCreation < 30) {
                    return WEIGHT_ACCOUNT_AGE * 0.3; // Less than a month old
                }
                
                return 0.0;
            })
            .orElse(0.0);
    }

    /**
     * Check device/IP reuse across multiple accounts
     */
    private double checkDeviceReuse(String deviceFingerprint, String ipAddress, Long viewerId) {
        if (deviceFingerprint == null && ipAddress == null) {
            return 0.0;
        }

        // Check how many different users used this device or IP
        long deviceUserCount = 0;
        long ipUserCount = 0;

        if (deviceFingerprint != null) {
            deviceUserCount = viewerTaskRepository.findAll().stream()
                .filter(t -> deviceFingerprint.equals(t.getDeviceFingerprint()))
                .map(ViewerTaskEntity::getViewerId)
                .distinct()
                .count();
        }

        if (ipAddress != null) {
            ipUserCount = viewerTaskRepository.findAll().stream()
                .filter(t -> ipAddress.equals(t.getIpAddress()))
                .map(ViewerTaskEntity::getViewerId)
                .distinct()
                .count();
        }

        long maxUserCount = Math.max(deviceUserCount, ipUserCount);

        if (maxUserCount > 5) {
            return WEIGHT_DEVICE_REUSE;
        } else if (maxUserCount > 3) {
            return WEIGHT_DEVICE_REUSE * 0.6;
        } else if (maxUserCount > 1) {
            return WEIGHT_DEVICE_REUSE * 0.3;
        }

        return 0.0;
    }

    /**
     * Generate SHA-256 hash
     */
    private String generateHash(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            logger.error("Error generating hash", e);
            return "";
        }
    }

    /**
     * Check if auto-rejection is needed
     */
    public boolean shouldAutoReject(double riskScore) {
        return riskScore >= HIGH_RISK_THRESHOLD;
    }
}
