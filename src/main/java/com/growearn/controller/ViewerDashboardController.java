package com.growearn.controller;

import com.growearn.entity.User;
import com.growearn.entity.ViewerTask;
import com.growearn.repository.UserRepository;
import com.growearn.repository.ViewerTaskRepository;
import com.growearn.repository.EarningRepository;
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
    private final EarningRepository earningRepository;

    public ViewerDashboardController(JwtUtil jwtUtil, UserRepository userRepository, ViewerTaskRepository viewerTaskRepository, EarningRepository earningRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.viewerTaskRepository = viewerTaskRepository;
        this.earningRepository = earningRepository;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> getViewerDashboard(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Map.of("error", "Unauthorized");
        }
        String token = authHeader.substring(7);
        Long userId = jwtUtil.extractUserId(token);

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return Map.of("error", "User not found");
        }

        List<ViewerTask> completedTasks = viewerTaskRepository.findByViewerIdAndCompleted(userId, true);

        int totalSubscriptions = (int) completedTasks.stream().filter(t -> "SUBSCRIBE".equalsIgnoreCase(t.getTaskType())).count();
        int videoViews = (int) completedTasks.stream().filter(t -> "VIEW".equalsIgnoreCase(t.getTaskType())).count();
        int videoLikes = (int) completedTasks.stream().filter(t -> "LIKE".equalsIgnoreCase(t.getTaskType())).count();
        int videoComments = (int) completedTasks.stream().filter(t -> "COMMENT".equalsIgnoreCase(t.getTaskType())).count();
        int shortViews = (int) completedTasks.stream().filter(t -> "SHORT_VIEW".equalsIgnoreCase(t.getTaskType())).count();
        int shortLikes = (int) completedTasks.stream().filter(t -> "SHORT_LIKE".equalsIgnoreCase(t.getTaskType())).count();
        int shortComments = (int) completedTasks.stream().filter(t -> "SHORT_COMMENT".equalsIgnoreCase(t.getTaskType())).count();

        Double moneyEarnings = earningRepository.sumEarningsByViewerId(userId);
        if (moneyEarnings == null) moneyEarnings = 0.0;

        return Map.of(
            "userId", userId,
            "totalSubscriptions", totalSubscriptions,
            "videoViews", videoViews,
            "videoLikes", videoLikes,
            "videoComments", videoComments,
            "moneyEarnings", moneyEarnings,
            "shortViews", shortViews,
            "shortLikes", shortLikes,
            "shortComments", shortComments
        );
    }
    @GetMapping("/earnings")
    public Map<String, Object> getViewerEarnings(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Map.of("error", "Unauthorized");
        }
        String token = authHeader.substring(7);
        Long userId = jwtUtil.extractUserId(token);

        Double moneyEarnings = earningRepository.sumEarningsByViewerId(userId);
        if (moneyEarnings == null) moneyEarnings = 0.0;

        return Map.of(
            "userId", userId,
            "moneyEarnings", moneyEarnings
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
