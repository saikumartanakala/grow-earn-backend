package com.growearn.service;

import com.growearn.entity.Platform;
import com.growearn.entity.TaskType;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Platform-specific task mapping and validation helper
 * Maps generic TaskTypes to platform-specific meanings
 */
@Component
public class PlatformTaskMapper {

    /**
     * Get human-readable task description based on platform and task type
     */
    public String getTaskDescription(Platform platform, TaskType taskType) {
        return switch (platform) {
            case YOUTUBE -> switch (taskType) {
                case FOLLOW -> "Subscribe to Channel";
                case VIEW_LONG -> "Watch Video";
                case VIEW_SHORT -> "Watch YouTube Short";
                case LIKE -> "Like Video";
                case COMMENT -> "Comment on Video";
            };
            case INSTAGRAM -> switch (taskType) {
                case FOLLOW -> "Follow Account";
                case VIEW_LONG -> "View Post";
                case VIEW_SHORT -> "Watch Reel";
                case LIKE -> "Like Post/Reel";
                case COMMENT -> "Comment on Post/Reel";
            };
            case FACEBOOK -> switch (taskType) {
                case FOLLOW -> "Like Page";
                case VIEW_LONG -> "Watch Video";
                case VIEW_SHORT -> "Watch Reel";
                case LIKE -> "Like Post";
                case COMMENT -> "Comment on Post";
            };
            case TWITTER -> switch (taskType) {
                case FOLLOW -> "Follow Account";
                case VIEW_LONG -> "Watch Video";
                case VIEW_SHORT -> "Watch Short Clip";
                case LIKE -> "Like Tweet";
                case COMMENT -> "Reply to Tweet";
            };
        };
    }

    /**
     * Determine hold period in days based on platform and task type
     * FOLLOW tasks always require longer hold period
     */
    public int getHoldPeriodDays(Platform platform, TaskType taskType) {
        // FOLLOW tasks need longer hold to verify they don't unfollow
        if (taskType == TaskType.FOLLOW) {
            return 7; // 7 days for all FOLLOW tasks
        }
        
        // View tasks can have reduced hold
        if (taskType == TaskType.VIEW_LONG || taskType == TaskType.VIEW_SHORT) {
            return 3; // 3 days for view verification
        }
        
        // Default hold for LIKE and COMMENT
        return 5; // 5 days for interactions
    }

    /**
     * Get base risk score multiplier for task type
     * Higher risk for FOLLOW (easier to fake and reverse)
     */
    public double getRiskMultiplier(TaskType taskType) {
        return switch (taskType) {
            case FOLLOW -> 1.5;      // Highest risk - easy to follow then unfollow
            case COMMENT -> 1.3;     // High risk - spam comments
            case LIKE -> 1.0;        // Medium risk
            case VIEW_SHORT -> 0.8;  // Lower risk - harder to fake
            case VIEW_LONG -> 0.7;   // Lowest risk - requires time investment
        };
    }

    /**
     * Check if task type is valid for platform
     * All platforms support all task types currently
     */
    public boolean isValidTaskTypeForPlatform(Platform platform, TaskType taskType) {
        // Currently all platforms support all task types
        // Can add platform-specific restrictions here if needed
        return true;
    }

    /**
     * Get platform-specific rate limits (tasks per day)
     */
    public int getPlatformRateLimit(Platform platform, TaskType taskType) {
        // FOLLOW tasks have strict rate limits to prevent abuse
        if (taskType == TaskType.FOLLOW) {
            return switch (platform) {
                case YOUTUBE -> 10;     // Max 10 subscribes per day
                case INSTAGRAM -> 15;   // Instagram allows more follows
                case FACEBOOK -> 10;    // Page likes limited
                case TWITTER -> 20;     // Twitter follows more common
            };
        }
        
        // Other tasks have higher limits
        return switch (platform) {
            case YOUTUBE -> 50;
            case INSTAGRAM -> 40;
            case FACEBOOK -> 40;
            case TWITTER -> 60;
        };
    }

    /**
     * Map legacy string task type to TaskType enum
     * For backward compatibility with existing data
     */
    public TaskType mapLegacyTaskType(String legacyType, String contentType) {
        if (legacyType == null) return TaskType.VIEW_LONG;
        
        return switch (legacyType.toUpperCase()) {
            case "SUBSCRIBE" -> TaskType.FOLLOW;
            case "VIEW" -> {
                // Check content type to determine if it's long or short
                if ("SHORT".equalsIgnoreCase(contentType)) {
                    yield TaskType.VIEW_SHORT;
                }
                yield TaskType.VIEW_LONG;
            }
            case "LIKE" -> TaskType.LIKE;
            case "COMMENT" -> TaskType.COMMENT;
            default -> TaskType.VIEW_LONG;
        };
    }

    /**
     * Map TaskType to legacy string format for stats tracking compatibility
     */
    public String toLegacyTaskType(TaskType taskType) {
        return switch (taskType) {
            case FOLLOW -> "SUBSCRIBE";
            case VIEW_LONG, VIEW_SHORT -> "VIEW";
            case LIKE -> "LIKE";
            case COMMENT -> "COMMENT";
        };
    }
}
