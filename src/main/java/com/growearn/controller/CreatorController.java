package com.growearn.controller;

import com.growearn.entity.Campaign;
import com.growearn.entity.CreatorStats;
import com.growearn.entity.Earning;
import com.growearn.entity.TaskEntity;
import com.growearn.entity.ViewerTaskEntity;
import com.growearn.entity.User;
import com.growearn.repository.CampaignRepository;
import com.growearn.repository.TaskRepository;
import com.growearn.repository.EarningRepository;
import com.growearn.repository.ViewerTaskEntityRepository;
import com.growearn.repository.UserRepository;
import com.growearn.service.CreatorStatsService;
import com.growearn.security.JwtUtil;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/creator")
public class CreatorController {

    private final CampaignRepository campaignRepository;
    private final TaskRepository taskRepository;
    private final EarningRepository earningRepository;
    private final CreatorStatsService creatorStatsService;
    private final ViewerTaskEntityRepository viewerTaskEntityRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public CreatorController(CampaignRepository campaignRepository, TaskRepository taskRepository,
                             EarningRepository earningRepository, CreatorStatsService creatorStatsService,
                             ViewerTaskEntityRepository viewerTaskEntityRepository,
                             UserRepository userRepository,
                             JwtUtil jwtUtil) {
        this.campaignRepository = campaignRepository;
        this.taskRepository = taskRepository;
        this.earningRepository = earningRepository;
        this.creatorStatsService = creatorStatsService;
        this.viewerTaskEntityRepository = viewerTaskEntityRepository;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    // Dashboard endpoint is provided by CreatorDashboardController

    @GetMapping("/goals")
    public List<Campaign> goals(HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) throw new RuntimeException("Missing token");
        Long creatorId = jwtUtil.extractUserId(auth.substring(7));
        return campaignRepository.findByCreatorId(creatorId);
    }

    @GetMapping("/goals/in-progress")
    public List<Campaign> inProgress(HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) throw new RuntimeException("Missing token");
        Long creatorId = jwtUtil.extractUserId(auth.substring(7));
        return campaignRepository.findByCreatorIdAndStatus(creatorId, "ACTIVE");
    }

    @GetMapping("/goals/completed")
    public List<Campaign> completed(HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) throw new RuntimeException("Missing token");
        Long creatorId = jwtUtil.extractUserId(auth.substring(7));
        return campaignRepository.findByCreatorIdAndStatus(creatorId, "COMPLETED");
    }

    @GetMapping("/transactions")
    public List<Earning> transactions(HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) throw new RuntimeException("Missing token");
        Long creatorId = jwtUtil.extractUserId(auth.substring(7));
        // find campaigns for this creator
        java.util.List<Campaign> campaigns = campaignRepository.findByCreatorId(creatorId);
        java.util.List<Long> campaignIds = campaigns.stream().map(Campaign::getId).toList();
        if (campaignIds.isEmpty()) return List.of();
        // find tasks for these campaigns
        java.util.List<Long> taskIds = taskRepository.findByCampaignIdIn(campaignIds).stream().map(t -> t.getId()).toList();
        if (taskIds.isEmpty()) return List.of();
        return earningRepository.findByTaskIdIn(taskIds);
    }

    /**
     * Get tasks under verification for the creator's campaigns.
     * These are tasks submitted by viewers and awaiting admin approval.
     * GET /api/creator/tasks/pending-verification
     */
    @GetMapping("/tasks/pending-verification")
    public List<Map<String, Object>> getPendingVerificationTasks(HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) throw new RuntimeException("Missing token");
        Long creatorId = jwtUtil.extractUserId(auth.substring(7));
        
        // Get all campaigns for this creator
        List<Campaign> campaigns = campaignRepository.findByCreatorId(creatorId);
        List<Long> campaignIds = campaigns.stream().map(Campaign::getId).toList();
        if (campaignIds.isEmpty()) return List.of();
        
        // Get all tasks for these campaigns
        List<TaskEntity> tasks = taskRepository.findByCampaignIdIn(campaignIds);
        List<Long> taskIds = tasks.stream().map(TaskEntity::getId).toList();
        if (taskIds.isEmpty()) return List.of();
        
        // Get viewer tasks that are under verification for these tasks
        List<ViewerTaskEntity> pendingTasks = viewerTaskEntityRepository.findByTaskIdInAndStatus(taskIds, "UNDER_VERIFICATION");
        
        return enrichCreatorViewerTasks(pendingTasks, tasks, campaigns);
    }

    /**
     * Get completed/approved tasks for the creator's campaigns.
     * These are tasks that have been approved by admin.
     * GET /api/creator/tasks/completed
     */
    @GetMapping("/tasks/completed")
    public List<Map<String, Object>> getCompletedTasks(HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) throw new RuntimeException("Missing token");
        Long creatorId = jwtUtil.extractUserId(auth.substring(7));
        
        // Get all campaigns for this creator
        List<Campaign> campaigns = campaignRepository.findByCreatorId(creatorId);
        List<Long> campaignIds = campaigns.stream().map(Campaign::getId).toList();
        if (campaignIds.isEmpty()) return List.of();
        
        // Get all tasks for these campaigns
        List<TaskEntity> tasks = taskRepository.findByCampaignIdIn(campaignIds);
        List<Long> taskIds = tasks.stream().map(TaskEntity::getId).toList();
        if (taskIds.isEmpty()) return List.of();
        
        // Get viewer tasks that are completed for these tasks
        List<ViewerTaskEntity> completedTasks = viewerTaskEntityRepository.findByTaskIdInAndStatus(taskIds, "PAID");

        return enrichCreatorViewerTasks(completedTasks, tasks, campaigns);
    }

    /**
     * Helper method to enrich viewer tasks with task and campaign details for creator view.
     */
    private List<Map<String, Object>> enrichCreatorViewerTasks(
            List<ViewerTaskEntity> viewerTasks, 
            List<TaskEntity> tasks, 
            List<Campaign> campaigns) {
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        // Create lookup maps for efficiency
        Map<Long, TaskEntity> taskMap = new HashMap<>();
        for (TaskEntity t : tasks) {
            taskMap.put(t.getId(), t);
        }
        
        Map<Long, Campaign> campaignMap = new HashMap<>();
        for (Campaign c : campaigns) {
            campaignMap.put(c.getId(), c);
        }
        
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
            
            // Get task details
            TaskEntity task = taskMap.get(vt.getTaskId());
            if (task != null) {
                taskData.put("taskType", task.getTaskType());
                taskData.put("targetLink", task.getTargetLink());
                taskData.put("earning", task.getEarning());
                
                // Get campaign details
                Campaign campaign = campaignMap.get(task.getCampaignId());
                if (campaign != null) {
                    taskData.put("campaignId", campaign.getId());
                    taskData.put("channelName", campaign.getChannelName());
                    taskData.put("platform", campaign.getPlatform());
                    taskData.put("contentType", campaign.getContentType());
                    taskData.put("videoLink", campaign.getVideoLink());
                }
            }
            
            // Get viewer details
            User viewer = userRepository.findById(vt.getViewerId()).orElse(null);
            if (viewer != null) {
                taskData.put("viewerEmail", viewer.getEmail());
                taskData.put("viewerName", viewer.getEmail());
            }
            
            result.add(taskData);
        }
        
        return result;
    }
}
