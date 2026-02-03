
package com.growearn.controller;
import com.growearn.repository.DeviceRegistryRepository;
import com.growearn.repository.LoginAuditRepository;
import com.growearn.entity.DeviceRegistry;
import org.springframework.security.access.prepost.PreAuthorize;

import com.growearn.entity.*;
import com.growearn.dto.UserDTO;
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
@CrossOrigin(origins = {"http://localhost:5173", "http://192.168.55.104:5173"}, allowCredentials = "true")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private static final Logger logger = LoggerFactory.getLogger(AdminUserController.class);

    private final UserService userService;
    private final UserViolationService violationService;
    private final JwtUtil jwtUtil;
    private final DeviceRegistryRepository deviceRegistryRepo;
    private final LoginAuditRepository loginAuditRepo;

    public AdminUserController(UserService userService, UserViolationService violationService, JwtUtil jwtUtil, DeviceRegistryRepository deviceRegistryRepo, LoginAuditRepository loginAuditRepo) {
        this.userService = userService;
        this.violationService = violationService;
        this.jwtUtil = jwtUtil;
        this.deviceRegistryRepo = deviceRegistryRepo;
        this.loginAuditRepo = loginAuditRepo;
    }
    /**
     * GET /api/admin/devices
     * List all device registry entries
     */
    @GetMapping("/devices")
    public ResponseEntity<?> getAllDevices() {
        List<DeviceRegistry> devices = deviceRegistryRepo.findAll();
        List<Map<String, Object>> deviceList = devices.stream().map(device -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", device.getId());
            map.put("deviceFingerprint", device.getDeviceFingerprint());
            map.put("userId", device.getUser() != null ? device.getUser().getId() : null);
            map.put("role", device.getRole());
            map.put("firstIp", device.getFirstIp());
            map.put("lastIp", device.getLastIp());
            map.put("createdAt", device.getCreatedAt() != null ? device.getCreatedAt().toString() : null);
            map.put("lastSeenAt", device.getLastSeenAt() != null ? device.getLastSeenAt().toString() : null);
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(deviceList);
    }

    /**
     * GET /api/admin/users/{id}/security
     * Get user security info (device, status, last IP)
     */
    @GetMapping("/{id}/security")
    public ResponseEntity<?> getUserSecurity(@PathVariable Long id) {
        return userService.findById(id)
            .map(user -> Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "role", user.getRole().name(),
                "status", user.getStatus() != null ? user.getStatus().name() : "ACTIVE",
                "deviceFingerprint", user.getDeviceFingerprint(),
                "lastIp", user.getLastIp(),
                "suspensionUntil", user.getSuspensionUntil(),
                "createdAt", user.getCreatedAt(),
                "updatedAt", user.getUpdatedAt()
            ))
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/admin/users/{id}/reset-device
     * Remove device registry entry, clear user.device_fingerprint, revoke all tokens, log admin action
     */
    @PostMapping("/{id}/reset-device")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> resetDevice(@PathVariable Long id, HttpServletRequest req) {
        Long adminId = extractAdminId(req);
        User user = userService.findById(id).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();
        String fingerprint = user.getDeviceFingerprint();
        if (fingerprint != null) {
            deviceRegistryRepo.deleteByDeviceFingerprint(fingerprint);
        }
        user.setDeviceFingerprint(null);
        userService.forceLogout(id, adminId, "Device reset by admin");
        userService.save(user);
        logger.info("Admin {} reset device for user {}", adminId, id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Device reset, user must re-register device."));
    }

    /**
     * POST /api/admin/users/{id}/suspend
     * Suspend user (indefinite)
     */

    /**
     * POST /api/admin/users/{id}/ban
     * Ban user
     */

    /**
     * POST /api/admin/users/{id}/force-logout
     * Force logout user
     */
    @PostMapping("/{id}/force-logout")
    public ResponseEntity<?> forceLogoutByIdAdmin(@PathVariable Long id, HttpServletRequest req) {
        Long adminId = extractAdminId(req);
        userService.forceLogout(id, adminId, "Force logout by admin");
        logger.info("Admin {} forced logout for user {} via /{id}/force-logout", adminId, id);
        return ResponseEntity.ok(Map.of("success", true, "message", "User logged out, all tokens revoked."));
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
        List<UserDTO> userList = users.stream()
            .map(this::userToDTO)
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
            .map(user -> ResponseEntity.ok(userToDTO(user)))
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
            List<UserDTO> userList = users.stream()
                .map(this::userToDTO)
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
                "user", userToDTO(user)
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
                "user", userToDTO(user)
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
                "user", userToDTO(user)
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
                "user", userToDTO(user)
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
                "user", userToDTO(user)
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
                "user", userToDTO(user)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

        /**
         * POST /api/admin/users/{id}/verify
         * Verify a creator account by user ID (for frontend compatibility)
         */
        @PostMapping("/{id}/verify")
        public ResponseEntity<?> verifyCreatorById(@PathVariable Long id, HttpServletRequest req) {
            Long adminId = extractAdminId(req);
            try {
                User user = userService.verifyCreator(id);
                logger.info("Admin {} verified creator {} via /{id}/verify", adminId, id);
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Creator verified successfully",
                    "user", userToDTO(user)
                ));
            } catch (Exception e) {
                logger.error("Failed to verify creator {}: {}", id, e.getMessage());
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
            }
        }


    /**
     * POST /api/admin/users/{id}/unverify
     * Unverify a creator account by user ID
     */
    @PostMapping("/{id}/unverify")
    public ResponseEntity<?> unverifyCreatorById(@PathVariable Long id, HttpServletRequest req) {
        Long adminId = extractAdminId(req);
        try {
            User user = userService.unverifyCreator(id);
            logger.info("Admin {} unverified creator {} via /{id}/unverify", adminId, id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Creator verification removed",
                "user", userToDTO(user)
            ));
        } catch (Exception e) {
            logger.error("Failed to unverify creator {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * POST /api/admin/users/{id}/suspend
     * Suspend a user by ID (default 7 days, can be extended with ?days=)
     */
    @PostMapping("/{id}/suspend")
    public ResponseEntity<?> suspendUserById(@PathVariable Long id, @RequestParam(value = "days", required = false) Integer days, HttpServletRequest req) {
        Long adminId = extractAdminId(req);
        int suspendDays = (days != null) ? days : 7;
        String reason = "Policy violation";
        try {
            User user = userService.suspendUser(id, suspendDays, reason);
            userService.forceLogout(id, adminId, "Account suspended: " + reason);
            logger.info("Admin {} suspended user {} for {} days via /{id}/suspend", adminId, id, suspendDays);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "User suspended for " + suspendDays + " days",
                "suspensionUntil", user.getSuspensionUntil() != null ? user.getSuspensionUntil().toString() : null,
                "user", userToDTO(user)
            ));
        } catch (Exception e) {
            logger.error("Failed to suspend user {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * POST /api/admin/users/{id}/ban
     * Permanently ban a user by ID
     */
    @PostMapping("/{id}/ban")
    public ResponseEntity<?> banUserById(@PathVariable Long id, HttpServletRequest req) {
        Long adminId = extractAdminId(req);
        String reason = "Severe policy violation";
        try {
            User user = userService.banUser(id, reason);
            userService.forceLogout(id, adminId, "Account banned: " + reason);
            logger.info("Admin {} banned user {} via /{id}/ban", adminId, id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "User permanently banned",
                "user", userToDTO(user)
            ));
        } catch (Exception e) {
            logger.error("Failed to ban user {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * POST /api/admin/users/{id}/reset-password
     * Reset a user's password by ID (requires newPassword in body)
     */
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<?> resetPasswordById(@PathVariable Long id, @RequestBody Map<String, Object> body, HttpServletRequest req) {
        Long adminId = extractAdminId(req);
        String newPassword = body.containsKey("newPassword") ? String.valueOf(body.get("newPassword")) : null;
        if (newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "newPassword must be at least 6 characters"));
        }
        try {
            userService.resetPassword(id, newPassword);
            userService.forceLogout(id, adminId, "Password reset by admin");
            logger.info("Admin {} reset password for user {} via /{id}/reset-password", adminId, id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Password reset successfully. User will need to login again."
            ));
        } catch (Exception e) {
            logger.error("Failed to reset password for user {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * POST /api/admin/users/{id}/logout
     * Force logout a user by ID
     */
    @PostMapping("/{id}/logout")
    public ResponseEntity<?> forceLogoutById(@PathVariable Long id, HttpServletRequest req) {
        Long adminId = extractAdminId(req);
        String reason = "Forced logout by admin";
        try {
            userService.forceLogout(id, adminId, reason);
            logger.info("Admin {} forced logout for user {} via /{id}/logout", adminId, id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "User logged out successfully. All active sessions terminated."
            ));
        } catch (Exception e) {
            logger.error("Failed to force logout user {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * POST /api/admin/users/{id}/activate
     * Activate a user account by ID
     */
    @PostMapping("/{id}/activate")
    public ResponseEntity<?> activateUserById(@PathVariable Long id, HttpServletRequest req) {
        Long adminId = extractAdminId(req);
        try {
            User user = userService.activateUser(id);
            logger.info("Admin {} activated user {} via /{id}/activate", adminId, id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "User activated successfully",
                "user", userToDTO(user)
            ));
        } catch (Exception e) {
            logger.error("Failed to activate user {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }


    /**
     * POST /api/admin/users/{id}/logout
     * Force logout a user by ID (duplicate for compatibility)
     */
    @PostMapping("/{id}/logout-path")
    public ResponseEntity<?> forceLogoutByIdPath(@PathVariable Long id, HttpServletRequest req) {
        Long adminId = extractAdminId(req);
        String reason = "Forced logout by admin";
        try {
            userService.forceLogout(id, adminId, reason);
            logger.info("Admin {} forced logout for user {} via /{id}/logout-path", adminId, id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "User logged out successfully. All active sessions terminated."
            ));
        } catch (Exception e) {
            logger.error("Failed to force logout user {}: {}", id, e.getMessage());
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

    private UserDTO userToDTO(User user) {
        return new UserDTO(
            user.getId(),
            user.getEmail(),
            user.getRole() != null ? user.getRole().name() : null,
            user.getStatus() != null ? user.getStatus().name() : "ACTIVE",
            user.getIsVerified() != null ? user.getIsVerified() : false,
            user.getSuspensionUntil() != null ? user.getSuspensionUntil().toString() : null,
            user.getCreatedAt() != null ? user.getCreatedAt().toString() : null,
            userService.canUserLogin(user)
        );
    }
}
