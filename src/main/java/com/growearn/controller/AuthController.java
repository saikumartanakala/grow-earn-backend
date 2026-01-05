package com.growearn.controller;

import com.growearn.entity.Role;
import com.growearn.entity.User;
import com.growearn.repository.UserRepository;
import com.growearn.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AuthController {

    private final UserRepository userRepo;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthController(
            UserRepository userRepo,
            JwtUtil jwtUtil,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepo = userRepo;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    // ✅ CHECK EMAIL + ROLE (PUBLIC)
    @PostMapping("/check-email-role")
    public ResponseEntity<?> checkEmailRole(@RequestBody Map<String, String> req) {

        String email = req.get("email");
        String roleStr = req.get("role");

        Role role = Role.valueOf(roleStr);

        boolean exists =
                userRepo.findByEmailAndRole(email, role).isPresent();

        return ResponseEntity.ok(Map.of("exists", exists));
    }

    // ✅ SIGNUP
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody Map<String, String> req) {

        String email = req.get("email");
        String password = req.get("password");
        Role role = Role.valueOf(req.get("role"));

        if (userRepo.findByEmailAndRole(email, role).isPresent()) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", "Account already exists"));
        }

        User user = new User(
                email,
                passwordEncoder.encode(password),
                role
        );

        userRepo.save(user);

        String token = jwtUtil.generateToken(user.getId(), role.name());

        return ResponseEntity.ok(
                Map.of("token", token, "role", role.name())
        );
    }

    // ✅ LOGIN
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> req) {

        String email = req.get("email");
        String password = req.get("password");
        Role role = Role.valueOf(req.get("role"));

        User user = userRepo.findByEmailAndRole(email, role)
                .orElse(null);

        if (user == null ||
                !passwordEncoder.matches(password, user.getPassword())) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid credentials"));
        }

        String token = jwtUtil.generateToken(user.getId(), role.name());

        return ResponseEntity.ok(
                Map.of("token", token, "role", role.name())
        );
    }
}
