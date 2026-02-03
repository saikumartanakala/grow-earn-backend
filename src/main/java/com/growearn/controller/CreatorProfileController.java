package com.growearn.controller;

import com.growearn.entity.User;
import com.growearn.entity.UserViolation;
import com.growearn.entity.AccountStatus;
import com.growearn.repository.UserRepository;
import com.growearn.repository.UserViolationRepository;
import com.growearn.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/creator/profile")
@CrossOrigin(origins = {"http://localhost:5173", "http://192.168.55.104:5173"}, allowCredentials = "true")
public class CreatorProfileController {

    private static final Logger logger = LoggerFactory.getLogger(CreatorProfileController.class);

    private final UserRepository userRepository;
    private final UserViolationRepository violationRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public CreatorProfileController(UserRepository userRepository, UserViolationRepository violationRepository,
                                     JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.violationRepository = violationRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    private Long extractUserId(HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return jwtUtil.extractUserId(auth.substring(7));
        }
        return null;
    }

    /**
     * GET /api/creator/profile
     * Get current creator's profile
     */
    @GetMapping
    public ResponseEntity<?> getProfile(HttpServletRequest req) {
        Long userId = extractUserId(req);
        if (userId == null) {
            return ResponseEntity.status(401)
                .body(Map.of("success", false, "message", "Unauthorized"));
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404)
                .body(Map.of("success", false, "message", "User not found"));
        }

        User user = userOpt.get();
        return ResponseEntity.ok(userToProfileMap(user));
    }

    /**
     * PUT /api/creator/profile
     * Update current creator's profile
     */
    @PutMapping
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        Long userId = extractUserId(req);
        if (userId == null) {
            return ResponseEntity.status(401)
                .body(Map.of("success", false, "message", "Unauthorized"));
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404)
                .body(Map.of("success", false, "message", "User not found"));
        }

        User user = userOpt.get();

        // Update fields if provided
        if (body.containsKey("fullName")) {
            user.setFullName(String.valueOf(body.get("fullName")));
        }
        // Accept both 'phoneNumber' and 'whatsappNumber' from frontend
        if (body.containsKey("whatsappNumber")) {
            String phone = String.valueOf(body.get("whatsappNumber"));
            user.setPhoneNumber(phone.equals("null") ? null : phone);
        } else if (body.containsKey("phoneNumber")) {
            String phone = String.valueOf(body.get("phoneNumber"));
            user.setPhoneNumber(phone.equals("null") ? null : phone);
        }
        if (body.containsKey("channelName")) {
            user.setChannelName(String.valueOf(body.get("channelName")));
        }
        if (body.containsKey("profilePicUrl")) {
            user.setProfilePicUrl(String.valueOf(body.get("profilePicUrl")));
        }

        userRepository.save(user);
        logger.info("Creator profile updated for user {}", userId);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Profile updated successfully",
            "profile", userToProfileMap(user)
        ));
    }

    /**
     * PUT /api/creator/profile/password
     * Update password
     */
    @PutMapping("/password")
    public ResponseEntity<?> updatePassword(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        Long userId = extractUserId(req);
        if (userId == null) {
            return ResponseEntity.status(401)
                .body(Map.of("success", false, "message", "Unauthorized"));
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404)
                .body(Map.of("success", false, "message", "User not found"));
        }

        String currentPassword = body.containsKey("currentPassword") ? String.valueOf(body.get("currentPassword")) : null;
        String newPassword = body.containsKey("newPassword") ? String.valueOf(body.get("newPassword")) : null;
        String confirmPassword = body.containsKey("confirmPassword") ? String.valueOf(body.get("confirmPassword")) : null;

        if (currentPassword == null || newPassword == null || confirmPassword == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "All password fields are required"));
        }

        if (!newPassword.equals(confirmPassword)) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "New passwords do not match"));
        }

        if (newPassword.length() < 6) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Password must be at least 6 characters"));
        }

        User user = userOpt.get();

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Current password is incorrect"));
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        logger.info("Password updated for creator {}", userId);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Password updated successfully"
        ));
    }

    // ========== Helper Methods ==========

    private Map<String, Object> userToProfileMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("email", user.getEmail());
        map.put("role", user.getRole().name());
        map.put("fullName", user.getFullName());
        map.put("phoneNumber", user.getPhoneNumber());
        map.put("whatsappNumber", user.getPhoneNumber()); // Also return as whatsappNumber for frontend
        map.put("channelName", user.getChannelName());
        map.put("profilePicUrl", user.getProfilePicUrl());
        map.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
        
        // Account Status Information
        Map<String, Object> accountStatus = new HashMap<>();
        AccountStatus status = user.getStatus() != null ? user.getStatus() : AccountStatus.ACTIVE;
        accountStatus.put("status", status.name());
        accountStatus.put("statusLabel", getStatusLabel(status));
        accountStatus.put("statusMessage", getStatusMessage(status, user.getSuspensionUntil()));
        
        // Suspension details if applicable
        if (status == AccountStatus.SUSPENDED && user.getSuspensionUntil() != null) {
            accountStatus.put("suspensionUntil", user.getSuspensionUntil().toString());
            accountStatus.put("isSuspended", true);
            // Check if suspension has expired
            if (user.getSuspensionUntil().isBefore(LocalDateTime.now())) {
                accountStatus.put("suspensionExpired", true);
            } else {
                accountStatus.put("suspensionExpired", false);
            }
        } else {
            accountStatus.put("isSuspended", false);
        }
        
        accountStatus.put("isBanned", status == AccountStatus.BANNED);
        accountStatus.put("isActive", status == AccountStatus.ACTIVE);
        
        // Strike count
        int strikeCount = violationRepository.getMaxStrikeCount(user.getId());
        accountStatus.put("strikeCount", strikeCount);
        accountStatus.put("maxStrikes", 3);
        
        // Recent violations (last 5)
        List<UserViolation> violations = violationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        List<Map<String, Object>> recentViolations = new ArrayList<>();
        int count = 0;
        for (UserViolation v : violations) {
            if (count >= 5) break;
            Map<String, Object> vMap = new HashMap<>();
            vMap.put("type", v.getViolationType());
            vMap.put("description", v.getDescription());
            vMap.put("actionTaken", v.getActionTaken());
            vMap.put("createdAt", v.getCreatedAt() != null ? v.getCreatedAt().toString() : null);
            recentViolations.add(vMap);
            count++;
        }
        accountStatus.put("recentViolations", recentViolations);
        accountStatus.put("totalViolations", violations.size());
        
        map.put("accountStatus", accountStatus);
        return map;
    }
    
    private String getStatusLabel(AccountStatus status) {
        return switch (status) {
            case ACTIVE -> "Active";
            case SUSPENDED -> "Suspended";
            case BANNED -> "Permanently Banned";
            default -> "Unknown";
        };
    }
    
    private String getStatusMessage(AccountStatus status, LocalDateTime suspensionUntil) {
        return switch (status) {
            case ACTIVE -> "Your account is in good standing.";
            case SUSPENDED -> {
                if (suspensionUntil != null) {
                    if (suspensionUntil.isBefore(LocalDateTime.now())) {
                        yield "Your suspension has expired. Please contact support if access is still restricted.";
                    }
                    yield "Your account is temporarily suspended until " + suspensionUntil.toLocalDate() + ".";
                }
                yield "Your account is temporarily suspended.";
            }
            case BANNED -> "Your account has been permanently banned due to multiple policy violations.";
            default -> "";
        };
    }
}
