package com.growearn.controller;

import com.growearn.entity.User;
import com.growearn.entity.ViewerTask;
import com.growearn.repository.UserRepository;
import com.growearn.repository.ViewerTaskRepository;
import com.growearn.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/viewer")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class ViewerDashboardController {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final ViewerTaskRepository viewerTaskRepository;

    public ViewerDashboardController(JwtUtil jwtUtil, UserRepository userRepository, ViewerTaskRepository viewerTaskRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.viewerTaskRepository = viewerTaskRepository;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> getViewerDashboard(HttpServletRequest request) {

        String token = request.getHeader("Authorization").substring(7);
        Long userId = jwtUtil.extractUserId(token);
        String role = jwtUtil.extractRole(token);

        // Get user info
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return Map.of("error", "User not found");
        }
        User user = userOpt.get();

        // Get tasks data
        List<ViewerTask> assignedTasks = viewerTaskRepository.findByViewerIdAndCompleted(userId, false);
        List<ViewerTask> completedTasks = viewerTaskRepository.findByViewerIdAndCompleted(userId, true);
        List<ViewerTask> availableTasks = viewerTaskRepository.findAvailableTasks();

        // Calculate stats
        int totalTasksCompleted = completedTasks.size();
        double totalEarnings = completedTasks.size() * 10.0; // Assume $10 per task for demo

        return Map.of(
                "user", Map.of(
                        "id", user.getId(),
                        "email", user.getEmail(),
                        "role", user.getRole()
                ),
                "stats", Map.of(
                        "totalTasksCompleted", totalTasksCompleted,
                        "totalEarnings", totalEarnings,
                        "assignedTasksCount", assignedTasks.size()
                ),
                "tasks", Map.of(
                        "assigned", assignedTasks,
                        "completed", completedTasks,
                        "available", availableTasks
                )
        );
    }

    @PostMapping("/tasks/{taskId}/claim")
    public Map<String, Object> claimTask(@PathVariable Long taskId, HttpServletRequest request) {

        String token = request.getHeader("Authorization").substring(7);
        Long userId = jwtUtil.extractUserId(token);

        Optional<ViewerTask> taskOpt = viewerTaskRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            return Map.of("error", "Task not found");
        }

        ViewerTask task = taskOpt.get();
        if (task.getViewerId() != null) {
            return Map.of("error", "Task already claimed");
        }

        task.setViewerId(userId);
        viewerTaskRepository.save(task);

        return Map.of("success", true, "message", "Task claimed successfully");
    }
}
