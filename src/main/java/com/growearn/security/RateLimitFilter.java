package com.growearn.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    @Value("${app.rate-limit.capacity:120}")
    private long capacity;

    @Value("${app.rate-limit.refill-seconds:60}")
    private long refillSeconds;

    @Value("${app.rate-limit.login.capacity:5}")
    private long loginCapacity;

    @Value("${app.rate-limit.login.refill-seconds:60}")
    private long loginRefillSeconds;

    @Value("${app.rate-limit.register.capacity:3}")
    private long registerCapacity;

    @Value("${app.rate-limit.register.refill-seconds:60}")
    private long registerRefillSeconds;

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        RateLimitRule rule = resolveRule(request);
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }
        String key = resolveClientKey(request) + ":" + rule.key;
        Bucket bucket = buckets.computeIfAbsent(key, k -> newBucket(rule.capacity, rule.refillSeconds));

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(429);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"rate_limited\",\"message\":\"Too many requests\"}");
    }

    private Bucket newBucket(long cap, long refillSec) {
        Refill refill = Refill.intervally(cap, Duration.ofSeconds(refillSec));
        Bandwidth limit = Bandwidth.classic(cap, refill);
        return Bucket.builder().addLimit(limit).build();
    }

    private String resolveClientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private RateLimitRule resolveRule(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if (!"POST".equalsIgnoreCase(method)) {
            return null;
        }
        if (path.equals("/api/auth/login") || path.equals("/auth/login")) {
            return new RateLimitRule("login", loginCapacity, loginRefillSeconds);
        }
        if (path.equals("/api/auth/signup") || path.equals("/api/auth/register") || path.equals("/auth/register")) {
            return new RateLimitRule("register", registerCapacity, registerRefillSeconds);
        }
        return null;
    }

    private static class RateLimitRule {
        final String key;
        final long capacity;
        final long refillSeconds;

        RateLimitRule(String key, long capacity, long refillSeconds) {
            this.key = key;
            this.capacity = capacity;
            this.refillSeconds = refillSeconds;
        }
    }
}
