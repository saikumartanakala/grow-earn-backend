package com.growearn.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import com.growearn.entity.Campaign;
import com.growearn.entity.TaskEntity;
import com.growearn.entity.ViewerTaskEntity;
import com.growearn.repository.TaskRepository;
import com.growearn.repository.ViewerTaskEntityRepository;
import com.growearn.security.JwtUtil;
import com.growearn.service.CampaignService;
import com.growearn.service.CreatorWalletService;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping({"/api/creator/campaign", "/api/creator/campaigns"})
public class CampaignController {

    private final CampaignService campaignService;
    private final TaskRepository taskRepository;
    private final ViewerTaskEntityRepository viewerTaskEntityRepository;
    private final JwtUtil jwtUtil;
    private final CreatorWalletService creatorWalletService;

    public CampaignController(CampaignService campaignService, TaskRepository taskRepository,
                              ViewerTaskEntityRepository viewerTaskEntityRepository, JwtUtil jwtUtil,
                              CreatorWalletService creatorWalletService) {
        this.campaignService = campaignService;
        this.taskRepository = taskRepository;
        this.viewerTaskEntityRepository = viewerTaskEntityRepository;
        this.jwtUtil = jwtUtil;
        this.creatorWalletService = creatorWalletService;
    }

    /**
     * Step 1: Goal selection endpoint for campaign creation wizard.
     * Accepts a set of selected goals and returns allowed/required fields for next step.
     * Example: POST /api/creator/campaign/goal-select { "goalTypes": ["SUBSCRIBE", "VIEW"] }
     */
    @PostMapping("/goal-select")
    public ResponseEntity<?> selectGoals(@RequestBody Map<String, Set<String>> body) {
        Set<String> goalTypes = body.get("goalTypes");
        if (goalTypes == null || goalTypes.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No goalTypes provided"));
        }
        // For each goal type, define required fields for the next step
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("selectedGoals", goalTypes);
        // Example: for SUBSCRIBE, need subscriberGoal; for VIEW, need viewsGoal, etc.
        java.util.List<String> requiredFields = new java.util.ArrayList<>();
        if (goalTypes.contains("SUBSCRIBE")) requiredFields.add("subscriberGoal");
        if (goalTypes.contains("VIEW")) requiredFields.add("viewsGoal");
        if (goalTypes.contains("LIKE")) requiredFields.add("likesGoal");
        if (goalTypes.contains("COMMENT")) requiredFields.add("commentsGoal");
        response.put("requiredFields", requiredFields);
        return ResponseEntity.ok(response);
    }

