package com.growearn.controller;

import com.growearn.entity.*;
import com.growearn.service.ViewerTaskFlowService;
import com.growearn.service.TaskService;
import com.growearn.repository.*;
import com.growearn.security.JwtUtil;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/admin/tasks")
@CrossOrigin(origins = {"http://localhost:5173", "http://192.168.55.104:5173"}, allowCredentials = "true")
public class AdminTaskController {

    private static final Logger logger = LoggerFactory.getLogger(AdminTaskController.class);

    private final ViewerTaskEntityRepository viewerTaskRepository;
    private final ViewerTaskFlowService viewerTaskService;
    private final TaskService taskService;
    private final TaskRepository taskRepository;
    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final EarningRepository earningRepository;
    private final CreatorStatsRepository creatorStatsRepository;
    private final JwtUtil jwtUtil;

    public AdminTaskController(ViewerTaskEntityRepository viewerTaskRepository, 
                               ViewerTaskFlowService viewerTaskService,
                               TaskService taskService,
                               TaskRepository taskRepository,
                               CampaignRepository campaignRepository,
                               UserRepository userRepository,
                               WalletRepository walletRepository,
                               EarningRepository earningRepository,
                               CreatorStatsRepository creatorStatsRepository,
                               JwtUtil jwtUtil) {
        this.viewerTaskRepository = viewerTaskRepository;
        this.viewerTaskService = viewerTaskService;
        this.taskService = taskService;
        this.taskRepository = taskRepository;
        this.campaignRepository = campaignRepository;
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.earningRepository = earningRepository;
        this.creatorStatsRepository = creatorStatsRepository;
        this.jwtUtil = jwtUtil;
    }

    /**
     * GET /api/admin/tasks/pending
     * Get all tasks under verification with viewer and task details
     */
    @GetMapping("/pending")
    public List<Map<String, Object>> getPendingSubmissions() {
        logger.info("Admin requested pending submissions");
        List<ViewerTaskEntity> pendingTasks = viewerTaskRepository.findByStatus("UNDER_VERIFICATION");
        return enrichTasksWithDetails(pendingTasks);
    }

    /**
     * GET /api/admin/tasks/completed
     * Get all approved/completed tasks with viewer and task details
     */
    @GetMapping("/completed")
    public List<Map<String, Object>> getCompletedTasks() {
        logger.info("Admin requested completed tasks");
        List<ViewerTaskEntity> completedTasks = viewerTaskRepository.findByStatus("COMPLETED");
        return enrichTasksWithDetails(completedTasks);
    }

    /**
     * GET /api/admin/tasks/all
     * Get all tasks with their status
     */
    @GetMapping("/all")
    public List<Map<String, Object>> getAllTasks() {
        logger.info("Admin requested all tasks");
        List<ViewerTaskEntity> allTasks = viewerTaskRepository.findAll();
        return enrichTasksWithDetails(allTasks);
    }

    /**
     * GET /api/admin/tasks/stats
     * Get admin dashboard statistics
     */
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        long pendingCount = viewerTaskRepository.findByStatus("UNDER_VERIFICATION").size();
        long completedCount = viewerTaskRepository.findByStatus("COMPLETED").size();
        long totalTasks = taskRepository.count();
        long openTasks = taskRepository.countByStatus("OPEN");
        long totalViewers = userRepository.findByRole(Role.USER).size() + 
                           userRepository.findByRole(Role.VIEWER).size();
        long totalCreators = userRepository.findByRole(Role.CREATOR).size();
        
