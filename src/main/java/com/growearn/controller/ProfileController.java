package com.growearn.controller;

import com.growearn.entity.User;
import com.growearn.repository.UserRepository;
import com.growearn.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = {"http://localhost:5173", "http://192.168.55.104:5173"}, allowCredentials = "true")
public class ProfileController {

    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public ProfileController(UserRepository userRepository, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
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
     * GET /api/profile
     * Get current user's profile
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
     * PUT /api/profile
     * Update current user's profile
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
        if (body.containsKey("phoneNumber")) {
            String phone = String.valueOf(body.get("phoneNumber"));
            // Basic phone validation
            if (phone != null && !phone.isEmpty() && !phone.matches("^[+]?[0-9]{10,15}$")) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Invalid phone number format"));
            }
            user.setPhoneNumber(phone);
        }
        if (body.containsKey("channelName")) {
            user.setChannelName(String.valueOf(body.get("channelName")));
        }
        if (body.containsKey("profilePicUrl")) {
            user.setProfilePicUrl(String.valueOf(body.get("profilePicUrl")));
        }
        if (body.containsKey("upiId")) {
            String upiId = String.valueOf(body.get("upiId"));
            // Basic UPI ID validation
            if (upiId != null && !upiId.isEmpty() && !upiId.matches("^[a-zA-Z0-9._-]+@[a-zA-Z]+$")) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Invalid UPI ID format (e.g., username@bank)"));
            }
            user.setUpiId(upiId);
        }

        userRepository.save(user);
        logger.info("Profile updated for user {}", userId);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Profile updated successfully",
            "profile", userToProfileMap(user)
        ));
    }

    /**
     * PUT /api/profile/password
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
        logger.info("Password updated for user {}", userId);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Password updated successfully"
        ));
    }

    /**
     * POST /api/profile/upload-pic
     * Upload profile picture (returns URL to store)
     * Note: In production, this would upload to cloud storage
     * For now, we accept a URL directly
     */
    @PostMapping("/upload-pic")
    public ResponseEntity<?> uploadProfilePic(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        Long userId = extractUserId(req);
        if (userId == null) {
            return ResponseEntity.status(401)
                .body(Map.of("success", false, "message", "Unauthorized"));
        }

        String imageUrl = body.containsKey("imageUrl") ? String.valueOf(body.get("imageUrl")) : null;
        if (imageUrl == null || imageUrl.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Image URL is required"));
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404)
                .body(Map.of("success", false, "message", "User not found"));
        }

        User user = userOpt.get();
        user.setProfilePicUrl(imageUrl);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Profile picture updated",
            "profilePicUrl", imageUrl
        ));
    }

    /**
     * GET /api/profile/check-complete
     * Check if profile is complete (for showing prompts)
     */
    @GetMapping("/check-complete")
    public ResponseEntity<?> checkProfileComplete(HttpServletRequest req) {
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
        boolean isComplete = isProfileComplete(user);
        Map<String, Boolean> missingFields = getMissingFields(user);

        return ResponseEntity.ok(Map.of(
            "isComplete", isComplete,
            "missingFields", missingFields
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
        map.put("channelName", user.getChannelName());
        map.put("profilePicUrl", user.getProfilePicUrl());
        map.put("upiId", user.getUpiId());
        map.put("isVerified", user.getIsVerified());
        map.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
        map.put("isProfileComplete", isProfileComplete(user));
        return map;
    }

    private boolean isProfileComplete(User user) {
        // Required fields: fullName, phoneNumber, upiId
        // channelName is required only for creators
        boolean basicComplete = user.getFullName() != null && !user.getFullName().isEmpty()
            && user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()
            && user.getUpiId() != null && !user.getUpiId().isEmpty();

        if (user.getRole().name().equals("CREATOR")) {
            return basicComplete && user.getChannelName() != null && !user.getChannelName().isEmpty();
        }
        return basicComplete;
    }

    private Map<String, Boolean> getMissingFields(User user) {
        Map<String, Boolean> missing = new HashMap<>();
        missing.put("fullName", user.getFullName() == null || user.getFullName().isEmpty());
        missing.put("phoneNumber", user.getPhoneNumber() == null || user.getPhoneNumber().isEmpty());
        missing.put("upiId", user.getUpiId() == null || user.getUpiId().isEmpty());
        if (user.getRole().name().equals("CREATOR")) {
            missing.put("channelName", user.getChannelName() == null || user.getChannelName().isEmpty());
        }
        return missing;
    }
}
