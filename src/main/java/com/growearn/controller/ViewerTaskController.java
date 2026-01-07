package com.growearn.controller;

import com.growearn.entity.ViewerTask;
import com.growearn.repository.ViewerTaskRepository;
import com.growearn.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/viewer/tasks")
@CrossOrigin(origins = "http://localhost:5173")
public class ViewerTaskController {

    private final ViewerTaskRepository repo;
    private final JwtUtil jwtUtil;

    public ViewerTaskController(ViewerTaskRepository repo, JwtUtil jwtUtil) {
        this.repo = repo;
        this.jwtUtil = jwtUtil;
    }

    // 🔹 FETCH AVAILABLE TASKS (GLOBAL)
    @GetMapping
    public List<ViewerTask> getAvailableTasks() {
        return repo.findByViewerIdIsNullAndCompletedFalse();
    }

    // 🔹 COMPLETE TASK
    @PostMapping("/{taskId}/complete")
    public void completeTask(
            @PathVariable Long taskId,
            HttpServletRequest request
    ) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        Long viewerId = jwtUtil.extractUserId(token);

        ViewerTask task = repo.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        task.setViewerId(viewerId);
        task.setCompleted(true);

        repo.save(task);
    }
}
