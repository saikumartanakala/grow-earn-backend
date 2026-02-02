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

import com.growearn.entity.Campaign;
import com.growearn.security.JwtUtil;
import com.growearn.service.CampaignService;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping({"/api/creator/campaign", "/api/creator/campaigns"})
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class CampaignController {

    private final CampaignService campaignService;
    private final JwtUtil jwtUtil;

    public CampaignController(CampaignService campaignService, JwtUtil jwtUtil) {
        this.campaignService = campaignService;
        this.jwtUtil = jwtUtil;
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
        if (isNullOrEmpty(campaign.getPlatform())) missingFields.add("platform");
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
            return ResponseEntity.ok(saved);
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
        String normalizedStatus = status.equalsIgnoreCase("in-progress") ? "IN_PROGRESS" : status.equalsIgnoreCase("completed") ? "COMPLETED" : status.toUpperCase();
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
}