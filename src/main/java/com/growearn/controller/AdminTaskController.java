package com.growearn.controller;

import com.growearn.entity.*;
import com.growearn.service.ViewerTaskFlowService;
import com.growearn.service.TaskService;
import com.growearn.service.UserViolationService;
import com.growearn.service.ViewerWalletService;
import com.growearn.repository.*;
import com.growearn.security.JwtUtil;
import com.growearn.dto.*;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/tasks")
public class AdminTaskController {

    private static final Logger logger = LoggerFactory.getLogger(AdminTaskController.class);

    private final ViewerTaskEntityRepository viewerTaskRepository;
    private final ViewerTaskFlowService viewerTaskService;
    private final TaskService taskService;
    private final TaskRepository taskRepository;
    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;
    private final EarningRepository earningRepository;
    private final CreatorStatsRepository creatorStatsRepository;
    private final UserViolationService userViolationService;
    private final ViewerWalletService viewerWalletService;
    private final JwtUtil jwtUtil;

    public AdminTaskController(ViewerTaskEntityRepository viewerTaskRepository, 
                               ViewerTaskFlowService viewerTaskService,
                               TaskService taskService,
                               TaskRepository taskRepository,
                               CampaignRepository campaignRepository,
                               UserRepository userRepository,
                               EarningRepository earningRepository,
                               CreatorStatsRepository creatorStatsRepository,
                               UserViolationService userViolationService,
                               ViewerWalletService viewerWalletService,
                               JwtUtil jwtUtil) {
        this.viewerTaskRepository = viewerTaskRepository;
        this.viewerTaskService = viewerTaskService;
        this.taskService = taskService;
        this.taskRepository = taskRepository;
        this.campaignRepository = campaignRepository;
        this.userRepository = userRepository;
        this.earningRepository = earningRepository;
        this.creatorStatsRepository = creatorStatsRepository;
        this.userViolationService = userViolationService;
        this.viewerWalletService = viewerWalletService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * GET /api/admin/tasks?status={status}
     * Get tasks filtered by status (pending, on-hold, paid, rejected)
     * Status mapping: pending -> UNDER_VERIFICATION, on-hold -> HOLD
     */
    @GetMapping
    public List<AdminTaskDTO> getTasksByStatus(@RequestParam(required = false) String status) {
        logger.info("Admin requested tasks with status: {}", status);
        
        List<ViewerTaskEntity> tasks;
        if (status != null && !status.isEmpty()) {
            // Map frontend status to backend status
            String backendStatus = mapFrontendStatus(status);
            logger.info("Mapped frontend status '{}' to backend status '{}'", status, backendStatus);
            // Use repository method with built-in ordering
            tasks = viewerTaskRepository.findByStatusOrderBySubmittedAtDesc(backendStatus);
            logger.info("Found {} tasks with status '{}'", tasks.size(), backendStatus);
        } else {
            tasks = viewerTaskRepository.findAll();
            logger.info("Found {} total tasks", tasks.size());
            // Sort manually when fetching all
            tasks.sort((t1, t2) -> {
                if (t1.getSubmittedAt() == null && t2.getSubmittedAt() == null) return 0;
                if (t1.getSubmittedAt() == null) return 1;
                if (t2.getSubmittedAt() == null) return -1;
                return t2.getSubmittedAt().compareTo(t1.getSubmittedAt());
            });
        }
        
        List<AdminTaskDTO> result = tasks.stream()
                .map(this::convertToAdminTaskDTO)
                .collect(Collectors.toList());
        
        logger.info("Returning {} task DTOs to frontend", result.size());
        return result;
    }
    
    /**
     * GET /api/admin/tasks/pending (legacy support)
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
        List<ViewerTaskEntity> completedTasks = new ArrayList<>();
        completedTasks.addAll(viewerTaskRepository.findByStatus("PAID"));
        completedTasks.addAll(viewerTaskRepository.findByStatus("COMPLETED"));
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
        long completedCount = viewerTaskRepository.findByStatus("PAID").size()
            + viewerTaskRepository.findByStatus("COMPLETED").size();
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
     * Approve a task and move to HOLD status (no payment yet)
     * Accepts: { "taskId": "123" }
     */
    @PostMapping("/approve")
    public Map<String, Object> approveTask(@RequestBody TaskApproveRequestDTO request, HttpServletRequest req) {
        Long adminId = extractAdminId(req);
        
        if (request.getTaskId() == null || request.getTaskId().isEmpty()) {
            return Map.of("success", false, "error", "missing_taskId", "message", "Task ID is required");
        }
        
        Long viewerTaskId;
        try {
            viewerTaskId = Long.parseLong(request.getTaskId());
        } catch (NumberFormatException e) {
            return Map.of("success", false, "error", "invalid_taskId", "message", "Task ID must be a number");
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

        LocalDateTime now = LocalDateTime.now();

        vt.setStatus("ON_HOLD");
        vt.setApprovedAt(now);
        vt.setApprovedBy(adminId);
        vt.setHoldStartTime(now);
        vt.setHoldEndTime(now.plusDays(7));
        viewerTaskRepository.save(vt);

        logger.info("Task {} approved and moved to ON_HOLD until {}", viewerTaskId, vt.getHoldEndTime());

        return Map.of(
            "success", true,
            "message", "Task approved, now on hold",
            "holdEndTime", vt.getHoldEndTime().toString()
        );
    }
    
    /**
     * POST /api/admin/tasks/approve (legacy support)
     * Old approve method that directly credits wallet
     */
    @PostMapping("/approve-legacy")
    public Map<String, Object> approveTaskLegacy(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        Long adminId = extractAdminId(req);
        
        Long viewerTaskId = extractId(body, "viewerTaskId", "taskId", "id");
        if (viewerTaskId == null) {
            return Map.of("success", false, "error", "missing_taskId", "message", "Task ID is required");
        }

        logger.info("Admin {} approving viewerTaskId={} (legacy)", adminId, viewerTaskId);

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

        LocalDateTime now = LocalDateTime.now();

        vt.setStatus("ON_HOLD");
        vt.setApprovedAt(now);
        vt.setApprovedBy(adminId);
        vt.setHoldStartTime(now);
        vt.setHoldEndTime(now.plusDays(7));
        viewerTaskRepository.save(vt);

        logger.info("Task {} approved (legacy) and moved to ON_HOLD until {}", viewerTaskId, vt.getHoldEndTime());

        return Map.of(
            "success", true,
            "message", "Task approved and moved to hold",
            "holdEndTime", vt.getHoldEndTime().toString()
        );
    }

    /**
     * POST /api/admin/tasks/reject
     * Reject a task with a reason and add strike to viewer
     */
    @PostMapping("/reject")
    public Map<String, Object> rejectTask(@RequestBody TaskRejectRequestDTO request, HttpServletRequest req) {
        Long adminId = extractAdminId(req);
        
        if (request.getTaskId() == null || request.getTaskId().isEmpty()) {
            return Map.of("success", false, "error", "missing_taskId", "message", "Task ID is required");
        }
        
        if (request.getReason() == null || request.getReason().isEmpty()) {
            return Map.of("success", false, "error", "missing_reason", "message", "Rejection reason is required");
        }
        
        Long viewerTaskId;
        try {
            viewerTaskId = Long.parseLong(request.getTaskId());
        } catch (NumberFormatException e) {
            return Map.of("success", false, "error", "invalid_taskId", "message", "Task ID must be a number");
        }

        logger.info("Admin {} rejecting viewerTaskId={} with reason: {}", adminId, viewerTaskId, request.getReason());

        ViewerTaskEntity vt = viewerTaskRepository.findById(viewerTaskId).orElse(null);
        if (vt == null) {
            return Map.of("success", false, "error", "not_found", "message", "Task not found");
        }

        if (!"UNDER_VERIFICATION".equalsIgnoreCase(vt.getStatus())) {
            return Map.of("success", false, "error", "invalid_status", "message", "Task is not under verification");
        }

        vt.setStatus("REJECTED");
        vt.setRejectionReason(request.getReason());
        vt.setApprovedBy(adminId);
        viewerTaskRepository.save(vt);

        TaskEntity task = taskRepository.findById(vt.getTaskId()).orElse(null);
        if (task != null) {
            viewerWalletService.removeLockedEarnings(vt.getViewerId(), toAmount(task));
        }
        
        // Add strike to viewer for fraudulent submission
        try {
            userViolationService.strikeUser(vt.getViewerId(), "Task Rejection", 
                "Task rejected: " + request.getReason(), adminId);
        } catch (Exception e) {
            logger.error("Error adding strike to viewer {}: {}", vt.getViewerId(), e.getMessage());
        }

        logger.info("Task {} rejected and strike added to viewer {}", viewerTaskId, vt.getViewerId());

        return Map.of("success", true, "message", "Task rejected", "taskId", viewerTaskId);
    }

    /**
     * POST /api/admin/tasks/mark-paid
     * Mark task as paid and credit viewer wallet
     * This is the ONLY place where wallet updates happen
     */
    @PostMapping("/mark-paid")
    public Map<String, Object> markTaskPaid(@RequestBody TaskMarkPaidRequestDTO request, HttpServletRequest req) {
        Long adminId = extractAdminId(req);
        
        if (request.getTaskId() == null || request.getTaskId().isEmpty()) {
            return Map.of("success", false, "error", "missing_taskId", "message", "Task ID is required");
        }
        
        Long viewerTaskId;
        try {
            viewerTaskId = Long.parseLong(request.getTaskId());
        } catch (NumberFormatException e) {
            return Map.of("success", false, "error", "invalid_taskId", "message", "Task ID must be a number");
        }

        logger.info("Admin {} marking viewerTaskId={} as paid", adminId, viewerTaskId);

        ViewerTaskEntity vt = viewerTaskRepository.findById(viewerTaskId).orElse(null);
        if (vt == null) {
            return Map.of("success", false, "error", "not_found", "message", "Task not found");
        }

        if (!"HOLD".equalsIgnoreCase(vt.getStatus())) {
            return Map.of("success", false, "error", "invalid_status", 
                         "message", "Task is not on hold. Current status: " + vt.getStatus());
        }

        TaskEntity task = taskRepository.findById(vt.getTaskId()).orElse(null);
        if (task == null) {
            return Map.of("success", false, "error", "task_not_found", "message", "Associated task not found");
        }
        
        double reward = task.getEarning() != null ? task.getEarning() : 0.0;

        // Update task status to PAID
        vt.setStatus("PAID");
        vt.setPaidAt(LocalDateTime.now());
        vt.setPaymentTxnId(generateTxnId());
        viewerTaskRepository.save(vt);

        task.setStatus("COMPLETED");
        taskRepository.save(task);

        viewerWalletService.releaseToBalance(vt.getViewerId(), BigDecimal.valueOf(reward));

        // Record earning
        Earning earning = new Earning();
        earning.setViewerId(vt.getViewerId());
        earning.setAmount(reward);
        earning.setDescription("Task paid: " + task.getTaskType() + " - " + vt.getPaymentTxnId());
        earning.setCreatedAt(LocalDateTime.now());
        earningRepository.save(earning);

        // Update campaign and creator stats
        if (task.getCampaignId() != null) {
            updateCampaignStats(task);
        }

        logger.info("Task {} marked as PAID. Credited {} to viewer {}", viewerTaskId, reward, vt.getViewerId());

        return Map.of(
            "success", true,
            "message", "Task marked as paid, wallet updated",
            "earning", reward,
            "txnId", vt.getPaymentTxnId()
        );
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
    
    /**
     * Map frontend status to backend status
     */
    private String mapFrontendStatus(String frontendStatus) {
        return switch (frontendStatus.toLowerCase()) {
            case "pending", "pending_verification" -> "UNDER_VERIFICATION";
            case "on-hold", "on_hold", "onhold", "hold" -> "ON_HOLD";
            case "paid" -> "PAID";
            case "rejected" -> "REJECTED";
            default -> frontendStatus.toUpperCase();
        };
    }
    
    /**
     * Convert ViewerTaskEntity to AdminTaskDTO
     */
    private AdminTaskDTO convertToAdminTaskDTO(ViewerTaskEntity vt) {
        AdminTaskDTO dto = new AdminTaskDTO();
        dto.setId(vt.getId());
        dto.setProofUrl(vt.getProofUrl());
        dto.setPublicId(vt.getProofPublicId());
        dto.setProofText(vt.getProofText());
        dto.setRiskScore(vt.getRiskScore() != null ? vt.getRiskScore() : 0.0);
        dto.setStatus(vt.getStatus());
        dto.setSubmittedAt(vt.getSubmittedAt());
        
        // DEBUG: Log proof data
        logger.info("Converting task {}: proofUrl={}, publicId={}, proofText={}", 
                   vt.getId(), vt.getProofUrl(), vt.getProofPublicId(), vt.getProofText());
        
        // Get task details
        TaskEntity task = taskRepository.findById(vt.getTaskId()).orElse(null);
        if (task != null) {
            dto.setTitle(task.getTaskType());
            dto.setTaskType(task.getTaskType());
            dto.setTargetLink(task.getTargetLink());
            dto.setEarning(task.getEarning());
            
            if (task.getCampaignId() != null) {
                dto.setCampaignId(task.getCampaignId().toString());
            }
        }
        
        // Get viewer details
        User viewer = userRepository.findById(vt.getViewerId()).orElse(null);
        if (viewer != null) {
            AdminTaskDTO.ViewerInfoDTO viewerInfo = new AdminTaskDTO.ViewerInfoDTO();
            viewerInfo.setId(viewer.getId());
            // Use fullName if available, otherwise use email
            String displayName = (viewer.getFullName() != null && !viewer.getFullName().isEmpty()) 
                ? viewer.getFullName() 
                : viewer.getEmail();
            viewerInfo.setName(displayName);
            viewerInfo.setEmail(viewer.getEmail());
            dto.setViewer(viewerInfo);
        }
        
        return dto;
    }
    
    /**
     * Generate unique transaction ID
     */
    private String generateTxnId() {
        return "TXN-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 10000);
    }

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

    private BigDecimal toAmount(TaskEntity task) {
        double reward = task != null && task.getEarning() != null ? task.getEarning() : 0.0;
        return BigDecimal.valueOf(reward);
    }
}
