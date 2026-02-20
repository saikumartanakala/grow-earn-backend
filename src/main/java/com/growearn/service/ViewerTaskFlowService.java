package com.growearn.service;

import com.growearn.entity.*;
import com.growearn.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import java.util.concurrent.ConcurrentHashMap;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
public class ViewerTaskFlowService {

    private static final Logger logger = LoggerFactory.getLogger(ViewerTaskFlowService.class);
    private static final int HOLD_PERIOD_DAYS = 7;

    private final TaskRepository taskRepository;
    private final ViewerTaskEntityRepository viewerTaskEntityRepository;
    private final EarningRepository earningRepository;
    private final CampaignRepository campaignRepository;
    private final CreatorStatsRepository creatorStatsRepository;
    private final RiskAnalysisService riskAnalysisService;
    private final UserViolationService userViolationService;
    private final PlatformTaskMapper platformTaskMapper;
    private final ViewerWalletService viewerWalletService;
    private static final ConcurrentHashMap<String, Object> SUBMIT_LOCKS = new ConcurrentHashMap<>();

    public ViewerTaskFlowService(TaskRepository taskRepository,
                                 ViewerTaskEntityRepository viewerTaskEntityRepository,
                                 EarningRepository earningRepository,
                                 CampaignRepository campaignRepository,
                                 CreatorStatsRepository creatorStatsRepository,
                                 RiskAnalysisService riskAnalysisService,
                                 UserViolationService userViolationService,
                                 PlatformTaskMapper platformTaskMapper,
                                 ViewerWalletService viewerWalletService) {
        this.taskRepository = taskRepository;
        this.viewerTaskEntityRepository = viewerTaskEntityRepository;
        this.earningRepository = earningRepository;
        this.campaignRepository = campaignRepository;
        this.creatorStatsRepository = creatorStatsRepository;
        this.riskAnalysisService = riskAnalysisService;
        this.userViolationService = userViolationService;
        this.platformTaskMapper = platformTaskMapper;
        this.viewerWalletService = viewerWalletService;
    }

    public List<TaskEntity> getActiveTasks() {
        return taskRepository.findByStatus("OPEN");
    }

    /**
     * Get active tasks for a specific viewer, excluding tasks they already grabbed.
     */
    public List<TaskEntity> getActiveTasksForViewer(Long viewerId) {
        List<TaskEntity> allOpenTasks = taskRepository.findByStatus("OPEN");
        // Get task IDs already grabbed by this viewer
        List<ViewerTaskEntity> viewerTasks = viewerTaskEntityRepository.findByViewerId(viewerId);
        List<Long> grabbedTaskIds = viewerTasks.stream().map(ViewerTaskEntity::getTaskId).toList();
        // Filter out grabbed tasks
        return allOpenTasks.stream()
                .filter(t -> !grabbedTaskIds.contains(t.getId()))
                .toList();
    }

    @Transactional
    public ViewerTaskEntity grabTask(Long taskId, Long viewerId) {
        TaskEntity t = taskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("Task not found"));
        if (!"OPEN".equalsIgnoreCase(t.getStatus())) throw new RuntimeException("Task not available");
        t.setStatus("ASSIGNED");
        taskRepository.save(t);

