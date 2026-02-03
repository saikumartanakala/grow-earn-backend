package com.growearn.controller;

import com.growearn.service.ViewerTaskFlowService;
import com.growearn.entity.ViewerTaskEntity;
import com.growearn.entity.Earning;
import com.growearn.security.JwtUtil;
import com.growearn.repository.TaskRepository;
import com.growearn.repository.ViewerTaskEntityRepository;
import com.growearn.repository.EarningRepository;
import com.growearn.repository.WalletRepository;
import com.growearn.entity.TaskEntity;
import com.growearn.entity.WalletEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.List;
import java.util.HashMap;

@RestController
@RequestMapping("/api/viewer/tasks")
@CrossOrigin(origins = {"http://localhost:5173", "http://192.168.55.104:5173"}, allowCredentials = "true")
public class ViewerTaskController {

    private final ViewerTaskFlowService viewerTaskFlowService;
    private final JwtUtil jwtUtil;
    private final TaskRepository taskRepository;
    private final ViewerTaskEntityRepository viewerTaskEntityRepository;
    private final EarningRepository earningRepository;
    private final WalletRepository walletRepository;
    private final com.growearn.repository.CampaignRepository campaignRepository;

    public ViewerTaskController(ViewerTaskFlowService viewerTaskFlowService, JwtUtil jwtUtil,
                                TaskRepository taskRepository, ViewerTaskEntityRepository viewerTaskEntityRepository,
                                EarningRepository earningRepository, WalletRepository walletRepository,
                                com.growearn.repository.CampaignRepository campaignRepository) {
        this.viewerTaskFlowService = viewerTaskFlowService;
        this.jwtUtil = jwtUtil;
        this.taskRepository = taskRepository;
        this.viewerTaskEntityRepository = viewerTaskEntityRepository;
        this.earningRepository = earningRepository;
        this.walletRepository = walletRepository;
        this.campaignRepository = campaignRepository;
    }