    /**
     * Step 2: Calculate campaign amount based on selected goals and counts.
     * Example: POST /api/creator/campaign/calculate-amount { "subscriberGoal": 100, "viewsGoal": 500, ... }
     * Returns: { "totalAmount": 123.45, "breakdown": { ... } }
     */
    @PostMapping("/calculate-amount")
    public ResponseEntity<?> calculateAmount(@RequestBody Map<String, Object> body) {
        // Example pricing logic (replace with real business logic as needed)
        int subscriberGoal = ((Number) body.getOrDefault("subscriberGoal", 0)).intValue();
        int viewsGoal = ((Number) body.getOrDefault("viewsGoal", 0)).intValue();
        int likesGoal = ((Number) body.getOrDefault("likesGoal", 0)).intValue();
        int commentsGoal = ((Number) body.getOrDefault("commentsGoal", 0)).intValue();

        double subscriberPrice = 2.0; // Example: $2 per subscriber
        double viewPrice = 0.05;      // Example: $0.05 per view
        double likePrice = 0.10;      // Example: $0.10 per like
        double commentPrice = 0.20;   // Example: $0.20 per comment

        double totalAmount = subscriberGoal * subscriberPrice +
                            viewsGoal * viewPrice +
                            likesGoal * likePrice +
                            commentsGoal * commentPrice;

        Map<String, Object> breakdown = Map.of(
            "subscriberAmount", subscriberGoal * subscriberPrice,
            "viewsAmount", viewsGoal * viewPrice,
            "likesAmount", likesGoal * likePrice,
            "commentsAmount", commentsGoal * commentPrice
        );

        Map<String, Object> response = Map.of(
            "totalAmount", totalAmount,
            "breakdown", breakdown
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Step 2.5: Validate campaign details before creation.
     * POST /api/creator/campaign/validate-details
     * Body: { "channelName": "...", "channelLink": "...", "contentType": "...", "videoLink": "...", "videoDuration": "..." }
     * Response:
     *   If all required fields are present and valid: { "valid": true }
     *   If any required fields are missing or invalid: { "valid": false, "missingFields": [ ... ] }
     */
    @PostMapping("/validate-details")
    public ResponseEntity<?> validateDetails(@RequestBody Map<String, Object> body) {
        java.util.List<String> missingFields = new java.util.ArrayList<>();
        if (body.get("channelName") == null || body.get("channelName").toString().trim().isEmpty()) missingFields.add("channelName");
        if (body.get("channelLink") == null || body.get("channelLink").toString().trim().isEmpty()) missingFields.add("channelLink");
        if (body.get("contentType") == null || body.get("contentType").toString().trim().isEmpty()) missingFields.add("contentType");
        if (body.get("videoLink") == null || body.get("videoLink").toString().trim().isEmpty()) missingFields.add("videoLink");
        if ((body.get("contentType") != null && 
             ("VIDEO".equalsIgnoreCase(body.get("contentType").toString()) || "SHORT".equalsIgnoreCase(body.get("contentType").toString())))
            && (body.get("videoDuration") == null || body.get("videoDuration").toString().trim().isEmpty())) {
            missingFields.add("videoDuration");
        }
        if (!missingFields.isEmpty()) {
            return ResponseEntity.ok(java.util.Map.of("valid", false, "missingFields", missingFields));
        }
        return ResponseEntity.ok(java.util.Map.of("valid", true));
    }

    /**
     * Step 3: Final campaign creation endpoint for multi-step flow.
     * Accepts all required fields from previous steps and creates the campaign.
     * Example: POST /api/creator/campaign/create { ...full campaign fields... }
     *
     * Required fields by step:
     * 1. Goal selection: goalType (e.g., "SVLC" for all goals)
     * 2. Amount calculation: subscriberGoal, viewsGoal, likesGoal, commentsGoal, totalAmount
     * 3. Details: platform, channelName, channelLink, contentType, videoLink, videoDuration (if VIDEO/SHORT), status
     */
    @PostMapping("/create")
    public ResponseEntity<?> createCampaign(@RequestBody Campaign campaign, HttpServletRequest request) {
        // Validate required fields for multi-step flow
        java.util.List<String> missingFields = new java.util.ArrayList<>();
        if (isNullOrEmpty(campaign.getGoalType())) missingFields.add("goalType");
        if (campaign.getSubscriberGoal() < 0) missingFields.add("subscriberGoal");
        if (campaign.getViewsGoal() < 0) missingFields.add("viewsGoal");
        if (campaign.getLikesGoal() < 0) missingFields.add("likesGoal");
        if (campaign.getCommentsGoal() < 0) missingFields.add("commentsGoal");
        if (campaign.getTotalAmount() <= 0.0) missingFields.add("totalAmount");
        if (campaign.getPlatform() == null) missingFields.add("platform");
        if (isNullOrEmpty(campaign.getChannelName())) missingFields.add("channelName");
        if (isNullOrEmpty(campaign.getChannelLink())) missingFields.add("channelLink");
        if (isNullOrEmpty(campaign.getContentType())) missingFields.add("contentType");
        if (isNullOrEmpty(campaign.getVideoLink())) missingFields.add("videoLink");
        if (("VIDEO".equalsIgnoreCase(campaign.getContentType()) || "SHORT".equalsIgnoreCase(campaign.getContentType())) && isNullOrEmpty(campaign.getVideoDuration())) missingFields.add("videoDuration");
        if (isNullOrEmpty(campaign.getStatus())) missingFields.add("status");
        if (!missingFields.isEmpty()) {
            System.out.println("[CampaignController] Missing required campaign fields: " + missingFields + " | Campaign: " + campaign);
            return ResponseEntity.badRequest().body(java.util.Map.of(
                "error", "Missing required fields",
                "missingFields", missingFields
            ));
        }


        // Log campaign creation attempt and authorities
        System.out.println("[CampaignController] Attempting to create campaign: " + campaign);
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        System.out.println("[CampaignController] Authenticated principal: " + auth.getPrincipal());
        System.out.println("[CampaignController] Authorities: " + auth.getAuthorities());

        // Assign creatorId from JWT
        String token = request.getHeader("Authorization").substring(7);
        Long creatorId = jwtUtil.extractUserId(token);
        campaign.setCreatorId(creatorId);

        try {
            Campaign saved = campaignService.createCampaign(campaign);
            System.out.println("[CampaignController] Campaign created for creatorId: " + creatorId + ", campaignId: " + (saved != null ? saved.getId() : "null"));
            // Return a DTO/map instead of the JPA entity
            Map<String, Object> dto = new java.util.HashMap<>();
            dto.put("id", saved.getId());
            dto.put("creatorId", saved.getCreatorId());
            dto.put("title", saved.getTitle());
            dto.put("description", saved.getDescription());
            dto.put("goalAmount", saved.getGoalAmount());
            dto.put("currentAmount", saved.getCurrentAmount());
            dto.put("updatedAt", saved.getUpdatedAt() != null ? saved.getUpdatedAt().toString() : null);
            dto.put("platform", saved.getPlatform());
            dto.put("goalType", saved.getGoalType());
            dto.put("channelName", saved.getChannelName());
            dto.put("channelLink", saved.getChannelLink());
            dto.put("contentType", saved.getContentType());
            dto.put("videoLink", saved.getVideoLink());
            dto.put("videoDuration", saved.getVideoDuration());
            dto.put("subscriberGoal", saved.getSubscriberGoal());
            dto.put("viewsGoal", saved.getViewsGoal());
            dto.put("likesGoal", saved.getLikesGoal());
            dto.put("commentsGoal", saved.getCommentsGoal());
            dto.put("currentSubscribers", saved.getCurrentSubscribers());
            dto.put("currentViews", saved.getCurrentViews());
            dto.put("currentLikes", saved.getCurrentLikes());
            dto.put("currentComments", saved.getCurrentComments());
            dto.put("subscriberTaskCount", saved.getSubscriberTaskCount());
            dto.put("viewsTaskCount", saved.getViewsTaskCount());
            dto.put("likesTaskCount", saved.getLikesTaskCount());
            dto.put("commentsTaskCount", saved.getCommentsTaskCount());
            dto.put("totalAmount", saved.getTotalAmount());
            dto.put("status", saved.getStatus());
            com.growearn.entity.CreatorWallet wallet = creatorWalletService.getOrCreateWallet(creatorId);
            java.math.BigDecimal available = wallet.getBalance().subtract(wallet.getLockedBalance());
            dto.put("wallet", Map.of(
                "balance", wallet.getBalance(),
                "available", available,
                "locked", wallet.getLockedBalance()
            ));
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            System.out.println("[CampaignController] Failed to create campaign: " + e.getMessage());
            return ResponseEntity.status(400).body("Failed to create campaign: " + e.getMessage());
        }
    }

    // Helper method for validation
    private boolean isNullOrEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    @GetMapping("/list")
    public ResponseEntity<?> getCampaignsByStatus(@RequestParam("status") String status) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        System.out.println("[CampaignController] Principal from SecurityContext (GET): " + principal + " (" + (principal != null ? principal.getClass().getName() : "null") + ")");
        Long creatorId = null;
        try {
            if (principal instanceof Long) {
                creatorId = (Long) principal;
            } else if (principal instanceof String) {
                creatorId = Long.parseLong((String) principal);
            }
        } catch (Exception e) {
            System.out.println("[CampaignController] Error parsing principal (GET): " + e.getMessage());
            return ResponseEntity.status(403).body("Invalid JWT principal: " + principal);
        }
        if (creatorId == null) {
            System.out.println("[CampaignController] Missing or invalid creatorId in JWT principal. (GET)");
            return ResponseEntity.status(403).body("Missing or invalid creatorId in JWT principal.");
        }
        String normalizedStatus = status.equalsIgnoreCase("in-progress") ? "ACTIVE" : status.equalsIgnoreCase("completed") ? "COMPLETED" : status.toUpperCase();
        List<Campaign> campaigns = campaignService.getCampaignsByCreatorAndStatus(creatorId, normalizedStatus);
        // Map to always include required fields with defaults
        List<Campaign> safeCampaigns = (campaigns != null) ? campaigns : new java.util.ArrayList<>();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = safeCampaigns.stream()
            .map((Campaign c) -> {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", c.getId() != null ? c.getId() : "");
                map.put("title", c.getTitle() != null ? c.getTitle() : "");
                map.put("description", c.getDescription() != null ? c.getDescription() : "");
                map.put("goalAmount", c.getGoalAmount());
                map.put("currentAmount", c.getCurrentAmount());
                map.put("status", c.getStatus() != null ? c.getStatus().toLowerCase() : "in-progress");
                map.put("updatedAt", c.getUpdatedAt() != null ? c.getUpdatedAt().toString() : java.time.LocalDateTime.now().toString());
                return map;
            })
            .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(java.util.Map.of("campaigns", result));
    }

    // Support /api/creator/campaigns?status=... and /api/creator/campaign?status=...
    @GetMapping("")
    public ResponseEntity<?> getCampaignsByStatusRoot(@RequestParam("status") String status) {
        return getCampaignsByStatus(status);
    }

    @GetMapping("/")
    public ResponseEntity<?> getCampaignsByStatusRootSlash(@RequestParam("status") String status) {
        return getCampaignsByStatus(status);
    }

    /**
     * Get ALL campaigns for the creator (regardless of status).
     * GET /api/creator/campaigns/all
     * Returns all campaigns with task progress information.
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllCampaigns(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Missing token"));
        }
        Long creatorId = jwtUtil.extractUserId(auth.substring(7));
        
        List<Campaign> campaigns = campaignService.getCampaignsByCreator(creatorId);
        if (campaigns == null || campaigns.isEmpty()) {
            return ResponseEntity.ok(Map.of("campaigns", new ArrayList<>()));
        }

        // Enrich campaigns with task progress data
        List<Map<String, Object>> enrichedCampaigns = new ArrayList<>();
        for (Campaign c : campaigns) {
            Map<String, Object> map = enrichCampaignWithProgress(c);
            enrichedCampaigns.add(map);
        }

        return ResponseEntity.ok(Map.of("campaigns", enrichedCampaigns));
    }

    /**
     * Get campaigns with pending viewer tasks (tasks submitted but awaiting admin approval).
     * GET /api/creator/campaigns/with-pending-tasks
     */
    @GetMapping("/with-pending-tasks")
    public ResponseEntity<?> getCampaignsWithPendingTasks(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Missing token"));
        }
        Long creatorId = jwtUtil.extractUserId(auth.substring(7));
        
        List<Campaign> campaigns = campaignService.getCampaignsByCreator(creatorId);
        if (campaigns == null || campaigns.isEmpty()) {
            return ResponseEntity.ok(Map.of("campaigns", new ArrayList<>()));
        }

        // Filter campaigns that have pending verification tasks
        List<Map<String, Object>> campaignsWithPending = new ArrayList<>();
        for (Campaign c : campaigns) {
            Map<String, Object> enriched = enrichCampaignWithProgress(c);
            int pendingCount = (int) enriched.getOrDefault("pendingVerificationCount", 0);
            if (pendingCount > 0) {
                campaignsWithPending.add(enriched);
            }
        }

        return ResponseEntity.ok(Map.of("campaigns", campaignsWithPending));
    }

