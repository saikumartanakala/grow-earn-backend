
package com.growearn.controller;
import jakarta.servlet.http.HttpServletRequest;

import com.growearn.entity.ViewerTask;
import com.growearn.repository.ViewerTaskRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import com.growearn.security.JwtUtil;

@RestController
@RequestMapping("/api/viewer/tasks")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class ViewerTaskController {
    private final JwtUtil jwtUtil;

    private final ViewerTaskRepository viewerTaskRepository;

    public ViewerTaskController(ViewerTaskRepository viewerTaskRepository, JwtUtil jwtUtil) {
        this.viewerTaskRepository = viewerTaskRepository;
        this.jwtUtil = jwtUtil;
    }

    // ✅ PAGINATED TASKS ENDPOINT
    @GetMapping
    public Map<String, Object> getTasks(
        HttpServletRequest request,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        String token = request.getHeader("Authorization").substring(7);
        Long userId = jwtUtil.extractUserId(token);
        Page<Object[]> rows = viewerTaskRepository.findTasksWithCampaignLinkByViewerIdPaged(userId, PageRequest.of(page, size));
        List<Map<String, Object>> tasks = rows.getContent().stream().map(row -> Map.of(
            "id", row[0],
            "campaign_id", row[1],
            "viewer_id", row[2],
            "creator_id", row[3],
            "task_type", row[4],
            "completed", row[5],
            "status", row[6],
            "target_link", row[7] != null ? row[7] : ""
        )).toList();
        return Map.of(
            "tasks", tasks,
            "totalPages", rows.getTotalPages(),
            "totalElements", rows.getTotalElements(),
            "currentPage", rows.getNumber()
        );
    }
}
