package com.growearn.service;

import com.growearn.entity.*;
import com.growearn.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ViewerTaskFlowService {

    private final TaskRepository taskRepository;
    private final ViewerTaskEntityRepository viewerTaskEntityRepository;
    private final EarningRepository earningRepository;
    private final WalletRepository walletRepository;
    private final CampaignRepository campaignRepository;
    private final CreatorStatsRepository creatorStatsRepository;

    public ViewerTaskFlowService(TaskRepository taskRepository,
                                 ViewerTaskEntityRepository viewerTaskEntityRepository,
                                 EarningRepository earningRepository,
                                 WalletRepository walletRepository,
                                 CampaignRepository campaignRepository,
                                 CreatorStatsRepository creatorStatsRepository) {
        this.taskRepository = taskRepository;
        this.viewerTaskEntityRepository = viewerTaskEntityRepository;
        this.earningRepository = earningRepository;
        this.walletRepository = walletRepository;
        this.campaignRepository = campaignRepository;
        this.creatorStatsRepository = creatorStatsRepository;
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
}
