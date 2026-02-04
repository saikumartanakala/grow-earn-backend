package com.growearn.service;

import com.growearn.entity.*;
import com.growearn.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private final WalletRepository walletRepository;
    private final CampaignRepository campaignRepository;
    private final CreatorStatsRepository creatorStatsRepository;
    private final RiskAnalysisService riskAnalysisService;
    private final UserViolationService userViolationService;

    public ViewerTaskFlowService(TaskRepository taskRepository,
                                 ViewerTaskEntityRepository viewerTaskEntityRepository,
                                 EarningRepository earningRepository,
                                 WalletRepository walletRepository,
                                 CampaignRepository campaignRepository,
                                 CreatorStatsRepository creatorStatsRepository,
                                 RiskAnalysisService riskAnalysisService,
                                 UserViolationService userViolationService) {
        this.taskRepository = taskRepository;
        this.viewerTaskEntityRepository = viewerTaskEntityRepository;
        this.earningRepository = earningRepository;
        this.walletRepository = walletRepository;
        this.campaignRepository = campaignRepository;
        this.creatorStatsRepository = creatorStatsRepository;
        this.riskAnalysisService = riskAnalysisService;
        this.userViolationService = userViolationService;
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
        t.setStatus("UNDER_VERIFICATION");
        taskRepository.save(t);

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
            viewerTaskEntityRepository.save(v);
            
            TaskEntity t = taskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("Task not found"));
            t.setStatus("UNDER_VERIFICATION");
            taskRepository.save(t);
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
        v.setStatus("UNDER_VERIFICATION");
        v.setProof("Task completed by viewer at " + LocalDateTime.now());
        v.setAssignedAt(LocalDateTime.now());
        viewerTaskEntityRepository.save(v);
        
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

        // 1) Update viewer_tasks -> COMPLETED
        v.setStatus("COMPLETED");
        v.setCompletedAt(LocalDateTime.now());
        viewerTaskEntityRepository.save(v);

        // 2) Update tasks -> COMPLETED
        t.setStatus("COMPLETED");
        taskRepository.save(t);

        // 3) Insert into earnings
        double reward = t.getEarning() != null ? t.getEarning() : 0.0;
        Earning e = new Earning();
        e.setViewerId(v.getViewerId());
        e.setAmount(reward);
        e.setDescription("task=" + t.getId() + ",campaign=" + t.getCampaignId());
        e.setCreatedAt(LocalDateTime.now());
        earningRepository.save(e);

        // 4) Update wallet balance
        WalletEntity w = walletRepository.findByViewerId(v.getViewerId()).orElseGet(() -> {
            WalletEntity ne = new WalletEntity();
            ne.setViewerId(v.getViewerId());
            ne.setBalance(0.0);
            return walletRepository.save(ne);
        });
        w.setBalance(w.getBalance() + reward);
        walletRepository.save(w);

        // 5) Update campaigns counters
        Campaign c = campaignRepository.findById(t.getCampaignId()).orElseThrow(() -> new RuntimeException("Campaign not found"));
        switch (t.getTaskType()) {
            case "SUBSCRIBE" -> c.setCurrentSubscribers(c.getCurrentSubscribers() + 1);
            case "VIEW" -> c.setCurrentViews(c.getCurrentViews() + 1);
            case "LIKE" -> c.setCurrentLikes(c.getCurrentLikes() + 1);
            case "COMMENT" -> c.setCurrentComments(c.getCurrentComments() + 1);
        }
        c.setCurrentAmount(c.getCurrentAmount() + reward);
        campaignRepository.save(c);

        // 6) Update creator_stats
        CreatorStats stats = creatorStatsRepository.findByCreatorId(c.getCreatorId()).orElseGet(() -> new CreatorStats(c.getCreatorId()));
        String contentType = c.getContentType() != null ? c.getContentType() : "VIDEO";
        switch (t.getTaskType()) {
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

        return java.util.Map.of("success", true, "reward", reward);
    }

    /**
     * Submit task with proof and automatic risk analysis
     * STAGE 1 & 2: Task Submission + Auto Analysis
     */
    @Transactional
    public Map<String, Object> submitTaskWithProof(Long taskId, Long viewerId, String proofUrl, 
                                                     String proofPublicId, String proofText,
                                                     String deviceFingerprint, String ipAddress) {
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

        // STAGE 2: Auto Risk Analysis
        Map<String, Object> riskAnalysis = riskAnalysisService.analyzeRisk(
            viewerTask, proofUrl, proofText, deviceFingerprint, ipAddress
        );

        double riskScore = (Double) riskAnalysis.get("riskScore");
        boolean autoFlag = (Boolean) riskAnalysis.get("autoFlag");
        boolean autoReject = (Boolean) riskAnalysis.get("autoReject");

        viewerTask.setRiskScore(riskScore);
        viewerTask.setAutoFlag(autoFlag);

        // Auto-reject if risk score is too high
        if (autoReject) {
            viewerTask.setStatus("REJECTED");
            viewerTask.setRejectionReason("Auto-rejected: " + riskAnalysis.get("reasons"));
            viewerTaskEntityRepository.save(viewerTask);

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
        viewerTaskEntityRepository.save(viewerTask);

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

        // Move to HOLD status (7-day hold period)
        viewerTask.setStatus("HOLD");
        viewerTask.setApprovedAt(LocalDateTime.now());
        viewerTask.setApprovedBy(adminId);
        viewerTask.setHoldExpiry(LocalDateTime.now().plus(HOLD_PERIOD_DAYS, ChronoUnit.DAYS));
        viewerTaskEntityRepository.save(viewerTask);

        logger.info("Task approved by admin {} and moved to HOLD. ViewerTaskId: {}", adminId, viewerTaskId);

        return Map.of(
            "success", true,
            "status", "HOLD",
            "message", "Task approved and placed on hold for " + HOLD_PERIOD_DAYS + " days",
            "holdExpiry", viewerTask.getHoldExpiry()
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

        if (!"HOLD".equalsIgnoreCase(viewerTask.getStatus())) {
            return Map.of("error", "Task is not in HOLD status", "currentStatus", viewerTask.getStatus());
        }

        if (viewerTask.getHoldExpiry() == null || viewerTask.getHoldExpiry().isAfter(LocalDateTime.now())) {
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

        // Credit wallet
        double reward = task.getEarning() != null ? task.getEarning() : 0.0;
        WalletEntity wallet = walletRepository.findByViewerId(viewerTask.getViewerId())
            .orElseGet(() -> {
                WalletEntity newWallet = new WalletEntity();
                newWallet.setViewerId(viewerTask.getViewerId());
                newWallet.setBalance(0.0);
                return walletRepository.save(newWallet);
            });
        
        wallet.setBalance(wallet.getBalance() + reward);
        walletRepository.save(wallet);

        // Insert earnings record
        Earning earning = new Earning();
        earning.setViewerId(viewerTask.getViewerId());
        earning.setAmount(reward);
        earning.setDescription("Task verified and paid. TaskId: " + task.getId() + ", Campaign: " + task.getCampaignId());
        earning.setCreatedAt(LocalDateTime.now());
        earningRepository.save(earning);

        // Update campaign counters
        Campaign campaign = campaignRepository.findById(task.getCampaignId())
            .orElseThrow(() -> new RuntimeException("Campaign not found"));
        
        String contentType = campaign.getContentType() != null ? campaign.getContentType() : "VIDEO";
        switch (task.getTaskType().toUpperCase()) {
            case "SUBSCRIBE" -> campaign.setCurrentSubscribers(campaign.getCurrentSubscribers() + 1);
            case "VIEW", "VIDEO_VIEW" -> campaign.setCurrentViews(campaign.getCurrentViews() + 1);
            case "SHORT_VIEW" -> campaign.setCurrentViews(campaign.getCurrentViews() + 1);
            case "LIKE", "VIDEO_LIKE" -> campaign.setCurrentLikes(campaign.getCurrentLikes() + 1);
            case "SHORT_LIKE" -> campaign.setCurrentLikes(campaign.getCurrentLikes() + 1);
            case "COMMENT" -> campaign.setCurrentComments(campaign.getCurrentComments() + 1);
        }
        campaign.setCurrentAmount(campaign.getCurrentAmount() + reward);
        campaignRepository.save(campaign);

        // Update creator stats
        CreatorStats stats = creatorStatsRepository.findByCreatorId(campaign.getCreatorId())
            .orElseGet(() -> new CreatorStats(campaign.getCreatorId()));
        
        switch (task.getTaskType().toUpperCase()) {
            case "SUBSCRIBE" -> {
                stats.setTotalFollowers(stats.getTotalFollowers() + 1);
                stats.setSubscribers(stats.getSubscribers() + 1);
            }
            case "VIEW", "VIDEO_VIEW" -> {
                stats.setTotalViews(stats.getTotalViews() + 1);
                stats.setVideoViews(stats.getVideoViews() + 1);
            }
            case "SHORT_VIEW" -> {
                stats.setTotalViews(stats.getTotalViews() + 1);
                stats.setShortViews(stats.getShortViews() + 1);
            }
            case "LIKE", "VIDEO_LIKE" -> {
                stats.setTotalLikes(stats.getTotalLikes() + 1);
                stats.setVideoLikes(stats.getVideoLikes() + 1);
            }
            case "SHORT_LIKE" -> {
                stats.setTotalLikes(stats.getTotalLikes() + 1);
                stats.setShortLikes(stats.getShortLikes() + 1);
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
        List<ViewerTaskEntity> holdTasks = viewerTaskEntityRepository.findByStatus("HOLD");
        return holdTasks.stream()
            .filter(t -> t.getHoldExpiry() != null && t.getHoldExpiry().isBefore(LocalDateTime.now()))
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
}
