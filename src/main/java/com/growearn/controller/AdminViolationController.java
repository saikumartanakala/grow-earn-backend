package com.growearn.controller;

import com.growearn.entity.UserViolation;
import com.growearn.service.UserViolationService;
import com.growearn.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/violations")
public class AdminViolationController {

    private static final Logger logger = LoggerFactory.getLogger(AdminViolationController.class);

    private final UserViolationService violationService;
    private final JwtUtil jwtUtil;

    public AdminViolationController(UserViolationService violationService, JwtUtil jwtUtil) {
        this.violationService = violationService;
        this.jwtUtil = jwtUtil;
    }

    private Long extractAdminId(HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return jwtUtil.extractUserId(auth.substring(7));
        }
        return null;
    }

    /**
     * POST /api/admin/violations/warn
     * Issue a warning to a user (does not increase strike count)
     * Body: { userId, violationType, description }
     */
    @PostMapping("/warn")
    public ResponseEntity<?> warnUser(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        Long adminId = extractAdminId(req);
        Long userId = extractUserId(body);
        String violationType = body.containsKey("violationType") ? String.valueOf(body.get("violationType")) : "POLICY_VIOLATION";
        String description = body.containsKey("description") ? String.valueOf(body.get("description")) : "";

        if (userId == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "userId is required"));
        }

        try {
            Map<String, Object> result = violationService.warnUser(userId, violationType, description, adminId);
            logger.info("Admin {} warned user {} for {}", adminId, userId, violationType);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * POST /api/admin/violations/strike
     * Issue a strike to a user with automatic enforcement:
     * - 1st strike = warning
     * - 2nd strike = 7-day suspension
     * - 3rd strike = permanent ban
     * Body: { userId, violationType, description }
     */
    @PostMapping("/strike")
    public ResponseEntity<?> strikeUser(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        Long adminId = extractAdminId(req);
        Long userId = extractUserId(body);
        String violationType = body.containsKey("violationType") ? String.valueOf(body.get("violationType")) : "POLICY_VIOLATION";
        String description = body.containsKey("description") ? String.valueOf(body.get("description")) : "";

        if (userId == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "userId is required"));
        }

        try {
            Map<String, Object> result = violationService.strikeUser(userId, violationType, description, adminId);
            logger.info("Admin {} issued strike to user {} for {}. Result: {}", 
                adminId, userId, violationType, result.get("action"));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * GET /api/admin/violations/{userId}
     * Get violation history for a user
     */
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserViolations(@PathVariable Long userId) {
        try {
            Map<String, Object> summary = violationService.getViolationSummary(userId);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * GET /api/admin/violations/strikes/{userId}
     * Get strike count for a user
     */
    @GetMapping("/strikes/{userId}")
    public ResponseEntity<?> getStrikeCount(@PathVariable Long userId) {
        try {
            int strikes = violationService.getStrikeCount(userId);
            return ResponseEntity.ok(Map.of(
                "userId", userId,
                "strikeCount", strikes,
                "maxStrikes", 3,
                "strikesUntilBan", Math.max(0, 3 - strikes)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * DELETE /api/admin/violations/{violationId}
     * Remove a specific violation
     */
    @DeleteMapping("/{violationId}")
    public ResponseEntity<?> deleteViolation(@PathVariable Long violationId, HttpServletRequest req) {
        Long adminId = extractAdminId(req);
        try {
            Map<String, Object> result = violationService.deleteViolation(violationId, adminId);
            logger.info("Admin {} deleted violation {}", adminId, violationId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Failed to delete violation {}: {}", violationId, e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * DELETE /api/admin/violations/user/{userId}
     * Clear all violations for a user
     */
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<?> clearUserViolations(@PathVariable Long userId, HttpServletRequest req) {
        Long adminId = extractAdminId(req);
        try {
            Map<String, Object> result = violationService.clearViolations(userId, adminId);
            logger.info("Admin {} cleared all violations for user {}", adminId, userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Failed to clear violations for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ========== Helper Methods ==========

    private Long extractUserId(Map<String, Object> body) {
        if (body.containsKey("userId")) {
            return Long.valueOf(String.valueOf(body.get("userId")));
        }
        if (body.containsKey("id")) {
            return Long.valueOf(String.valueOf(body.get("id")));
        }
        return null;
    }
}