        ViewerTaskEntity v = new ViewerTaskEntity();
        v.setTaskId(taskId);
        v.setViewerId(viewerId);
        v.setTaskType(toViewerTaskType(t));
        v.setStatus("ASSIGNED");
        v.setAssignedAt(LocalDateTime.now());
        viewerTaskEntityRepository.save(v);
        return v;
    }

    @Transactional
    public ViewerTaskEntity submitTask(Long taskId, Long viewerId, String proof) {
        ViewerTaskEntity v = viewerTaskEntityRepository.findByTaskIdAndViewerId(taskId, viewerId)
                .orElseThrow(() -> new RuntimeException("ViewerTask not found or not assigned to viewer"));
        v.setStatus("UNDER_VERIFICATION");
        v.setProof(proof);
        viewerTaskEntityRepository.save(v);

        TaskEntity t = taskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("Task not found"));
        v.setTaskType(toViewerTaskType(t));
        t.setStatus("UNDER_VERIFICATION");
        taskRepository.save(t);

        addLockedForTask(viewerId, t);
        return v;
    }

    /**
     * Complete a task in one step: grab + submit in a single transaction.
     * This method handles checking if the task is already assigned to the viewer.
     */
    @Transactional
    public ViewerTaskEntity completeTask(Long taskId, Long viewerId) {
        // Check if viewer already has this task assigned
        var existingViewerTask = viewerTaskEntityRepository.findByTaskIdAndViewerId(taskId, viewerId);
        
        if (existingViewerTask.isPresent()) {
            ViewerTaskEntity v = existingViewerTask.get();
            if ("UNDER_VERIFICATION".equalsIgnoreCase(v.getStatus()) || 
                "COMPLETED".equalsIgnoreCase(v.getStatus())) {
                throw new RuntimeException("Task already submitted or completed");
            }
            // Submit the already assigned task
            v.setStatus("UNDER_VERIFICATION");
            v.setProof("Task completed by viewer at " + LocalDateTime.now());
            v.setTaskType(toViewerTaskType(taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"))));
            viewerTaskEntityRepository.save(v);
            
            TaskEntity t = taskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("Task not found"));
            t.setStatus("UNDER_VERIFICATION");
            taskRepository.save(t);
            addLockedForTask(viewerId, t);
            return v;
        }
        
        // Grab the task - but DON'T change the main task status
        // This allows multiple viewers to grab the same task
        TaskEntity t = taskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("Task not found"));
        if (!"OPEN".equalsIgnoreCase(t.getStatus())) {
            throw new RuntimeException("Task not available");
        }
        // DON'T change task status - keep it OPEN for other viewers
        // t.setStatus("UNDER_VERIFICATION"); 
        // taskRepository.save(t);

        // Create viewer task entry as UNDER_VERIFICATION for THIS viewer only
        ViewerTaskEntity v = new ViewerTaskEntity();
        v.setTaskId(taskId);
        v.setViewerId(viewerId);
        v.setTaskType(toViewerTaskType(t));
        v.setStatus("UNDER_VERIFICATION");
        v.setProof("Task completed by viewer at " + LocalDateTime.now());
        v.setAssignedAt(LocalDateTime.now());
        viewerTaskEntityRepository.save(v);

        addLockedForTask(viewerId, t);
        
        return v;
    }

    public List<ViewerTaskEntity> getPendingSubmissions() {
        return viewerTaskEntityRepository.findByStatus("UNDER_VERIFICATION");
    }

    @Transactional
    public java.util.Map<String,Object> approveSubmission(Long viewerTaskId) {
        ViewerTaskEntity v = viewerTaskEntityRepository.findById(viewerTaskId).orElseThrow(() -> new RuntimeException("ViewerTask not found"));
        if (!"UNDER_VERIFICATION".equalsIgnoreCase(v.getStatus())) return java.util.Map.of("error","not_under_verification");

        TaskEntity t = taskRepository.findById(v.getTaskId()).orElseThrow(() -> new RuntimeException("Task not found"));

        // 1) Update viewer_tasks -> HOLD
        LocalDateTime now = LocalDateTime.now();
        v.setStatus("ON_HOLD");
        v.setApprovedAt(now);
        v.setHoldStartTime(now);
        v.setHoldEndTime(now.plusDays(HOLD_PERIOD_DAYS));
        viewerTaskEntityRepository.save(v);

        // 5) Update campaigns counters (normalize task type for legacy compatibility)
        Campaign c = campaignRepository.findById(t.getCampaignId()).orElseThrow(() -> new RuntimeException("Campaign not found"));
        String normalizedType = normalizeTaskType(t.getTaskType());
        switch (normalizedType) {
            case "FOLLOW" -> c.setCurrentSubscribers(c.getCurrentSubscribers() + 1);
            case "VIEW" -> c.setCurrentViews(c.getCurrentViews() + 1);
            case "LIKE" -> c.setCurrentLikes(c.getCurrentLikes() + 1);
            case "COMMENT" -> c.setCurrentComments(c.getCurrentComments() + 1);
        }
        double rewardValue = t.getEarning() != null ? t.getEarning() : 0.0;
        c.setCurrentAmount(c.getCurrentAmount() + rewardValue);
        campaignRepository.save(c);

        // 6) Update creator_stats
        CreatorStats stats = creatorStatsRepository.findByCreatorId(c.getCreatorId()).orElseGet(() -> new CreatorStats(c.getCreatorId()));
        String contentType = c.getContentType() != null ? c.getContentType() : "VIDEO";
        switch (normalizedType) {
            case "FOLLOW" -> {
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

        return java.util.Map.of("success", true, "status", "ON_HOLD", "holdEndTime", v.getHoldEndTime());
    }

    /**
     * Submit task with proof and automatic risk analysis
     * STAGE 1 & 2: Task Submission + Auto Analysis
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Map<String, Object> submitTaskWithProof(Long taskId, Long viewerId, String proofUrl, 
                                                     String proofPublicId, String proofText,
                                                     String deviceFingerprint, String ipAddress) {
        String lockKey = viewerId + ":" + taskId;
        Object lock = SUBMIT_LOCKS.computeIfAbsent(lockKey, k -> new Object());
        synchronized (lock) {
        // Check if viewer already has this task
        var existingViewerTask = viewerTaskEntityRepository.findByTaskIdAndViewerId(taskId, viewerId);
        
        ViewerTaskEntity viewerTask;
        if (existingViewerTask.isPresent()) {
            viewerTask = existingViewerTask.get();
            if ("UNDER_VERIFICATION".equalsIgnoreCase(viewerTask.getStatus()) || 
                "HOLD".equalsIgnoreCase(viewerTask.getStatus()) ||
                "PAID".equalsIgnoreCase(viewerTask.getStatus()) ||
                "COMPLETED".equalsIgnoreCase(viewerTask.getStatus())) {
                throw new RuntimeException("Task already submitted or completed");
            }
            // Timer enforcement (backward compatible)
            if (viewerTask.getTaskUnlockTime() != null &&
                LocalDateTime.now().isBefore(viewerTask.getTaskUnlockTime())) {
                throw new RuntimeException("Watch time not completed");
            }
        } else {
            // Create new viewer task
            TaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
            
            if (!"OPEN".equalsIgnoreCase(task.getStatus())) {
                throw new RuntimeException("Task not available");
            }

            viewerTask = new ViewerTaskEntity();
            viewerTask.setTaskId(taskId);
            viewerTask.setViewerId(viewerId);
            viewerTask.setAssignedAt(LocalDateTime.now());
        }

        // Validate proof presence
        if (proofUrl == null || proofUrl.trim().isEmpty()) {
            throw new RuntimeException("Proof URL is required");
        }

        // Set proof data
        viewerTask.setProofUrl(proofUrl);
        viewerTask.setProofPublicId(proofPublicId);
        viewerTask.setProofText(proofText);
        viewerTask.setProof(proofUrl); // Keep legacy field for compatibility
        viewerTask.setDeviceFingerprint(deviceFingerprint);
        viewerTask.setIpAddress(ipAddress);
        viewerTask.setSubmittedAt(LocalDateTime.now());
        viewerTask.setProofSubmitted(true);
        if (viewerTask.getTaskStartTime() != null) {
            long seconds = java.time.Duration.between(viewerTask.getTaskStartTime(), LocalDateTime.now()).getSeconds();
            viewerTask.setWatchSeconds((int) Math.max(0, seconds));
        }

        // STAGE 2: Auto Risk Analysis
        Map<String, Object> riskAnalysis;
        try {
            riskAnalysis = riskAnalysisService.analyzeRisk(
                viewerTask, proofUrl, proofText, deviceFingerprint, ipAddress
            );
        } catch (Exception e) {
            LoggerFactory.getLogger(ViewerTaskFlowService.class).error(
                "Risk analysis failed. taskId={}, viewerId={}, proofUrl={}",
                taskId, viewerId, proofUrl, e
            );
            throw new RuntimeException("Risk analysis failed: " + e.getMessage());
        }

        double riskScore = (Double) riskAnalysis.get("riskScore");
        boolean autoFlag = (Boolean) riskAnalysis.get("autoFlag");
        boolean autoReject = (Boolean) riskAnalysis.get("autoReject");

        viewerTask.setRiskScore(riskScore);
        viewerTask.setAutoFlag(autoFlag);

        // Auto-reject if risk score is too high
        if (autoReject) {
            viewerTask.setStatus("REJECTED");
            viewerTask.setRejectionReason("Auto-rejected: " + riskAnalysis.get("reasons"));
            viewerTask = saveOrUpdateOnDuplicate(viewerTask, taskId, viewerId);

            // Issue strike for high-risk submission
            userViolationService.strikeUser(viewerId, "FRAUDULENT_SUBMISSION", 
                "High-risk task submission auto-rejected. Score: " + riskScore, null);

            logger.warn("Task auto-rejected for viewer {} due to high risk score: {}", viewerId, riskScore);
            
            return Map.of(
                "success", false,
                "status", "REJECTED",
                "message", "Task submission rejected due to suspicious activity",
                "riskScore", riskScore
            );
        }

        // Set status to UNDER_VERIFICATION for admin review
        viewerTask.setStatus("UNDER_VERIFICATION");
        viewerTask.setTaskType(toViewerTaskType(taskRepository.findById(taskId)
            .orElseThrow(() -> new RuntimeException("Task not found"))));
        viewerTask = saveOrUpdateOnDuplicate(viewerTask, taskId, viewerId);

        TaskEntity task = taskRepository.findById(taskId)
            .orElseThrow(() -> new RuntimeException("Task not found"));
        addLockedForTask(viewerId, task);

        logger.info("Task submitted for verification. ViewerId: {}, TaskId: {}, RiskScore: {}", 
                    viewerId, taskId, riskScore);

        return Map.of(
            "success", true,
            "status", "UNDER_VERIFICATION",
            "message", "Task submitted successfully and is under verification",
            "viewerTaskId", viewerTask.getId(),
            "riskScore", riskScore,
            "riskLevel", riskAnalysis.get("riskLevel")
        );
        }
    }

    private ViewerTaskEntity saveOrUpdateOnDuplicate(ViewerTaskEntity viewerTask, Long taskId, Long viewerId) {
        try {
            return viewerTaskEntityRepository.saveAndFlush(viewerTask);
        } catch (DataIntegrityViolationException e) {
            // Handle duplicate viewer_task (unique viewer_id + task_id)
            int updated = viewerTaskEntityRepository.updateProofSubmission(
                taskId,
                viewerId,
                viewerTask.getProofUrl(),
                viewerTask.getProofPublicId(),
                viewerTask.getProofText(),
                viewerTask.getProof(),
                viewerTask.getDeviceFingerprint(),
                viewerTask.getIpAddress(),
                viewerTask.getSubmittedAt(),
                viewerTask.getProofSubmitted(),
                viewerTask.getWatchSeconds(),
                viewerTask.getStatus(),
                viewerTask.getTaskType(),
                viewerTask.getRejectionReason(),
                viewerTask.getRiskScore(),
                viewerTask.getAutoFlag()
            );
            if (updated <= 0) {
                throw e;
            }
            return viewerTaskEntityRepository.findByTaskIdAndViewerId(taskId, viewerId)
                .orElseThrow(() -> e);
        }
    }

    /**
     * STAGE 3: Admin approval - moves task to HOLD status
     */
    @Transactional
    public Map<String, Object> approveTaskSubmission(Long viewerTaskId, Long adminId) {
        ViewerTaskEntity viewerTask = viewerTaskEntityRepository.findById(viewerTaskId)
            .orElseThrow(() -> new RuntimeException("ViewerTask not found"));

        if (!"UNDER_VERIFICATION".equalsIgnoreCase(viewerTask.getStatus())) {
            return Map.of("error", "Task is not under verification", "currentStatus", viewerTask.getStatus());
        }

        // Move to HOLD (no wallet update yet)
        LocalDateTime now = LocalDateTime.now();
        viewerTask.setStatus("ON_HOLD");
        viewerTask.setApprovedAt(now);
        viewerTask.setApprovedBy(adminId);
        viewerTask.setHoldStartTime(now);
        viewerTask.setHoldEndTime(now.plusDays(HOLD_PERIOD_DAYS));
        viewerTaskEntityRepository.save(viewerTask);

        logger.info("Task approved by admin {} and moved to HOLD. ViewerTaskId: {}", adminId, viewerTaskId);

        return Map.of(
            "success", true,
            "status", "ON_HOLD",
            "message", "Task approved and placed on hold",
            "holdEndTime", viewerTask.getHoldEndTime()
        );
    }

    /**
     * STAGE 3: Admin rejection - rejects task and issues strike
     */
    @Transactional
    public Map<String, Object> rejectTaskSubmission(Long viewerTaskId, Long adminId, String rejectionReason) {
        ViewerTaskEntity viewerTask = viewerTaskEntityRepository.findById(viewerTaskId)
            .orElseThrow(() -> new RuntimeException("ViewerTask not found"));

        if (!"UNDER_VERIFICATION".equalsIgnoreCase(viewerTask.getStatus()) &&
            !"HOLD".equalsIgnoreCase(viewerTask.getStatus())) {
            return Map.of("error", "Task cannot be rejected in current status", "currentStatus", viewerTask.getStatus());
        }

        viewerTask.setStatus("REJECTED");
        viewerTask.setRejectionReason(rejectionReason != null ? rejectionReason : "Invalid proof submission");
        viewerTask.setApprovedBy(adminId);
        viewerTaskEntityRepository.save(viewerTask);

        TaskEntity task = taskRepository.findById(viewerTask.getTaskId())
            .orElseThrow(() -> new RuntimeException("Task not found"));
        removeLockedForTask(viewerTask.getViewerId(), task);

        // Issue strike to viewer
        userViolationService.strikeUser(
            viewerTask.getViewerId(),
            "INVALID_TASK_SUBMISSION",
            rejectionReason != null ? rejectionReason : "Task submission rejected by admin",
            adminId
        );

        logger.info("Task rejected by admin {}. ViewerTaskId: {}, Reason: {}", adminId, viewerTaskId, rejectionReason);

        return Map.of(
            "success", true,
            "status", "REJECTED",
            "message", "Task rejected and strike issued to viewer"
        );
    }

    /**
     * STAGE 5: Release payment for tasks that completed hold period
     * Called by scheduled job
     */
    @Transactional
    public Map<String, Object> releasePayment(Long viewerTaskId) {
        ViewerTaskEntity viewerTask = viewerTaskEntityRepository.findById(viewerTaskId)
            .orElseThrow(() -> new RuntimeException("ViewerTask not found"));

        if (!"ON_HOLD".equalsIgnoreCase(viewerTask.getStatus())) {
            return Map.of("error", "Task is not in ON_HOLD status", "currentStatus", viewerTask.getStatus());
        }

        if (viewerTask.getHoldEndTime() == null || viewerTask.getHoldEndTime().isAfter(LocalDateTime.now())) {
            return Map.of("error", "Hold period not yet expired");
        }

        TaskEntity task = taskRepository.findById(viewerTask.getTaskId())
            .orElseThrow(() -> new RuntimeException("Task not found"));

        // Update status to PAID
        viewerTask.setStatus("PAID");
        viewerTask.setPaidAt(LocalDateTime.now());
        viewerTask.setCompletedAt(LocalDateTime.now());
        viewerTask.setVerifiedAt(LocalDateTime.now());
        
        // Generate transaction ID
        String txnId = "TXN_" + viewerTaskId + "_" + System.currentTimeMillis();
        viewerTask.setPaymentTxnId(txnId);
        viewerTaskEntityRepository.save(viewerTask);

        task.setStatus("COMPLETED");
        taskRepository.save(task);

        // Release locked funds to available balance
        BigDecimal reward = toAmount(task);
        viewerWalletService.releaseToBalance(viewerTask.getViewerId(), reward);

        // Insert earnings record
        Earning earning = new Earning();
        earning.setViewerId(viewerTask.getViewerId());
        earning.setAmount(reward.doubleValue());
        earning.setDescription("Task verified and paid. TaskId: " + task.getId() + ", Campaign: " + task.getCampaignId());
        earning.setCreatedAt(LocalDateTime.now());
        earningRepository.save(earning);

        // Update campaign counters
        Campaign campaign = campaignRepository.findById(task.getCampaignId())
            .orElseThrow(() -> new RuntimeException("Campaign not found"));
        
        String contentType = campaign.getContentType() != null ? campaign.getContentType() : "VIDEO";
        String normalizedType = normalizeTaskType(task.getTaskType());
        switch (normalizedType) {
            case "FOLLOW" -> campaign.setCurrentSubscribers(campaign.getCurrentSubscribers() + 1);
            case "VIEW" -> campaign.setCurrentViews(campaign.getCurrentViews() + 1);
            case "LIKE" -> campaign.setCurrentLikes(campaign.getCurrentLikes() + 1);
            case "COMMENT" -> campaign.setCurrentComments(campaign.getCurrentComments() + 1);
        }
        campaign.setCurrentAmount(campaign.getCurrentAmount() + reward.doubleValue());
        campaignRepository.save(campaign);

        // Update creator stats
        CreatorStats stats = creatorStatsRepository.findByCreatorId(campaign.getCreatorId())
            .orElseGet(() -> new CreatorStats(campaign.getCreatorId()));
        
        switch (normalizedType) {
            case "FOLLOW" -> {
                stats.setTotalFollowers(stats.getTotalFollowers() + 1);
                stats.setSubscribers(stats.getSubscribers() + 1);
            }
            case "VIEW" -> {
                stats.setTotalViews(stats.getTotalViews() + 1);
                if ("SHORT".equalsIgnoreCase(contentType)) {
                    stats.setShortViews(stats.getShortViews() + 1);
                } else {
                    stats.setVideoViews(stats.getVideoViews() + 1);
                }
            }
            case "LIKE" -> {
                stats.setTotalLikes(stats.getTotalLikes() + 1);
                if ("SHORT".equalsIgnoreCase(contentType)) {
                    stats.setShortLikes(stats.getShortLikes() + 1);
                } else {
                    stats.setVideoLikes(stats.getVideoLikes() + 1);
                }
            }
            case "COMMENT" -> {
                stats.setTotalComments(stats.getTotalComments() + 1);
                if ("SHORT".equalsIgnoreCase(contentType)) {
                    stats.setShortComments(stats.getShortComments() + 1);
                } else {
                    stats.setVideoComments(stats.getVideoComments() + 1);
                }
            }
        }
        creatorStatsRepository.save(stats);

        logger.info("Payment released for ViewerTaskId: {}, Amount: {}, TxnId: {}", viewerTaskId, reward, txnId);

        return Map.of(
            "success", true,
            "status", "PAID",
            "reward", reward,
            "txnId", txnId
        );
    }

    /**
     * Get tasks ready for payment release
     */
    public List<ViewerTaskEntity> getTasksReadyForPayment() {
        List<ViewerTaskEntity> holdTasks = viewerTaskEntityRepository.findByStatus("ON_HOLD");
        return holdTasks.stream()
            .filter(t -> t.getHoldEndTime() != null && t.getHoldEndTime().isBefore(LocalDateTime.now()))
            .toList();
    }

    /**
     * Get tasks ready for proof cleanup (PAID + 30 days)
     */
    public List<ViewerTaskEntity> getTasksReadyForCleanup() {
        List<ViewerTaskEntity> paidTasks = viewerTaskEntityRepository.findByStatus("PAID");
        LocalDateTime cleanupThreshold = LocalDateTime.now().minus(30, ChronoUnit.DAYS);
        
        return paidTasks.stream()
            .filter(t -> t.getPaidAt() != null && t.getPaidAt().isBefore(cleanupThreshold))
            .filter(t -> t.getProofUrl() != null || t.getProofPublicId() != null)
            .toList();
    }

    /**
     * Normalize task type strings to handle both legacy and new enum-based types
     * Maps: SUBSCRIBE/FOLLOW -> FOLLOW
     *       VIEW/VIEW_LONG/VIEW_SHORT -> VIEW
     *       LIKE -> LIKE
     *       COMMENT -> COMMENT
     */
    private String normalizeTaskType(String taskType) {
        if (taskType == null) return "VIEW";
        
        String upper = taskType.toUpperCase();
        
        // Map legacy and new types to normalized form
        if (upper.contains("SUBSCRIBE") || upper.equals("FOLLOW")) {
            return "FOLLOW";
        } else if (upper.contains("VIEW")) {
            return "VIEW";
        } else if (upper.equals("LIKE")) {
            return "LIKE";
        } else if (upper.equals("COMMENT")) {
            return "COMMENT";
        }
        
        return "VIEW"; // Default fallback
    }

    private BigDecimal toAmount(TaskEntity task) {
        double reward = task != null && task.getEarning() != null ? task.getEarning() : 0.0;
        return BigDecimal.valueOf(reward);
    }

    private String toViewerTaskType(TaskEntity task) {
        if (task == null || task.getTaskType() == null) {
            return TaskType.VIEW_LONG.name();
        }
        String upper = task.getTaskType().trim().toUpperCase();
        if (upper.contains("SUBSCRIBE") || upper.equals("FOLLOW")) {
            return TaskType.FOLLOW.name();
        }
        if (upper.contains("VIEW_SHORT") || upper.contains("SHORT")) {
            return TaskType.VIEW_SHORT.name();
        }
        if (upper.contains("VIEW")) {
            return TaskType.VIEW_LONG.name();
        }
        if (upper.equals("LIKE")) {
            return TaskType.LIKE.name();
        }
        if (upper.equals("COMMENT")) {
            return TaskType.COMMENT.name();
        }
        return TaskType.VIEW_LONG.name();
    }

    private void addLockedForTask(Long viewerId, TaskEntity task) {
        BigDecimal reward = toAmount(task);
        viewerWalletService.addEarnings(viewerId, reward);
    }

    private void removeLockedForTask(Long viewerId, TaskEntity task) {
        BigDecimal reward = toAmount(task);
        viewerWalletService.removeLockedEarnings(viewerId, reward);
    }
}
