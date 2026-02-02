package com.growearn.controller;

import com.growearn.entity.Campaign;
import com.growearn.entity.CreatorStats;
import com.growearn.entity.Earning;
import com.growearn.repository.CampaignRepository;
import com.growearn.repository.TaskRepository;
import com.growearn.repository.EarningRepository;
import com.growearn.service.CreatorStatsService;
import com.growearn.security.JwtUtil;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/creator")
public class CreatorController {

    private final CampaignRepository campaignRepository;
    private final TaskRepository taskRepository;
    private final EarningRepository earningRepository;
    private final CreatorStatsService creatorStatsService;
    private final JwtUtil jwtUtil;

    public CreatorController(CampaignRepository campaignRepository, TaskRepository taskRepository,
                             EarningRepository earningRepository, CreatorStatsService creatorStatsService,
                             JwtUtil jwtUtil) {
        this.campaignRepository = campaignRepository;
        this.taskRepository = taskRepository;
        this.earningRepository = earningRepository;
        this.creatorStatsService = creatorStatsService;
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
        return campaignRepository.findByCreatorIdAndStatus(creatorId, "IN_PROGRESS");
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
}
