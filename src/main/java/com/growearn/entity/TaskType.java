package com.growearn.entity;

public enum TaskType {
    FOLLOW,       // Subscribe (YouTube), Follow (Instagram/Twitter), Page Like (Facebook)
    VIEW_LONG,    // Video View (YouTube/Facebook), Post View (Instagram)
    VIEW_SHORT,   // Shorts (YouTube), Reels (Instagram/Facebook), Short Clip (Twitter)
    LIKE,         // Like across all platforms
    COMMENT       // Comment across all platforms
}