    /**
     * Get campaigns with completed viewer tasks (tasks approved by admin).
     * GET /api/creator/campaigns/with-completed-tasks
     */
    @GetMapping("/with-completed-tasks")
    public ResponseEntity<?> getCampaignsWithCompletedTasks(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Missing token"));
        }
        Long creatorId = jwtUtil.extractUserId(auth.substring(7));
        
        List<Campaign> campaigns = campaignService.getCampaignsByCreator(creatorId);
        if (campaigns == null || campaigns.isEmpty()) {
            return ResponseEntity.ok(Map.of("campaigns", new ArrayList<>()));
        }

        // Filter campaigns that have completed tasks
        List<Map<String, Object>> campaignsWithCompleted = new ArrayList<>();
        for (Campaign c : campaigns) {
            Map<String, Object> enriched = enrichCampaignWithProgress(c);
            int completedCount = (int) enriched.getOrDefault("completedTasksCount", 0);
            if (completedCount > 0) {
                campaignsWithCompleted.add(enriched);
            }
        }

        return ResponseEntity.ok(Map.of("campaigns", campaignsWithCompleted));
    }

    /**
     * Helper method to enrich a campaign with task progress information.
     */
    private Map<String, Object> enrichCampaignWithProgress(Campaign c) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", c.getId());
        map.put("title", c.getTitle() != null ? c.getTitle() : "");
        map.put("description", c.getDescription() != null ? c.getDescription() : "");
        map.put("goalType", c.getGoalType());
        map.put("platform", c.getPlatform());
        map.put("channelName", c.getChannelName());
        map.put("channelLink", c.getChannelLink());
        map.put("contentType", c.getContentType());
        map.put("videoLink", c.getVideoLink());
        map.put("videoDuration", c.getVideoDuration());
        map.put("subscriberGoal", c.getSubscriberGoal());
        map.put("viewsGoal", c.getViewsGoal());
        map.put("likesGoal", c.getLikesGoal());
        map.put("commentsGoal", c.getCommentsGoal());
        map.put("currentSubscribers", c.getCurrentSubscribers());
        map.put("currentViews", c.getCurrentViews());
        map.put("currentLikes", c.getCurrentLikes());
        map.put("currentComments", c.getCurrentComments());
        map.put("totalAmount", c.getTotalAmount());
        map.put("goalAmount", c.getGoalAmount());
        map.put("currentAmount", c.getCurrentAmount());
        map.put("status", c.getStatus() != null ? c.getStatus() : "IN_PROGRESS");
        map.put("updatedAt", c.getUpdatedAt() != null ? c.getUpdatedAt().toString() : java.time.LocalDateTime.now().toString());

        // Get task counts for this campaign
        List<TaskEntity> tasks = taskRepository.findByCampaignIdIn(List.of(c.getId()));
        int totalTasks = tasks.size();
        int openTasks = 0;
        int underVerification = 0;
        int completedTasks = 0;

        if (!tasks.isEmpty()) {
            List<Long> taskIds = tasks.stream().map(TaskEntity::getId).toList();
            
            // Count viewer tasks by status
            List<ViewerTaskEntity> pendingViewerTasks = viewerTaskEntityRepository.findByTaskIdInAndStatus(taskIds, "UNDER_VERIFICATION");
            List<ViewerTaskEntity> completedViewerTasks = viewerTaskEntityRepository.findByTaskIdInAndStatus(taskIds, "PAID");
            
            underVerification = pendingViewerTasks.size();
            completedTasks = completedViewerTasks.size();
            
            // Count open tasks
            for (TaskEntity t : tasks) {
                if ("OPEN".equalsIgnoreCase(t.getStatus())) {
                    openTasks++;
                }
            }
        }

        map.put("totalTasks", totalTasks);
        map.put("openTasksCount", openTasks);
        map.put("pendingVerificationCount", underVerification);
        map.put("completedTasksCount", completedTasks);
        
        // Calculate progress percentage
        int totalGoals = c.getSubscriberGoal() + c.getViewsGoal() + c.getLikesGoal() + c.getCommentsGoal();
        int currentProgress = c.getCurrentSubscribers() + c.getCurrentViews() + c.getCurrentLikes() + c.getCurrentComments();
        double progressPercent = totalGoals > 0 ? (currentProgress * 100.0 / totalGoals) : 0;
        map.put("progressPercent", Math.min(100, progressPercent));

        return map;
    }
}
