package com.growearn.controller;

import com.growearn.dto.TaskSubmissionRequest;
import com.growearn.service.ViewerTaskFlowService;
import com.growearn.service.TaskTimerService;
import com.growearn.entity.ViewerTaskEntity;
import com.growearn.entity.Earning;
import com.growearn.security.JwtUtil;
import com.growearn.repository.TaskRepository;
import com.growearn.repository.ViewerTaskEntityRepository;
import com.growearn.repository.EarningRepository;
import com.growearn.service.CloudinaryUploadValidator;
import com.growearn.service.ViewerWalletService;
import com.growearn.entity.TaskEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.List;
import java.util.HashMap;

@RestController
@RequestMapping("/api/viewer/tasks")
public class ViewerTaskController {

    private static final Logger logger = LoggerFactory.getLogger(ViewerTaskController.class);

    private final ViewerTaskFlowService viewerTaskFlowService;
    private final TaskTimerService taskTimerService;
    private final JwtUtil jwtUtil;
    private final TaskRepository taskRepository;
    private final ViewerTaskEntityRepository viewerTaskEntityRepository;
    private final EarningRepository earningRepository;
    private final ViewerWalletService viewerWalletService;
    private final com.growearn.repository.CampaignRepository campaignRepository;
    private final CloudinaryUploadValidator cloudinaryUploadValidator;

    public ViewerTaskController(ViewerTaskFlowService viewerTaskFlowService, JwtUtil jwtUtil,
                                TaskRepository taskRepository, ViewerTaskEntityRepository viewerTaskEntityRepository,
                                EarningRepository earningRepository, ViewerWalletService viewerWalletService,
                                com.growearn.repository.CampaignRepository campaignRepository,
                                TaskTimerService taskTimerService,
                                CloudinaryUploadValidator cloudinaryUploadValidator) {
        this.viewerTaskFlowService = viewerTaskFlowService;
        this.taskTimerService = taskTimerService;
        this.jwtUtil = jwtUtil;
        this.taskRepository = taskRepository;
        this.viewerTaskEntityRepository = viewerTaskEntityRepository;
        this.earningRepository = earningRepository;
        this.viewerWalletService = viewerWalletService;
        this.campaignRepository = campaignRepository;
        this.cloudinaryUploadValidator = cloudinaryUploadValidator;
    }

