package com.growearn.controller;

import com.growearn.entity.User;
import com.growearn.entity.Earning;
import com.growearn.repository.UserRepository;
import com.growearn.repository.ViewerTaskEntityRepository;
import com.growearn.repository.TaskRepository;
import com.growearn.repository.EarningRepository;
import com.growearn.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

@RestController
@RequestMapping("/api/viewer")
public class ViewerDashboardController {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final ViewerTaskEntityRepository viewerTaskEntityRepository;
    private final TaskRepository taskRepository;
    private final EarningRepository earningRepository;
    private final com.growearn.service.ViewerTaskFlowService viewerTaskFlowService;

    public ViewerDashboardController(JwtUtil jwtUtil,
                                      UserRepository userRepository,
                                      ViewerTaskEntityRepository viewerTaskEntityRepository,
                                      TaskRepository taskRepository,
                                      EarningRepository earningRepository,
                                      com.growearn.service.ViewerTaskFlowService viewerTaskFlowService) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.viewerTaskEntityRepository = viewerTaskEntityRepository;
        this.taskRepository = taskRepository;
        this.earningRepository = earningRepository;
        this.viewerTaskFlowService = viewerTaskFlowService;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> getViewerDashboard(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            return Map.of("error", "Unauthorized");
        }
        Long userId;
        try {
            userId = Long.parseLong(auth.getPrincipal().toString());
        } catch (Exception e) {
            return Map.of("error", "Invalid principal");
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return Map.of("error", "User not found");
        }

        long completedTasksCount = viewerTaskEntityRepository.countByViewerIdAndStatus(userId, "PAID");

        Double totalEarnings = earningRepository.sumEarningsByViewerId(userId);
        if (totalEarnings == null) totalEarnings = 0.0;

        long availableTasksCount = taskRepository.countByStatus("OPEN");

        return Map.of(
            "completedTasksCount", completedTasksCount,
            "totalEarnings", totalEarnings,
            "availableTasksCount", availableTasksCount
        );
    }
    @GetMapping("/earnings")
    public Map<String, Object> getViewerEarnings(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            return Map.of("error", "Unauthorized");
        }
        Long userId = Long.parseLong(auth.getPrincipal().toString());

        List<Earning> earningsList = earningRepository.findByViewerId(userId);
        Double totalEarnings = earningRepository.sumEarningsByViewerId(userId);
        if (totalEarnings == null) totalEarnings = 0.0;

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("totalEarnings", totalEarnings);
        result.put("earnings", earningsList);
        return result;
    }

    @PostMapping("/tasks/{taskId}/claim")
    public Map<String, Object> claimTask(@PathVariable Long taskId, HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            return Map.of("error", "Unauthorized");
        }
        Long userId = Long.parseLong(auth.getPrincipal().toString());

        try {
            viewerTaskFlowService.grabTask(taskId, userId);
            return Map.of("success", true, "message", "Task grabbed successfully");
        } catch (Exception ex) {
            return Map.of("error", ex.getMessage());
        }
    }
}
