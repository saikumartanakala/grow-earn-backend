package com.growearn.controller;

import jakarta.servlet.http.HttpServletRequest;

import com.growearn.entity.Role;
import com.growearn.entity.User;
import com.growearn.repository.UserRepository;
import com.growearn.repository.CampaignRepository;
import com.growearn.repository.DeviceRegistryRepository;
import com.growearn.repository.LoginAuditRepository;
import com.growearn.entity.DeviceRegistry;
import com.growearn.entity.LoginAudit;
import com.growearn.repository.CreatorStatsRepository;
// legacy viewer tasks removed; new flow uses tasks + viewer_tasks entities
import com.growearn.entity.CreatorStats;
import com.growearn.security.JwtUtil;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import com.growearn.dto.AuthResponseDTO;
import java.util.List;
import com.growearn.entity.Campaign;
import java.util.Optional;

import jakarta.validation.ConstraintViolationException;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:5173", "http://192.168.55.104:5173"}, allowCredentials = "true")

public class AuthController {

        private final UserRepository userRepo;
        private final JwtUtil jwtUtil;
        private final PasswordEncoder passwordEncoder;
        private final CreatorStatsRepository creatorStatsRepo;
        private final CampaignRepository campaignRepo;
        private final DeviceRegistryRepository deviceRegistryRepo;
        private final LoginAuditRepository loginAuditRepo;
        private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AuthController.class);
        private final jakarta.persistence.EntityManager entityManager;

                public AuthController(
                    UserRepository userRepo,
                    JwtUtil jwtUtil,
                    PasswordEncoder passwordEncoder,
                    CreatorStatsRepository creatorStatsRepo,
                    CampaignRepository campaignRepo,
                    DeviceRegistryRepository deviceRegistryRepo,
                    LoginAuditRepository loginAuditRepo,
                    jakarta.persistence.EntityManager entityManager
                ) {
                    this.userRepo = userRepo;
                    this.jwtUtil = jwtUtil;
                    this.passwordEncoder = passwordEncoder;
                    this.creatorStatsRepo = creatorStatsRepo;
                    this.campaignRepo = campaignRepo;
                    this.deviceRegistryRepo = deviceRegistryRepo;
                    this.loginAuditRepo = loginAuditRepo;
                    this.entityManager = entityManager;
                }

    // ✅ CHECK EMAIL + ROLE (PUBLIC)
    @PostMapping("/check-email-role")
    public ResponseEntity<?> checkEmailRole(@RequestBody Map<String, String> req) {

        String email = req.get("email");
        String roleStr = req.get("role");

        Role role = Role.valueOf(roleStr.toUpperCase());

        boolean exists =
                userRepo.findByEmailAndRole(email, role).isPresent();

        return ResponseEntity.ok(Map.of("exists", exists));
    }

    // ✅ SIGNUP
    @PostMapping("/signup")
    @Transactional
    public ResponseEntity<?> signup(@RequestBody Map<String, String> req, @RequestHeader(value = "X-Device-Fingerprint", required = false) String deviceFingerprint, HttpServletRequest request) {
        String email = req.get("email");
        String password = req.get("password");
        Role role = Role.valueOf(req.get("role").toUpperCase());
        Role dbRole = (role == Role.VIEWER) ? Role.USER : role;
        String clientIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        // Device enforcement: block if device already registered
        if (deviceFingerprint == null || deviceFingerprint.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Device fingerprint required"));
        }
        // Add logging and forced cleanup before inserting into device_registry
        logger.info("Deleting existing device_registry rows for device_fingerprint: " + deviceFingerprint);
        try {
            deviceRegistryRepo.deleteByDeviceFingerprint(deviceFingerprint);
        } catch (Exception e) {
            logger.error("Failed to delete device registry entry for fingerprint: " + deviceFingerprint, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Failed to clean up device registry"));
        }

        // Force cleanup for legacy rows with user_id IS NULL
        logger.info("Deleting legacy rows with user_id IS NULL");
        entityManager.createNativeQuery("DELETE FROM device_registry WHERE user_id IS NULL").executeUpdate();
        logger.info("Deleted legacy rows with user_id IS NULL");

        // Defensive: always delete any existing device_registry row for this fingerprint
        deviceRegistryRepo.deleteByDeviceFingerprint(deviceFingerprint);

        // Validate email uniqueness before proceeding
        if (userRepo.findByEmail(email).isPresent()) {
            logger.warn("Duplicate email detected: " + email);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Email already exists"));
        }

        // Validate device fingerprint uniqueness before proceeding
        if (userRepo.findByDeviceFingerprint(deviceFingerprint).isPresent()) {
            logger.warn("Duplicate device fingerprint detected: " + deviceFingerprint);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Device fingerprint already exists"));
        }

        // Check for duplicate email+role (check both USER and VIEWER for viewers)
        if (userRepo.findByEmailAndRole(email, dbRole).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Account with this email and role already exists"));
        }
        if ((role == Role.VIEWER || role == Role.USER) && userRepo.findByEmailAndRole(email, role == Role.VIEWER ? Role.USER : Role.VIEWER).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Account with this email and role already exists"));
        }

        User newUser = new User(email, passwordEncoder.encode(password), dbRole);
        newUser.setDeviceFingerprint(deviceFingerprint);
        newUser.setJoined(java.time.LocalDateTime.now());
        newUser.setFirstIp(clientIp);
        newUser.setLastActive(java.time.LocalDateTime.now());
        logger.info("Saving new user with email: " + email);
        User savedUser = userRepo.save(newUser);
        logger.info("Saved user with ID: " + savedUser.getId());

        // Re-fetch the user to ensure ID is populated
        User fetchedUser = userRepo.findById(savedUser.getId()).orElseThrow(() -> new IllegalStateException("User not found after save"));
        logger.info("Fetched user with ID: " + fetchedUser.getId());

        // Assign user_id to device registry
        logger.info("Assigning user_id to device registry: " + fetchedUser.getId());
        try {
            // Check if device registry entry already exists
            Optional<DeviceRegistry> existingRegistry = deviceRegistryRepo.findByDeviceFingerprint(deviceFingerprint);
            if (existingRegistry.isPresent()) {
                DeviceRegistry registry = existingRegistry.get();
                registry.setLastIp(clientIp);
                registry.setLastSeenAt(java.time.LocalDateTime.now());
                deviceRegistryRepo.save(registry);
                logger.info("Updated existing DeviceRegistry entry for user ID: " + registry.getUser().getId());
            } else {
                DeviceRegistry deviceRegistry = new DeviceRegistry();
                deviceRegistry.setUser(fetchedUser);
                deviceRegistry.setDeviceFingerprint(deviceFingerprint);
                deviceRegistry.setRole(role.name());
                deviceRegistry.setFirstIp(clientIp);
                deviceRegistry.setLastIp(clientIp);
                deviceRegistry.setLastSeenAt(java.time.LocalDateTime.now());
                deviceRegistryRepo.save(deviceRegistry);
                logger.info("DeviceRegistry entry created with user ID: " + deviceRegistry.getUser().getId());
            }
        } catch (ConstraintViolationException e) {
            logger.error("Constraint violation while saving device registry entry", e);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Device fingerprint already exists"));
        } catch (Exception e) {
            logger.error("Failed to save device registry entry", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Failed to save device registry entry"));
        }

        logLoginAudit(fetchedUser, clientIp, deviceFingerprint, userAgent, "REGISTER");

        if (role == Role.CREATOR) {
            creatorStatsRepo.findByCreatorId(fetchedUser.getId()).orElseGet(() -> creatorStatsRepo.save(new CreatorStats(fetchedUser.getId())));
        }

        String token = jwtUtil.generateToken(fetchedUser.getId(), fetchedUser.getRole().name());
        return ResponseEntity.ok(new AuthResponseDTO(token, fetchedUser.getRole().name()));
    }

    // ✅ LOGIN
    @PostMapping("/login")
    @Transactional
    public ResponseEntity<?> login(@RequestBody Map<String, String> req, @RequestHeader(value = "X-Device-Fingerprint", required = false) String deviceFingerprint, HttpServletRequest request) {
        String email = req.get("email");
        String password = req.get("password");
        String roleStr = req.get("role");
        if (roleStr == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Role is required"));
        }
        Role role;
        try {
            role = Role.valueOf(roleStr.toUpperCase());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Invalid role"));
        }
        String clientIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        // Fetch user before device enforcement logic
        User user = userRepo.findByEmailAndRole(email, role).orElse(null);
        if (user == null && (role == Role.VIEWER || role == Role.USER)) {
            Role altRole = (role == Role.VIEWER) ? Role.USER : Role.VIEWER;
            user = userRepo.findByEmailAndRole(email, altRole).orElse(null);
        }
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found"));
        }

        // Device enforcement: allow updating device fingerprint if mismatched
        if (deviceFingerprint == null || deviceFingerprint.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Device fingerprint is required for login"));
        }
        DeviceRegistry reg = deviceRegistryRepo.findByDeviceFingerprint(deviceFingerprint).orElse(null);
        if (reg != null && (reg.getUser() == null || !reg.getUser().getEmail().equals(email))) {
            logLoginAudit(null, clientIp, deviceFingerprint, userAgent, "Device mismatch detected. Updating device fingerprint.");
            reg.setUser(user);
            reg.setRole(user.getRole().name());
            reg.setLastIp(clientIp);
            reg.setLastSeenAt(java.time.LocalDateTime.now());
            deviceRegistryRepo.save(reg);
        }

        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            logLoginAudit(null, clientIp, deviceFingerprint, userAgent, "FAILED: Invalid credentials");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid credentials or role"));
        }

        // Check account status before allowing login
        if (user.getStatus() != null) {
            if (user.getStatus() == com.growearn.entity.AccountStatus.BANNED) {
                logLoginAudit(user, clientIp, deviceFingerprint, userAgent, "BLOCKED: BANNED");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Your account has been permanently banned."));
            }
            if (user.getStatus() == com.growearn.entity.AccountStatus.SUSPENDED) {
                if (user.getSuspensionUntil() == null || java.time.LocalDateTime.now().isBefore(user.getSuspensionUntil())) {
                    String msg = user.getSuspensionUntil() != null ? "Your account is suspended until " + user.getSuspensionUntil() : "Your account is suspended. Please contact support.";
                    logLoginAudit(user, clientIp, deviceFingerprint, userAgent, "BLOCKED: SUSPENDED");
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", msg));
                } else {
                    // Suspension expired, auto-reactivate
                    user.setStatus(com.growearn.entity.AccountStatus.ACTIVE);
                    user.setSuspensionUntil(null);
                    userRepo.save(user);
                }
            }
        }

        // Update user fields
        user.setLastIp(clientIp);
        user.setLastActive(java.time.LocalDateTime.now());
        userRepo.save(user);

        // Update device registry fields
        DeviceRegistry deviceRegistry = deviceRegistryRepo.findByDeviceFingerprint(deviceFingerprint).orElse(new DeviceRegistry());
        deviceRegistry.setUser(user);
        deviceRegistry.setDeviceFingerprint(deviceFingerprint);
        deviceRegistry.setRole(user.getRole().name());
        deviceRegistry.setFirstIp(clientIp);
        deviceRegistry.setLastIp(clientIp);
        deviceRegistry.setLastSeenAt(java.time.LocalDateTime.now());
        deviceRegistryRepo.save(deviceRegistry);

        // Log audit
        logLoginAudit(user, clientIp, deviceFingerprint, userAgent, "LOGIN");

        String token = jwtUtil.generateToken(user.getId(), user.getRole().name());
        return ResponseEntity.ok(new AuthResponseDTO(token, user.getRole().name()));
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        return xf != null ? xf.split(",")[0] : request.getRemoteAddr();
    }

    private void logLoginAudit(User user, String ip, String deviceFingerprint, String userAgent, String action) {
        LoginAudit audit = new LoginAudit();
        audit.setUser(user);
        audit.setIpAddress(ip);
        audit.setDeviceFingerprint(deviceFingerprint);
        audit.setUserAgent(userAgent);
        audit.setCreatedAt(java.time.LocalDateTime.now());
        // Null out user before saving to avoid lazy loading issues
        User tempUser = audit.getUser();
        audit.setUser(null);
        loginAuditRepo.save(audit);
        // Optionally: log structured JSON
        System.out.println(String.format("{\"event\":\"login_audit\",\"userId\":%s,\"ip\":\"%s\",\"device\":\"%s\",\"action\":\"%s\"}", tempUser != null ? tempUser.getId() : null, ip, deviceFingerprint, action));
        // Optionally: log structured JSON
        System.out.println(String.format("{\"event\":\"login_audit\",\"userId\":%s,\"ip\":\"%s\",\"device\":\"%s\",\"action\":\"%s\"}", user != null ? user.getId() : null, ip, deviceFingerprint, action));
    }
}