        return Map.of(
            "pendingVerification", pendingCount,
            "completedTasks", completedCount,
            "totalTasks", totalTasks,
            "openTasks", openTasks,
            "totalViewers", totalViewers,
            "totalCreators", totalCreators
        );
    }

    /**
     * POST /api/admin/tasks/approve
     * Approve a task and credit earnings to viewer
     * Accepts: { "taskId": 123 } or { "viewerTaskId": 123 }
     */
    @PostMapping("/approve")
    public Map<String, Object> approveTask(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        Long adminId = extractAdminId(req);
        
        Long viewerTaskId = extractId(body, "viewerTaskId", "taskId", "id");
        if (viewerTaskId == null) {
            return Map.of("success", false, "error", "missing_taskId", "message", "Task ID is required");
        }

        logger.info("Admin {} approving viewerTaskId={}", adminId, viewerTaskId);

        ViewerTaskEntity vt = viewerTaskRepository.findById(viewerTaskId).orElse(null);
        if (vt == null) {
            return Map.of("success", false, "error", "not_found", "message", "Task not found");
        }

        if (!"UNDER_VERIFICATION".equalsIgnoreCase(vt.getStatus())) {
            return Map.of("success", false, "error", "invalid_status", 
                         "message", "Task is not under verification. Current status: " + vt.getStatus());
        }

        TaskEntity task = taskRepository.findById(vt.getTaskId()).orElse(null);
        if (task == null) {
            return Map.of("success", false, "error", "task_not_found", "message", "Associated task not found");
        }
        double reward = task.getEarning() != null ? task.getEarning() : 0.0;

        vt.setStatus("COMPLETED");
        vt.setCompletedAt(LocalDateTime.now());
        vt.setApprovedBy(adminId);
        viewerTaskRepository.save(vt);

        WalletEntity wallet = walletRepository.findByViewerId(vt.getViewerId()).orElseGet(() -> {
            WalletEntity newWallet = new WalletEntity();
            newWallet.setViewerId(vt.getViewerId());
            newWallet.setBalance(0.0);
            return walletRepository.save(newWallet);
        });
        wallet.setBalance(wallet.getBalance() + reward);
        walletRepository.save(wallet);

        Earning earning = new Earning();
        earning.setViewerId(vt.getViewerId());
        earning.setAmount(reward);
        earning.setDescription("Task approved: task=" + task.getId() + ", type=" + task.getTaskType());
        earning.setCreatedAt(LocalDateTime.now());
        earningRepository.save(earning);

        if (task.getCampaignId() != null) {
            updateCampaignStats(task);
        }

        logger.info("Task {} approved. Credited {} to viewer {}", viewerTaskId, reward, vt.getViewerId());

        return Map.of(
            "success", true,
            "message", "Task approved successfully",
            "earning", reward,
            "viewerId", vt.getViewerId(),
            "taskId", viewerTaskId
        );
    }

    /**
     * POST /api/admin/tasks/reject
     * Reject a task with a reason
     */
    @PostMapping("/reject")
    public Map<String, Object> rejectTask(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        Long adminId = extractAdminId(req);
        
        Long viewerTaskId = extractId(body, "viewerTaskId", "taskId", "id");
        if (viewerTaskId == null) {
            return Map.of("success", false, "error", "missing_taskId", "message", "Task ID is required");
        }

        String reason = body.containsKey("reason") ? String.valueOf(body.get("reason")) : "No reason provided";

        logger.info("Admin {} rejecting viewerTaskId={} with reason: {}", adminId, viewerTaskId, reason);

        ViewerTaskEntity vt = viewerTaskRepository.findById(viewerTaskId).orElse(null);
        if (vt == null) {
            return Map.of("success", false, "error", "not_found", "message", "Task not found");
        }

        if (!"UNDER_VERIFICATION".equalsIgnoreCase(vt.getStatus())) {
            return Map.of("success", false, "error", "invalid_status", "message", "Task is not under verification");
        }

        vt.setStatus("REJECTED");
        vt.setRejectionReason(reason);
        vt.setApprovedBy(adminId);
        viewerTaskRepository.save(vt);

        logger.info("Task {} rejected", viewerTaskId);

        return Map.of("success", true, "message", "Task rejected", "taskId", viewerTaskId, "reason", reason);
    }

    /**
     * POST /api/admin/tasks/generate
     * Generate tasks for all ACTIVE campaigns that don't have tasks yet
     */
    @PostMapping("/generate")
    public Map<String, Object> generateTasksForExistingCampaigns() {
        logger.info("Admin requested task generation for existing campaigns");
        List<Campaign> activeCampaigns = campaignRepository.findByStatus("ACTIVE");
        int generatedCount = 0;
        for (Campaign c : activeCampaigns) {
            if (!taskService.hasTasksForCampaign(c.getId())) {
                taskService.createTasksForCampaign(c);
                generatedCount++;
            }
        }
        return Map.of(
            "message", "Tasks generated successfully",
            "campaignsProcessed", generatedCount,
            "totalActiveCampaigns", activeCampaigns.size(),
            "openTasksCount", taskService.countOpenTasks()
        );
    }

    // ========== Helper Methods ==========

    private List<Map<String, Object>> enrichTasksWithDetails(List<ViewerTaskEntity> viewerTasks) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (ViewerTaskEntity vt : viewerTasks) {
            Map<String, Object> taskData = new HashMap<>();
            taskData.put("id", vt.getId());
            taskData.put("viewerTaskId", vt.getId());
            taskData.put("taskId", vt.getTaskId());
            taskData.put("viewerId", vt.getViewerId());
            taskData.put("status", vt.getStatus());
            taskData.put("proof", vt.getProof());
            taskData.put("assignedAt", vt.getAssignedAt());
            taskData.put("submittedAt", vt.getSubmittedAt());
            taskData.put("completedAt", vt.getCompletedAt());
            taskData.put("approvedBy", vt.getApprovedBy());
            taskData.put("rejectionReason", vt.getRejectionReason());
            
            TaskEntity task = taskRepository.findById(vt.getTaskId()).orElse(null);
            if (task != null) {
                taskData.put("taskType", task.getTaskType());
                taskData.put("targetLink", task.getTargetLink());
                taskData.put("earning", task.getEarning());
                taskData.put("campaignId", task.getCampaignId());
                
                if (task.getCampaignId() != null) {
                    Campaign campaign = campaignRepository.findById(task.getCampaignId()).orElse(null);
                    if (campaign != null) {
                        taskData.put("platform", campaign.getPlatform());
                        taskData.put("channelName", campaign.getChannelName());
                        taskData.put("creatorId", campaign.getCreatorId());
                    }
                }
            }
            
            User viewer = userRepository.findById(vt.getViewerId()).orElse(null);
            if (viewer != null) {
                taskData.put("viewerEmail", viewer.getEmail());
                taskData.put("viewerName", viewer.getEmail());
            }
            
            result.add(taskData);
        }
        
        return result;
    }

    private Long extractAdminId(HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            try {
                return jwtUtil.extractUserId(auth.substring(7));
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private Long extractId(Map<String, Object> body, String... keys) {
        for (String key : keys) {
            if (body.containsKey(key)) {
                try {
                    return Long.valueOf(String.valueOf(body.get(key)));
                } catch (Exception e) { }
            }
        }
        return null;
    }

    private void updateCampaignStats(TaskEntity task) {
        Campaign campaign = campaignRepository.findById(task.getCampaignId()).orElse(null);
        if (campaign == null) return;

        switch (task.getTaskType()) {
            case "SUBSCRIBE" -> campaign.setCurrentSubscribers(campaign.getCurrentSubscribers() + 1);
            case "VIEW" -> campaign.setCurrentViews(campaign.getCurrentViews() + 1);
            case "LIKE" -> campaign.setCurrentLikes(campaign.getCurrentLikes() + 1);
            case "COMMENT" -> campaign.setCurrentComments(campaign.getCurrentComments() + 1);
        }
        double reward = task.getEarning() != null ? task.getEarning() : 0.0;
        campaign.setCurrentAmount(campaign.getCurrentAmount() + reward);
        campaignRepository.save(campaign);

        CreatorStats stats = creatorStatsRepository.findByCreatorId(campaign.getCreatorId())
                .orElseGet(() -> new CreatorStats(campaign.getCreatorId()));
        String contentType = campaign.getContentType() != null ? campaign.getContentType() : "VIDEO";
        
        switch (task.getTaskType()) {
            case "SUBSCRIBE" -> {
                stats.setTotalFollowers(stats.getTotalFollowers() + 1);
                stats.setSubscribers(stats.getSubscribers() + 1);
            }
            case "VIEW" -> {
                stats.setTotalViews(stats.getTotalViews() + 1);
                if ("SHORT".equalsIgnoreCase(contentType)) stats.setShortViews(stats.getShortViews() + 1);
                else stats.setVideoViews(stats.getVideoViews() + 1);
            }
            case "LIKE" -> {
                stats.setTotalLikes(stats.getTotalLikes() + 1);
                if ("SHORT".equalsIgnoreCase(contentType)) stats.setShortLikes(stats.getShortLikes() + 1);
                else stats.setVideoLikes(stats.getVideoLikes() + 1);
            }
            case "COMMENT" -> {
                stats.setTotalComments(stats.getTotalComments() + 1);
                if ("SHORT".equalsIgnoreCase(contentType)) stats.setShortComments(stats.getShortComments() + 1);
                else stats.setVideoComments(stats.getVideoComments() + 1);
            }
        }
        creatorStatsRepository.save(stats);
    }
}
