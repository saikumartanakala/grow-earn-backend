package com.growearn.controller;

import com.growearn.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/viewer")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class ViewerDashboardController {

    private final JwtUtil jwtUtil;

    public ViewerDashboardController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> getViewerDashboard(HttpServletRequest request) {

        String token = request.getHeader("Authorization").substring(7);
        Long userId = jwtUtil.extractUserId(token);

        return Map.of(
                "userId", userId,
                "message", "Welcome Viewer",
                "status", "ACTIVE"
        );
    }
}