    /**
     * Get paginated list of active tasks for viewers.
     * Excludes tasks already grabbed by the current viewer.
     * GET /api/viewer/tasks?page=0&size=10
     */
    @GetMapping
    public Map<String, Object> getActiveTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest req) {
        
        // Get viewer ID from token
        String auth = req.getHeader("Authorization");
        Long viewerId = null;
        if (auth != null && auth.startsWith("Bearer ")) {
            viewerId = jwtUtil.extractUserId(auth.substring(7));
        }
        
        // Get active tasks for this viewer (excludes already grabbed tasks)
        List<TaskEntity> allOpenTasks;
        if (viewerId != null) {
            allOpenTasks = viewerTaskFlowService.getActiveTasksForViewer(viewerId);
        } else {
            allOpenTasks = taskRepository.findByStatus("OPEN");
        }
        
        // If no tasks in tasks table, return campaigns with ACTIVE status as tasks
        if (allOpenTasks.isEmpty()) {
            List<com.growearn.entity.Campaign> activeCampaigns = campaignRepository.findByStatus("ACTIVE");
            
            // Filter out campaigns that viewer already has tasks for
            if (viewerId != null) {
                List<com.growearn.entity.ViewerTaskEntity> viewerTasks = viewerTaskEntityRepository.findByViewerId(viewerId);
                List<Long> grabbedCampaignIds = new java.util.ArrayList<>();
                for (var vt : viewerTasks) {
                    // Get campaign ID from task
                    taskRepository.findById(vt.getTaskId()).ifPresent(t -> grabbedCampaignIds.add(t.getCampaignId()));
                }
                final List<Long> finalGrabbedCampaignIds = grabbedCampaignIds;
                activeCampaigns = activeCampaigns.stream()
                    .filter(c -> !finalGrabbedCampaignIds.contains(c.getId()))
                    .toList();
            }
            
            int totalElements = activeCampaigns.size();
            int totalPages = (int) Math.ceil((double) totalElements / size);
            int start = Math.min(page * size, totalElements);
            int end = Math.min(start + size, totalElements);
            List<com.growearn.entity.Campaign> pageContent = activeCampaigns.subList(start, end);
            
            // Convert campaigns to task-like objects
            List<Map<String, Object>> tasks = new java.util.ArrayList<>();
            for (com.growearn.entity.Campaign c : pageContent) {
                Map<String, Object> task = new HashMap<>();
                task.put("id", c.getId());
                task.put("campaignId", c.getId());
                task.put("taskType", c.getGoalType());
                task.put("targetLink", c.getVideoLink());
                task.put("channelName", c.getChannelName());
                task.put("channelLink", c.getChannelLink());
                task.put("platform", c.getPlatform());
                task.put("contentType", c.getContentType());
                task.put("videoDuration", c.getVideoDuration());
                task.put("earning", c.getTotalAmount() / Math.max(1, c.getSubscriberGoal() + c.getViewsGoal() + c.getLikesGoal() + c.getCommentsGoal()));
                task.put("status", "OPEN");
                task.put("createdAt", c.getUpdatedAt());
                tasks.add(task);
            }
            
            return Map.of(
                "content", tasks,
                "totalElements", totalElements,
                "totalPages", totalPages,
                "number", page,
                "size", size
            );
        }
        
        // Paginate the open tasks
        int totalElements = allOpenTasks.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int start = Math.min(page * size, totalElements);
        int end = Math.min(start + size, totalElements);
        List<TaskEntity> pageContent = allOpenTasks.subList(start, end);
        
        return Map.of(
            "content", pageContent,
            "totalElements", totalElements,
            "totalPages", totalPages,
            "number", page,
            "size", size
        );
    }

    @PostMapping("/grab")
    public ViewerTaskEntity grabTask(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        Long taskId = Long.valueOf(String.valueOf(body.get("taskId")));
        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) throw new RuntimeException("Missing token");
        Long viewerId = jwtUtil.extractUserId(auth.substring(7));
        return viewerTaskFlowService.grabTask(taskId, viewerId);
    }

    @PostMapping("/submit")
    public ViewerTaskEntity submitTask(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        // Accept either "taskId" or "id" from the request
        Long taskId = null;
        if (body.containsKey("taskId")) {
            taskId = Long.valueOf(String.valueOf(body.get("taskId")));
        } else if (body.containsKey("id")) {
            taskId = Long.valueOf(String.valueOf(body.get("id")));
        }
        if (taskId == null) throw new RuntimeException("Task ID is required");
        
        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) throw new RuntimeException("Missing token");
        Long viewerId = jwtUtil.extractUserId(auth.substring(7));
        // Use completeTask which handles grab+submit in one transaction
        return viewerTaskFlowService.completeTask(taskId, viewerId);
    }

    /**
     * Complete a task in one step: grabs and submits in a single call.
     * POST /api/viewer/tasks/complete
     * Body: { "taskId": 123 } or { "id": 123 }
     */
    @PostMapping("/complete")
    public Map<String, Object> completeTask(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return Map.of("success", false, "message", "Missing authentication token");
        }
        Long viewerId = jwtUtil.extractUserId(auth.substring(7));
        
        // Accept either "taskId" or "id" from the request
        Long taskId = null;
        if (body.containsKey("taskId")) {
            taskId = Long.valueOf(String.valueOf(body.get("taskId")));
        } else if (body.containsKey("id")) {
            taskId = Long.valueOf(String.valueOf(body.get("id")));
        }
        
        if (taskId == null) {
            return Map.of("success", false, "message", "Task ID is required");
        }
        
        try {
            // Get the task details first for the response
            TaskEntity task = taskRepository.findById(taskId).orElse(null);
            String targetLink = task != null ? task.getTargetLink() : null;
            String taskType = task != null ? task.getTaskType() : null;
            Double earning = task != null ? task.getEarning() : 0.0;
            
            // Use the single-transaction complete method
            ViewerTaskEntity viewerTask = viewerTaskFlowService.completeTask(taskId, viewerId);
            
            return Map.of(
                "success", true,
                "message", "Task submitted for verification",
                "targetLink", targetLink != null ? targetLink : "",
                "taskType", taskType != null ? taskType : "",
                "earning", earning,
                "viewerTaskId", viewerTask.getId()
            );
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    // Active tasks exposed by TaskController

    @GetMapping("/under-verification")
    public java.util.List<Map<String, Object>> getUnderVerification(HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) throw new RuntimeException("Missing token");
        Long viewerId = jwtUtil.extractUserId(auth.substring(7));
        // Use optimized query instead of fetching all and filtering
        java.util.List<ViewerTaskEntity> tasks = viewerTaskEntityRepository
            .findByViewerIdAndStatus(viewerId, "UNDER_VERIFICATION");
        return enrichViewerTasks(tasks);
    }

    @GetMapping("/completed")
    public java.util.List<Map<String, Object>> getCompleted(HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) throw new RuntimeException("Missing token");
        Long viewerId = jwtUtil.extractUserId(auth.substring(7));
        // Use optimized query - returns only completed tasks for this viewer, ordered by completion date
        java.util.List<ViewerTaskEntity> tasks = viewerTaskEntityRepository
            .findByViewerIdAndStatusOrderByCompletedAtDesc(viewerId, "COMPLETED");
        return enrichViewerTasks(tasks);
    }

    /**
     * Helper method to enrich ViewerTaskEntity with task details
     */
    private java.util.List<Map<String, Object>> enrichViewerTasks(java.util.List<ViewerTaskEntity> viewerTasks) {
        java.util.List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (ViewerTaskEntity vt : viewerTasks) {
            Map<String, Object> taskData = new HashMap<>();
            taskData.put("id", vt.getId());
            taskData.put("viewerTaskId", vt.getId());
            taskData.put("taskId", vt.getTaskId());
            taskData.put("viewerId", vt.getViewerId());
            taskData.put("status", vt.getStatus());
            taskData.put("proof", vt.getProof());
            taskData.put("submittedAt", vt.getSubmittedAt());
            taskData.put("completedAt", vt.getCompletedAt());
            taskData.put("assignedAt", vt.getAssignedAt());
            
            // Get task details
            TaskEntity task = taskRepository.findById(vt.getTaskId()).orElse(null);
            if (task != null) {
                taskData.put("taskType", task.getTaskType());
                taskData.put("targetLink", task.getTargetLink());
                taskData.put("earning", task.getEarning() != null ? task.getEarning() : 0.0);
                taskData.put("campaignId", task.getCampaignId());
                
                // Get campaign details for platform info
                if (task.getCampaignId() != null) {
                    campaignRepository.findById(task.getCampaignId()).ifPresent(campaign -> {
                        taskData.put("platform", campaign.getPlatform());
                        taskData.put("channelName", campaign.getChannelName());
                        taskData.put("channelLink", campaign.getChannelLink());
                        taskData.put("contentType", campaign.getContentType());
                    });
                }
            } else {
                taskData.put("taskType", "Task");
                taskData.put("earning", 0.0);
            }
            
            result.add(taskData);
        }
        return result;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) throw new RuntimeException("Missing token");
        Long viewerId = jwtUtil.extractUserId(auth.substring(7));
        long completed = viewerTaskEntityRepository.countByViewerIdAndStatus(viewerId, "COMPLETED");
        long underVerification = viewerTaskEntityRepository.countByViewerIdAndStatus(viewerId, "UNDER_VERIFICATION");
        Double totalEarnings = earningRepository.sumEarningsByViewerId(viewerId);
        // Count available tasks (excluding already grabbed by this viewer)
        long available = viewerTaskFlowService.getActiveTasksForViewer(viewerId).size();
        return Map.of(
                "completedTasksCount", completed,
                "underVerificationCount", underVerification,
                "totalEarnings", totalEarnings != null ? totalEarnings : 0.0,
                "availableTasksCount", available
        );
    }

    @GetMapping("/wallet")
    public Map<String, Object> wallet(HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) throw new RuntimeException("Missing token");
        Long viewerId = jwtUtil.extractUserId(auth.substring(7));
        WalletEntity w = walletRepository.findByViewerId(viewerId).orElseGet(() -> {
            WalletEntity ne = new WalletEntity(); ne.setViewerId(viewerId); ne.setBalance(0.0); return ne;
        });
        return Map.of("balance", w.getBalance());
    }

    @GetMapping("/earnings")
    public Map<String, Object> earnings(HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) throw new RuntimeException("Missing token");
        Long viewerId = jwtUtil.extractUserId(auth.substring(7));
        
        List<Earning> earningsList = earningRepository.findByViewerId(viewerId);
        Double totalEarnings = earningRepository.sumEarningsByViewerId(viewerId);
        if (totalEarnings == null) totalEarnings = 0.0;
        
        Map<String, Object> result = new HashMap<>();
        result.put("userId", viewerId);
        result.put("totalEarnings", totalEarnings);
        result.put("earnings", earningsList);
        return result;
    }

    @GetMapping("/active")
    public List<TaskEntity> getActiveTasks() {
        return viewerTaskFlowService.getActiveTasks();
    }
}
