package com.growearn.security;

import com.growearn.entity.RevokedToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class TokenBlacklistService {
    private static final Logger logger = LoggerFactory.getLogger(TokenBlacklistService.class);
    private static final String KEY_PREFIX = "jwt:bl:";

    private final StringRedisTemplate redisTemplate;
    private final boolean enabled;

    public TokenBlacklistService(StringRedisTemplate redisTemplate,
                                 @Value("${security.token-blacklist.enabled:true}") boolean enabled) {
        this.redisTemplate = redisTemplate;
        this.enabled = enabled;
    }

    public void blacklist(String token, Duration ttl) {
        if (!enabled) return;
        try {
            String key = KEY_PREFIX + RevokedToken.hashToken(token);
            redisTemplate.opsForValue().set(key, "1", ttl);
        } catch (Exception ex) {
            logger.warn("Failed to blacklist token in Redis: {}", ex.getMessage());
        }
    }

    public boolean isBlacklisted(String token) {
        if (!enabled) return false;
        try {
            String key = KEY_PREFIX + RevokedToken.hashToken(token);
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception ex) {
            logger.warn("Failed to check Redis blacklist: {}", ex.getMessage());
            return false;
        }
    }
}
