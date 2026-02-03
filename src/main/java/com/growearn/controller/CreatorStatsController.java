package com.growearn.controller;

import com.growearn.entity.CreatorStats;
import com.growearn.security.JwtUtil;
import com.growearn.service.CreatorStatsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/creator")
@CrossOrigin(origins = {"http://localhost:5173", "http://192.168.55.104:5173"}, allowCredentials = "true")
public class CreatorStatsController {

    private final CreatorStatsService service;
    private final JwtUtil jwtUtil;

    public CreatorStatsController(CreatorStatsService service, JwtUtil jwtUtil) {
        this.service = service;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/stats")
    public CreatorStats getCreatorStats(HttpServletRequest request) {

        String token = request.getHeader("Authorization").substring(7);
        Long userId = jwtUtil.extractUserId(token);

        return service.getOrCreateStats(userId);
    }
}