    /**
     * STAGE 1: Submit task with proof (NEW VERIFICATION PIPELINE)
     * POST /api/viewer/tasks/submit-with-proof
     */
    @PostMapping("/submit-with-proof")
    public Map<String, Object> submitTaskWithProof(@RequestBody TaskSubmissionRequest request, 
                                                     HttpServletRequest httpRequest) {
        try {
            // Extract viewer ID from token
            String auth = httpRequest.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Map.of("success", false, "message", "Missing or invalid authorization token");
            }
            Long viewerId = jwtUtil.extractUserId(auth.substring(7));

            // Extract IP address
            String ipAddress = extractIpAddress(httpRequest);

            // Validate request
            if (request.getTaskId() == null) {
                return Map.of("success", false, "message", "Task ID is required");
            }
            if (request.getProofUrl() == null || request.getProofUrl().trim().isEmpty()) {
                return Map.of("success", false, "message", "Proof URL is required");
            }
            try {
                cloudinaryUploadValidator.validate(request.getProofMimeType(), request.getProofSizeBytes());
            } catch (IllegalArgumentException ex) {
                return Map.of("success", false, "message", ex.getMessage());
            }

            logger.info("Task submission received from viewer {}: taskId={}, proofUrl={}", 
                       viewerId, request.getTaskId(), request.getProofUrl());

            // Submit task with proof and risk analysis
            Map<String, Object> result = viewerTaskFlowService.submitTaskWithProof(
                request.getTaskId(),
                viewerId,
                request.getProofUrl(),
                request.getProofPublicId(),
                request.getProofText(),
                request.getDeviceFingerprint(),
                ipAddress
            );

            return result;

        } catch (Exception e) {
            logger.error("Error submitting task with proof", e);
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    /**
     * Start timer for a task (ADD ONLY)
     * POST /api/viewer/tasks/startTimer
     * Body: { "taskId": 123 }
     */
    @PostMapping("/startTimer")
    public Map<String, Object> startTimer(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED, "Missing authentication token");
        }
        Long viewerId = jwtUtil.extractUserId(auth.substring(7));
        Long taskId = null;
        if (body.containsKey("taskId")) {
            taskId = Long.valueOf(String.valueOf(body.get("taskId")));
        } else if (body.containsKey("id")) {
            taskId = Long.valueOf(String.valueOf(body.get("id")));
        }
        if (taskId == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "Task ID is required");
        }
        Map<String, Object> result = taskTimerService.startTimer(taskId, viewerId);
        return Map.of(
            "unlockTime", result.get("unlockTime"),
            "requiredWatchSeconds", result.get("requiredWatchSeconds")
        );
    }

    /**
     * Extract IP address from request
     */
    private String extractIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("X-Real-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        // If multiple IPs in X-Forwarded-For, take the first one
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }
        return ipAddress;
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
    public Map<String, Object> submitTask(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        try {
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
            
            String auth = req.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Map.of("success", false, "message", "Missing token");
            }
            Long viewerId = jwtUtil.extractUserId(auth.substring(7));
            
            // Check if proof data is provided (new verification pipeline)
            if ((body.containsKey("proofUrl") && body.get("proofUrl") != null) ||
                (body.containsKey("proof") && body.get("proof") != null)) {
                String proofUrl = body.containsKey("proofUrl") && body.get("proofUrl") != null
                    ? String.valueOf(body.get("proofUrl"))
                    : String.valueOf(body.get("proof"));
                String proofPublicId = body.containsKey("publicId") ? String.valueOf(body.get("publicId")) : null;
                String proofText = body.containsKey("proofText") ? String.valueOf(body.get("proofText")) : null;
                String deviceFingerprint = req.getHeader("X-Device-Fingerprint");
                String ipAddress = extractIpAddress(req);
                String proofMimeType = body.containsKey("proofMimeType") ? String.valueOf(body.get("proofMimeType")) : null;
                Long proofSizeBytes = null;
                if (body.containsKey("proofSizeBytes") && body.get("proofSizeBytes") != null) {
                    proofSizeBytes = Long.valueOf(String.valueOf(body.get("proofSizeBytes")));
                }

                logger.info("Task submission with proof: taskId={}, viewerId={}, proofUrl={}", 
                           taskId, viewerId, proofUrl);

                try {
                    cloudinaryUploadValidator.validate(proofMimeType, proofSizeBytes);
                } catch (IllegalArgumentException ex) {
                    return Map.of("success", false, "message", ex.getMessage());
                }

                // Use new verification pipeline
                return viewerTaskFlowService.submitTaskWithProof(
                    taskId, viewerId, proofUrl, proofPublicId, proofText, deviceFingerprint, ipAddress
                );
            } else {
                // Legacy: Use completeTask which handles grab+submit in one transaction
                ViewerTaskEntity result = viewerTaskFlowService.completeTask(taskId, viewerId);
                return Map.of("success", true, "message", "Task completed", "taskId", result.getId());
            }
        } catch (Exception e) {
            logger.error("Error submitting task", e);
            return Map.of("success", false, "message", e.getMessage());
        }
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
            .findByViewerIdAndStatusOrderByCompletedAtDesc(viewerId, "PAID");
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
                double reward = task.getEarning() != null ? task.getEarning() : 0.0;
                taskData.put("earning", reward);
                taskData.put("reward", reward);
                taskData.put("amount", reward);
                taskData.put("earnings", reward);
                taskData.put("prize", reward);
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
                taskData.put("reward", 0.0);
                taskData.put("amount", 0.0);
                taskData.put("earnings", 0.0);
                taskData.put("prize", 0.0);
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
        long completed = viewerTaskEntityRepository.countByViewerIdAndStatus(viewerId, "PAID");
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
        com.growearn.entity.ViewerWallet wallet = viewerWalletService.getOrCreateWallet(viewerId);
        return Map.of(
            "balance", wallet.getBalance().add(wallet.getLockedBalance()),
            "available", wallet.getBalance(),
            "locked", wallet.getLockedBalance()
        );
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
