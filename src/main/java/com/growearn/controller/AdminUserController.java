package com.growearn.controller;

import com.growearn.entity.*;
import com.growearn.service.UserService;
import com.growearn.service.UserViolationService;
import com.growearn.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AdminUserController {

    private static final Logger logger = LoggerFactory.getLogger(AdminUserController.class);

    private final UserService userService;
    private final UserViolationService violationService;
    private final JwtUtil jwtUtil;

    public AdminUserController(UserService userService, UserViolationService violationService, JwtUtil jwtUtil) {
        this.userService = userService;
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
     * GET /api/admin/users
     * List all users with their status
     */
    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        List<User> users = userService.findAllUsers();
        List<Map<String, Object>> userList = users.stream()
            .map(this::userToMap)
            .collect(Collectors.toList());
        return ResponseEntity.ok(userList);
    }

    /**
     * GET /api/admin/users/{id}
     * Get user details
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id) {
        return userService.findById(id)
            .map(user -> ResponseEntity.ok(userToMap(user)))
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/admin/users/role/{role}
     * List users by role (VIEWER, CREATOR, ADMIN)
     */
    @GetMapping("/role/{role}")
    public ResponseEntity<?> getUsersByRole(@PathVariable String role) {
        try {
            Role r = Role.valueOf(role.toUpperCase());
            List<User> users = userService.findByRole(r);
            List<Map<String, Object>> userList = users.stream()
                .map(this::userToMap)
                .collect(Collectors.toList());
            return ResponseEntity.ok(userList);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid role: " + role));
        }
    }

    /**
     * POST /api/admin/users/activate
     * Activate a user account
     */
    @PostMapping("/activate")
    public ResponseEntity<?> activateUser(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        Long adminId = extractAdminId(req);
        Long userId = extractUserId(body);
        
        if (userId == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "userId is required"));
        }

        try {
            User user = userService.activateUser(userId);
            logger.info("Admin {} activated user {}", adminId, userId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "User activated successfully",
                "user", userToMap(user)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * POST /api/admin/users/deactivate
     * Deactivate a user account (indefinite suspension)
     */
    @PostMapping("/deactivate")
    public ResponseEntity<?> deactivateUser(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        Long adminId = extractAdminId(req);
        Long userId = extractUserId(body);
        
        if (userId == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "userId is required"));
        }

        try {
            User user = userService.deactivateUser(userId);
            userService.forceLogout(userId, adminId, "Account deactivated");
            logger.info("Admin {} deactivated user {}", adminId, userId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "User deactivated successfully",
                "user", userToMap(user)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * POST /api/admin/users/suspend
     * Suspend a user for a specific duration
     * Body: { userId, days (default 7), reason }
     */
    @PostMapping("/suspend")
    public ResponseEntity<?> suspendUser(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        Long adminId = extractAdminId(req);
        Long userId = extractUserId(body);
        int days = body.containsKey("days") ? Integer.parseInt(String.valueOf(body.get("days"))) : 7;
        String reason = body.containsKey("reason") ? String.valueOf(body.get("reason")) : "Policy violation";
        
        if (userId == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "userId is required"));
        }

        try {
            User user = userService.suspendUser(userId, days, reason);
            userService.forceLogout(userId, adminId, "Account suspended: " + reason);
            logger.info("Admin {} suspended user {} for {} days", adminId, userId, days);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "User suspended for " + days + " days",
                "suspensionUntil", user.getSuspensionUntil().toString(),
                "user", userToMap(user)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * POST /api/admin/users/ban
     * Permanently ban a user
     */
    @PostMapping("/ban")
    public ResponseEntity<?> banUser(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        Long adminId = extractAdminId(req);
        Long userId = extractUserId(body);
        String reason = body.containsKey("reason") ? String.valueOf(body.get("reason")) : "Severe policy violation";
        
        if (userId == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "userId is required"));
        }

        try {
            User user = userService.banUser(userId, reason);
            userService.forceLogout(userId, adminId, "Account banned: " + reason);
            logger.info("Admin {} banned user {}. Reason: {}", adminId, userId, reason);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "User permanently banned",
                "user", userToMap(user)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * POST /api/admin/users/reset-password
     * Reset a user's password
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        Long adminId = extractAdminId(req);
        Long userId = extractUserId(body);
        String newPassword = body.containsKey("newPassword") ? String.valueOf(body.get("newPassword")) : null;
        
        if (userId == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "userId is required"));
        }
        
        if (newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "newPassword must be at least 6 characters"));
        }

        try {
            userService.resetPassword(userId, newPassword);
            userService.forceLogout(userId, adminId, "Password reset by admin");
            logger.info("Admin {} reset password for user {}", adminId, userId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Password reset successfully. User will need to login again."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * POST /api/admin/users/verify-creator
     * Verify a creator account
     */
    @PostMapping("/verify-creator")
    public ResponseEntity<?> verifyCreator(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        Long adminId = extractAdminId(req);
        Long userId = extractUserId(body);
        
        if (userId == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "userId is required"));
        }

        try {
            User user = userService.verifyCreator(userId);
            logger.info("Admin {} verified creator {}", adminId, userId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Creator verified successfully",
                "user", userToMap(user)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * POST /api/admin/users/unverify-creator
     * Remove verification from a creator account
     */
    @PostMapping("/unverify-creator")
    public ResponseEntity<?> unverifyCreator(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        Long adminId = extractAdminId(req);
        Long userId = extractUserId(body);
        
        if (userId == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "userId is required"));
        }

        try {
            User user = userService.unverifyCreator(userId);
            logger.info("Admin {} unverified creator {}", adminId, userId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Creator verification removed",
                "user", userToMap(user)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * POST /api/admin/users/force-logout
     * Force logout a user by revoking all their tokens
     */
    @PostMapping("/force-logout")
    public ResponseEntity<?> forceLogout(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        Long adminId = extractAdminId(req);
        Long userId = extractUserId(body);
        String reason = body.containsKey("reason") ? String.valueOf(body.get("reason")) : "Forced logout by admin";
        
        if (userId == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "userId is required"));
        }

        try {
            userService.forceLogout(userId, adminId, reason);
            logger.info("Admin {} forced logout for user {}", adminId, userId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "User logged out successfully. All active sessions terminated."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ========== Violation Endpoints (path-based) ==========

    /**
     * POST /api/admin/users/{userId}/violations/warning
     * Issue a warning to a user
     */
    @PostMapping("/{userId}/violations/warning")
    public ResponseEntity<?> issueWarning(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> body,
            HttpServletRequest req) {
        Long adminId = extractAdminId(req);
        String violationType = body.containsKey("violationType") ? String.valueOf(body.get("violationType")) : "POLICY_VIOLATION";
        String description = body.containsKey("description") ? String.valueOf(body.get("description")) : "";

        try {
            Map<String, Object> result = violationService.warnUser(userId, violationType, description, adminId);
            logger.info("Admin {} warned user {} for {}", adminId, userId, violationType);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Failed to issue warning to user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * POST /api/admin/users/{userId}/violations/strike
     * Issue a strike to a user (auto-enforcement: 1st=warning, 2nd=suspend, 3rd=ban)
     */
    @PostMapping("/{userId}/violations/strike")
    public ResponseEntity<?> issueStrike(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> body,
            HttpServletRequest req) {
        Long adminId = extractAdminId(req);
        String violationType = body.containsKey("violationType") ? String.valueOf(body.get("violationType")) : "POLICY_VIOLATION";
        String description = body.containsKey("description") ? String.valueOf(body.get("description")) : "";

        try {
            Map<String, Object> result = violationService.strikeUser(userId, violationType, description, adminId);
            logger.info("Admin {} issued strike to user {} for {}. Result: {}", 
                adminId, userId, violationType, result.get("action"));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Failed to issue strike to user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * GET /api/admin/users/{userId}/violations
     * Get violation history for a user
     */
    @GetMapping("/{userId}/violations")
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
     * GET /api/admin/users/{userId}/strikes
     * Get strike count for a user
     */
    @GetMapping("/{userId}/strikes")
    public ResponseEntity<?> getUserStrikes(@PathVariable Long userId) {
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
     * DELETE /api/admin/users/{userId}/violations/{violationId}
     * Remove a specific violation
     */
    @DeleteMapping("/{userId}/violations/{violationId}")
    public ResponseEntity<?> deleteUserViolation(@PathVariable Long userId, 
                                                  @PathVariable Long violationId, 
                                                  HttpServletRequest req) {
        Long adminId = extractAdminId(req);
        try {
            Map<String, Object> result = violationService.deleteViolation(violationId, adminId);
            logger.info("Admin {} deleted violation {} for user {}", adminId, violationId, userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Failed to delete violation {}: {}", violationId, e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * DELETE /api/admin/users/{userId}/violations
     * Clear all violations for a user
     */
    @DeleteMapping("/{userId}/violations")
    public ResponseEntity<?> clearAllUserViolations(@PathVariable Long userId, HttpServletRequest req) {
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

    private Map<String, Object> userToMap(User user) {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("id", user.getId());
        map.put("email", user.getEmail());
        map.put("role", user.getRole().name());
        map.put("status", user.getStatus() != null ? user.getStatus().name() : "ACTIVE");
        map.put("isVerified", user.getIsVerified() != null ? user.getIsVerified() : false);
        map.put("suspensionUntil", user.getSuspensionUntil() != null ? user.getSuspensionUntil().toString() : null);
        map.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
        map.put("canLogin", userService.canUserLogin(user));
        return map;
    }
}
