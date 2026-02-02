package com.growearn.controller;

import com.growearn.entity.Role;
import com.growearn.entity.User;
import com.growearn.repository.UserRepository;
import com.growearn.repository.CampaignRepository;
import com.growearn.repository.CreatorStatsRepository;
// legacy viewer tasks removed; new flow uses tasks + viewer_tasks entities
import com.growearn.entity.CreatorStats;
import com.growearn.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;
import com.growearn.entity.Campaign;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")

public class AuthController {
        private final UserRepository userRepo;
        private final JwtUtil jwtUtil;
        private final PasswordEncoder passwordEncoder;
        private final CreatorStatsRepository creatorStatsRepo;
        private final CampaignRepository campaignRepo;

                public AuthController(
                        UserRepository userRepo,
                        JwtUtil jwtUtil,
                        PasswordEncoder passwordEncoder,
                        CreatorStatsRepository creatorStatsRepo,
                        CampaignRepository campaignRepo
                ) {
                        this.userRepo = userRepo;
                        this.jwtUtil = jwtUtil;
                        this.passwordEncoder = passwordEncoder;
                        this.creatorStatsRepo = creatorStatsRepo;
                            this.campaignRepo = campaignRepo;
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
    public ResponseEntity<?> signup(@RequestBody Map<String, String> req) {

        String email = req.get("email");
        String password = req.get("password");
        Role role = Role.valueOf(req.get("role").toUpperCase());
        
        // Normalize VIEWER to USER for database storage (backward compatibility)
        Role dbRole = (role == Role.VIEWER) ? Role.USER : role;

        // Check for duplicate email+role (check both USER and VIEWER for viewers)
        if (userRepo.findByEmailAndRole(email, dbRole).isPresent()) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("message", "Account with this email and role already exists"));
        }
        // Also check the alternative role for viewers
        if ((role == Role.VIEWER || role == Role.USER) && 
            userRepo.findByEmailAndRole(email, role == Role.VIEWER ? Role.USER : Role.VIEWER).isPresent()) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("message", "Account with this email and role already exists"));
        }

        User user = new User(
                email,
                passwordEncoder.encode(password),
                dbRole
        );

        userRepo.save(user);

        // Auto-create CreatorStats for new creators
        if (role == Role.CREATOR) {
            // Only create if not exists
            creatorStatsRepo.findByCreatorId(user.getId())
                    .orElseGet(() -> creatorStatsRepo.save(new CreatorStats(user.getId())));
        }

        // New flow: do NOT pre-assign tasks to viewers. Viewers will grab tasks via /api/viewer/tasks/grab

        // Always use the actual role from the saved user (database) for the JWT
        User savedUser = userRepo.findByEmail(email).orElse(user);
        String token = jwtUtil.generateToken(savedUser.getId(), savedUser.getRole().name());

        return ResponseEntity.ok(
                Map.of("token", token, "role", savedUser.getRole().name())
        );
    }

    // ✅ LOGIN
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> req) {
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
        
        // For viewers, check both VIEWER and USER roles (backward compatibility)
        User user = userRepo.findByEmailAndRole(email, role).orElse(null);
        if (user == null && (role == Role.VIEWER || role == Role.USER)) {
            // Try the alternative role for viewers
            Role altRole = (role == Role.VIEWER) ? Role.USER : Role.VIEWER;
            user = userRepo.findByEmailAndRole(email, altRole).orElse(null);
        }
        
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid credentials or role"));
        }

        // Check account status before allowing login
        if (user.getStatus() != null) {
            if (user.getStatus() == com.growearn.entity.AccountStatus.BANNED) {
                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Your account has been permanently banned."));
            }
            if (user.getStatus() == com.growearn.entity.AccountStatus.SUSPENDED) {
                if (user.getSuspensionUntil() == null || java.time.LocalDateTime.now().isBefore(user.getSuspensionUntil())) {
                    String msg = user.getSuspensionUntil() != null 
                        ? "Your account is suspended until " + user.getSuspensionUntil()
                        : "Your account is suspended. Please contact support.";
                    return ResponseEntity
                            .status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", msg));
                } else {
                    // Suspension expired, auto-reactivate
                    user.setStatus(com.growearn.entity.AccountStatus.ACTIVE);
                    user.setSuspensionUntil(null);
                    userRepo.save(user);
                }
            }
        }

        // Always use the actual role from the user entity for the JWT
        String token = jwtUtil.generateToken(user.getId(), user.getRole().name());
        return ResponseEntity.ok(
                Map.of("token", token, "role", user.getRole().name())
        );
    }
}
